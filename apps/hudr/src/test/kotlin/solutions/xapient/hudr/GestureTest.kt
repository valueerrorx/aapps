package solutions.xapient.hudr

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.location.LocationManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.hudr.domain.Fix
import solutions.xapient.hudr.domain.GpsPhase
import solutions.xapient.hudr.domain.Reading
import solutions.xapient.hudr.domain.SpeedUnit
import solutions.xapient.hudr.domain.Track
import solutions.xapient.hudr.ui.HudrAccent
import solutions.xapient.hudr.ui.HudrSettings
import solutions.xapient.hudr.ui.LocState
import solutions.xapient.hudr.ui.SpeedScreen
import solutions.xapient.hudr.ui.locState

// GPS ohne Geraet
class FakeGps(var on: Boolean = true) : GpsSource {
    var running = false
    var starts = 0
    override var onFix: ((Fix) -> Unit)? = null
    override var onSatellites: ((Int, Int) -> Unit)? = null
    override var onEnabled: ((Boolean) -> Unit)? = null
    override fun enabled() = on
    override fun start(): Boolean {
        running = true
        starts++
        return true
    }
    override fun stop() {
        running = false
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GestureTest {
    // Vor dem Start der Activity: nicht von selbst fragen, sonst pausiert der Systemdialog die Activity
    @get:Rule(order = 0) val noAutoAsk = object : ExternalResource() {
        override fun before() {
            Prefs(RuntimeEnvironment.getApplication()).askedLocation = true
        }
    }

    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    private val gps = FakeGps()
    private val app get() = RuntimeEnvironment.getApplication()

    private fun show(content: @Composable () -> Unit) {
        rule.runOnUiThread { rule.activity.setContent { AappsTheme(HudrAccent) { content() } } }
        rule.waitForIdle()
    }

    // Ganze App mit Fake; key erzwingt frischen Zustand
    private fun app(beep: (() -> Unit)? = null) {
        rule.runOnUiThread { rule.activity.setContent { key("test") { HudrRoot({ gps }, now = { 0L }, beep = beep) } } }
        rule.waitForIdle()
    }

    private fun grant(vararg p: String) = shadowOf(app).grantPermissions(*p)
    private fun rationale(show: Boolean) =
        shadowOf(app.packageManager).setShouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION, show)

    private var locTaps = 0
    private var resets = 0
    private var huds = 0

    private fun screen(loc: LocState) = show {
        SpeedScreen(
            { Reading.SEARCHING }, remember { mutableIntStateOf(0) }, loc, Settings(), null,
            onHud = { huds++ }, onReset = { resets++ }, onLoc = { locTaps++ }, onSettings = {}, animate = false,
        )
    }

    @Test fun resetAndHudButtons() {
        screen(LocState.READY)
        rule.onNodeWithContentDescription("Reset trip").performClick()
        rule.onNodeWithText("HUD", ignoreCase = true).performClick()
        rule.waitForIdle()
        assertEquals(1, resets)
        assertEquals(1, huds)
    }

    @Test fun locActionsPerState() {
        listOf(
            LocState.ASK to "Allow location",
            LocState.COARSE to "Use precise location",
            LocState.BLOCKED to "Open settings",
            LocState.GPS_OFF to "Turn on location",
        ).forEachIndexed { i, (state, label) ->
            screen(state)
            rule.onNodeWithText(label, ignoreCase = true).performClick()
            rule.waitForIdle()
            assertEquals(i + 1, locTaps)
            rule.onNodeWithContentDescription("Reset trip").assertDoesNotExist()
        }
    }

    @Test fun hudOnAndOff() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        app()
        val before = rule.activity.requestedOrientation
        rule.onNodeWithText("HUD", ignoreCase = true).performClick()
        rule.waitForIdle()
        // HUD flach auf dem Armaturenbrett: Lage festhalten
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, rule.activity.requestedOrientation)
        rule.onNodeWithContentDescription("Speed")
            .assert(SemanticsMatcher("Tipp-Bezeichnung") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Show exit button" })
        rule.onNodeWithContentDescription("Reset trip").assertDoesNotExist()
        rule.onNodeWithText("Exit HUD", ignoreCase = true).assertDoesNotExist()
        // Tippen zeigt den Ausgang, er verschwindet nach 3 s wieder
        rule.onNodeWithContentDescription("Speed").performClick()
        rule.onNodeWithText("Exit HUD", ignoreCase = true).assertExists()
        rule.mainClock.advanceTimeBy(3_500)
        rule.onNodeWithText("Exit HUD", ignoreCase = true).assertDoesNotExist()
        rule.onNodeWithContentDescription("Speed").performClick()
        rule.onNodeWithText("Exit HUD", ignoreCase = true).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Reset trip").assertExists()
        assertEquals(before, rule.activity.requestedOrientation)
        assertTrue(gps.running)
    }

    @Test fun beepOnceAboveLimit() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        Prefs(app).settings = Settings(SpeedUnit.KMH, limitOn = true, limit = 50)
        var beeps = 0
        app { beeps++ }
        rule.runOnUiThread {
            for (i in 0..10) gps.onFix?.invoke(Track.fix(i.toDouble(), meters = i * 16.0, speed = 16f))
        }
        rule.waitForIdle()
        assertEquals(1, beeps)
    }

    @Test fun noBeepWhenSoundOff() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        Prefs(app).settings = Settings(SpeedUnit.KMH, limitOn = true, limit = 50, sound = false)
        var beeps = 0
        app { beeps++ }
        rule.runOnUiThread {
            for (i in 0..10) gps.onFix?.invoke(Track.fix(i.toDouble(), meters = i * 16.0, speed = 16f))
        }
        rule.waitForIdle()
        assertEquals(0, beeps)
    }

    @Test fun limitSettings() {
        var s by mutableStateOf(Settings(SpeedUnit.KMH))
        show { HudrSettings(s, { s = it }, null, {}, {}) }
        rule.onNodeWithContentDescription("Raise limit").assertDoesNotExist()
        rule.onNodeWithText("Warn above a speed").performClick()
        rule.waitForIdle()
        assertTrue(s.limitOn)
        repeat(2) {
            rule.onNodeWithContentDescription("Raise limit").performClick()
            rule.waitForIdle()
        }
        assertEquals(60, s.limit)
        rule.onNodeWithContentDescription("Lower limit").performClick()
        rule.waitForIdle()
        assertEquals(55, s.limit)
        // Einheit wechseln nimmt die Schwelle mit: 55 km/h ~ 34 mph -> 35
        rule.onNodeWithText("mph").performClick()
        rule.waitForIdle()
        assertEquals(SpeedUnit.MPH, s.unit)
        assertEquals(35, s.limit)
        rule.onNodeWithText("Mirror HUD").performClick()
        rule.waitForIdle()
        assertFalse(s.mirror)
    }

    // Dialog weggetippt: keine Begruendung, aber nie abgelehnt -> weiter fragen
    @Test fun dismissedStaysAskable() {
        rationale(false)
        app()
        assertEquals(0, gps.starts)
        rule.onNodeWithText("Allow location", ignoreCase = true).assertExists()
    }

    // einmal abgelehnt: System will Begruendung, wird gemerkt
    @Test fun deniedOnceAsksAgain() {
        rationale(true)
        app()
        rule.onNodeWithText("Allow location", ignoreCase = true).assertExists()
        assertTrue(Prefs(app).rationaleSeen)
    }

    // zweimal abgelehnt: Begruendung war da, jetzt keine mehr = "nie wieder fragen"
    @Test fun deniedTwiceIsBlocked() {
        Prefs(app).rationaleSeen = true
        rationale(false)
        app()
        assertEquals(0, gps.starts)
        rule.onNodeWithText("Open settings", ignoreCase = true).assertExists()
    }

    // nur ungefaehr erteilt: Hinweis und erneut nach genau fragen
    @Test fun coarseOnlyAsksForPrecise() {
        grant(Manifest.permission.ACCESS_COARSE_LOCATION)
        rationale(false)
        app()
        assertEquals(0, gps.starts)
        rule.onNodeWithText("Precise location needed").assertExists()
        rule.onNodeWithText("Use precise location", ignoreCase = true).assertExists()
    }

    // ungefaehr erteilt bleibt ungefaehr, auch nach frueherer Ablehnung; blockiert erst nach sofortiger Absage
    @Test fun locStates() {
        assertEquals(LocState.READY, locState(fine = true, coarse = true, gpsOn = true, blocked = false, upgradeRefused = false))
        assertEquals(LocState.GPS_OFF, locState(fine = true, coarse = true, gpsOn = false, blocked = true, upgradeRefused = false))
        assertEquals(LocState.COARSE, locState(fine = false, coarse = true, gpsOn = true, blocked = true, upgradeRefused = false))
        assertEquals(LocState.BLOCKED, locState(fine = false, coarse = true, gpsOn = true, blocked = false, upgradeRefused = true))
        assertEquals(LocState.BLOCKED, locState(fine = false, coarse = false, gpsOn = true, blocked = true, upgradeRefused = false))
        assertEquals(LocState.ASK, locState(fine = false, coarse = false, gpsOn = true, blocked = false, upgradeRefused = false))
    }

    // nur ungefaehr, Begruendung schon gesehen: weiter "genau verwenden", nicht "blockiert"
    @Test fun coarseAfterRationaleStaysCoarse() {
        grant(Manifest.permission.ACCESS_COARSE_LOCATION)
        Prefs(app).rationaleSeen = true
        rationale(false)
        app()
        rule.onNodeWithText("Use precise location", ignoreCase = true).assertExists()
    }

    // Standort zur Laufzeit wieder an: System-Broadcast startet die Messung
    @Test fun providersChangedStartsGps() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        gps.on = false
        app()
        assertFalse(gps.running)
        rule.runOnUiThread {
            gps.on = true
            app.sendBroadcast(Intent(LocationManager.PROVIDERS_CHANGED_ACTION).setPackage(app.packageName))
        }
        rule.waitForIdle()
        assertTrue(gps.running)
    }

    @Test fun gpsOffShowsHint() {
        grant(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        gps.on = false
        app()
        assertEquals(0, gps.starts)
        rule.onNodeWithText("Turn on location", ignoreCase = true).assertExists()
        // Standort im System eingeschaltet: Messung startet ohne Umweg
        rule.runOnUiThread {
            gps.on = true
            gps.onEnabled?.invoke(true)
        }
        rule.waitForIdle()
        assertTrue(gps.running)
    }

    // ohne neuen Fix keine Bilder: nach dem Einschwingen steht die Uhr
    @Test fun idleWithoutFrames() {
        val version = mutableIntStateOf(0)
        var r = Reading(GpsPhase.FIX, 0f, false, 4f, 8, 12, Float.NaN, Float.NaN, 0.0, solutions.xapient.hudr.domain.Alert.NONE)
        show {
            SpeedScreen({ r }, version, LocState.READY, Settings(), null, {}, {}, {}, {}, animate = true)
        }
        val t0 = rule.mainClock.currentTime
        rule.waitForIdle()
        assertEquals(t0, rule.mainClock.currentTime)
        // neuer Fix: Feder laeuft eine Weile, schlaeft dann ein
        rule.runOnUiThread {
            r = r.copy(speed = 60f, moving = true)
            version.intValue++
        }
        rule.waitForIdle()
        val t1 = rule.mainClock.currentTime
        assertTrue("Feder lief ${t1 - t0} ms", t1 - t0 in 200..3_000)
        rule.waitForIdle()
        assertEquals(t1, rule.mainClock.currentTime)
    }
}
