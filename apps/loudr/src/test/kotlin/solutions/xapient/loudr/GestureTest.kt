package solutions.xapient.loudr

import android.Manifest
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
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
import solutions.xapient.loudr.domain.Reading
import solutions.xapient.loudr.ui.LoudrAccent
import solutions.xapient.loudr.ui.LoudrSettings
import solutions.xapient.loudr.ui.MeterScreen
import solutions.xapient.loudr.ui.MicState

// Pegelquelle ohne Mikrofon
class FakeMeter : LevelMeter {
    var running = false
    var starts = 0
    var stops = 0
    var resets = 0
    var reads = 0
    override var onFailure: (() -> Unit)? = null
    override fun start(): Boolean {
        running = true
        starts++
        return true
    }
    override fun stop() {
        if (running) stops++
        running = false
    }
    override fun reset() {
        resets++
    }
    override fun reading(): Reading {
        reads++
        return Reading(62f, 61f, 60f, 45f, 75f, 50f, 70f, FloatArray(40) { 55f + it % 7 }, running)
    }
    override fun save() = DoubleArray(0)
    override fun restore(state: DoubleArray) = Unit
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GestureTest {
    // Vor dem Start der Activity: nicht von selbst nach dem Mikrofon fragen, sonst pausiert der Systemdialog die Activity
    @get:Rule(order = 0) val noAutoAsk = object : ExternalResource() {
        override fun before() {
            Prefs(RuntimeEnvironment.getApplication()).askedMic = true
        }
    }

    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    private val meter = FakeMeter()

    // Eigener Inhalt statt der App; ComponentActivity ist im Test-Manifest nicht registriert
    private fun show(content: @Composable () -> Unit) {
        rule.runOnUiThread { rule.activity.setContent { AappsTheme(LoudrAccent) { content() } } }
        rule.waitForIdle()
    }

    // Ganze App mit Fake; key erzwingt frischen Zustand statt der App, die MainActivity schon zeigt
    private fun app() {
        rule.runOnUiThread { rule.activity.setContent { key("test") { LoudrApp { meter } } } }
        rule.waitForIdle()
    }

    private var measuring by mutableStateOf(true)
    private var micTaps = 0

    private fun screen(mic: MicState = MicState.READY, animate: Boolean = true) = show {
        MeterScreen(
            meter::reading, mic, measuring, Settings(), null,
            onToggle = { measuring = !measuring }, onReset = meter::reset, onMic = { micTaps++ }, onSettings = {}, animate = animate,
        )
    }

    @Test fun pauseAndResume() {
        measuring = false
        screen()
        // beim Messen laeuft jedes Bild: Uhr von Hand weiterstellen
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Resume", ignoreCase = true).performClick()
        rule.mainClock.advanceTimeBy(100)
        assertTrue(measuring)
        rule.onNodeWithText("Pause", ignoreCase = true).performClick()
        rule.mainClock.advanceTimeBy(100)
        assertFalse(measuring)
        rule.onNodeWithText("Resume", ignoreCase = true).assertExists()
    }

    @Test fun reset() {
        measuring = false
        screen()
        rule.onNodeWithContentDescription("Reset min, max and average").performClick()
        rule.waitForIdle()
        assertEquals(1, meter.resets)
    }

    @Test fun micActions() {
        screen(MicState.ASK)
        rule.onNodeWithText("Allow microphone", ignoreCase = true).performClick()
        rule.waitForIdle()
        assertEquals(1, micTaps)
        rule.onNodeWithContentDescription("Reset min, max and average").assertDoesNotExist()
    }

    @Test fun blockedLeadsToSettings() {
        screen(MicState.BLOCKED)
        rule.onNodeWithText("Open settings", ignoreCase = true).performClick()
        rule.waitForIdle()
        assertEquals(1, micTaps)
    }

    // Robolectric zeichnet nur auf Anfrage: je Schritt ein Bild erzwingen. Neue Werte holt die Anzeige nur bei neuer Bildzeit.
    private fun readsOver(steps: Int): Int {
        val before = meter.reads
        repeat(steps) {
            rule.mainClock.advanceTimeBy(100)
            rule.onRoot().captureToImage()
        }
        return meter.reads - before
    }

    // Pausiert: keine Frames, der Pegel wird nicht abgefragt; beim Messen jedes Bild
    @Test fun framesOnlyWhileMeasuring() {
        measuring = false
        screen()
        rule.mainClock.autoAdvance = false
        readsOver(1)
        assertEquals(0, readsOver(10))
        rule.runOnUiThread { measuring = true }
        rule.waitForIdle()
        readsOver(1)
        val live = readsOver(10)
        assertTrue("misst: $live", live >= 8)
        rule.runOnUiThread { measuring = false }
        rule.waitForIdle()
        readsOver(1)
        assertEquals(0, readsOver(10))
    }

    @Test fun calibrationSteps() {
        var s by mutableStateOf(Settings())
        show { LoudrSettings(s, { s = it }, { 60f }, null, {}, {}) }
        repeat(3) {
            rule.onNodeWithContentDescription("Raise offset").performClick()
            rule.waitForIdle()
        }
        assertEquals(1.5f, s.offset, 0f)
        rule.onNodeWithContentDescription("Lower offset").performClick()
        rule.waitForIdle()
        assertEquals(1f, s.offset, 0f)
        rule.onNodeWithText("Reset calibration").performClick()
        rule.waitForIdle()
        assertEquals(0f, s.offset, 0f)
        rule.onNodeWithText("Slow · 1 s").performClick()
        rule.waitForIdle()
        assertEquals(solutions.xapient.loudr.domain.TimeWeighting.SLOW, s.weighting)
    }

    private fun rationale(show: Boolean) =
        shadowOf(RuntimeEnvironment.getApplication().packageManager).setShouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO, show)

    // Dialog weggetippt: keine Begruendung, aber nie abgelehnt -> weiter fragen
    @Test fun dismissedStaysAskable() {
        rationale(false)
        app()
        assertEquals(0, meter.starts)
        rule.onNodeWithText("Allow microphone", ignoreCase = true).assertExists()
    }

    // einmal abgelehnt: System will Begruendung, wird gemerkt
    @Test fun deniedOnceAsksAgain() {
        rationale(true)
        app()
        rule.onNodeWithText("Allow microphone", ignoreCase = true).assertExists()
        assertTrue(Prefs(RuntimeEnvironment.getApplication()).rationaleSeen)
    }

    // zweimal abgelehnt: Begruendung war da, jetzt keine mehr = "nie wieder fragen"
    @Test fun deniedTwiceIsBlocked() {
        Prefs(RuntimeEnvironment.getApplication()).rationaleSeen = true
        rationale(false)
        app()
        assertEquals(0, meter.starts)
        rule.onNodeWithText("Open settings", ignoreCase = true).assertExists()
    }
}
