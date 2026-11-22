package solutions.xapient.magnr

import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.magnr.domain.Sensitivity
import solutions.xapient.magnr.ui.MagnrAccent
import solutions.xapient.magnr.ui.MagnrSettings

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GestureTest {
    // Einfuehrung schon gesehen, ausser ein Test will sie
    @get:Rule(order = 0) val seen = object : ExternalResource() {
        override fun before() {
            Prefs(RuntimeEnvironment.getApplication()).introSeen = true
        }
    }

    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    private val source = FakeSource()
    private val beeper = FakeBeeper()

    private fun prefs() = Prefs(RuntimeEnvironment.getApplication())

    // Ganze App mit Fakes; key erzwingt frischen Zustand statt der App, die MainActivity schon zeigt
    private fun app() {
        rule.runOnUiThread { rule.activity.setContent { key("test") { MagnrApp({ source }, { beeper }) } } }
        rule.waitForIdle()
    }

    private fun emit(z: Float, samples: Int) {
        rule.runOnUiThread { source.emit(z, samples) }
        rule.waitForIdle()
    }

    @Test fun zeroTakesCurrentField() {
        prefs().settings = Settings(sound = true)
        app()
        emit(48f, 50)
        emit(90f, 50)
        assertEquals(1f, beeper.strength, 0f)
        rule.onNodeWithText("Zero", ignoreCase = true).performClick()
        rule.waitForIdle()
        assertEquals(0f, beeper.strength, 0f)
        // gleiches Feld ist jetzt der Grundwert
        emit(90f, 50)
        assertEquals(0f, beeper.strength, 0f)
        // zurueck zum Erdfeld: wieder voller Ausschlag
        emit(48f, 50)
        assertEquals(1f, beeper.strength, 0f)
    }

    @Test fun pauseStopsSensorAndSound() {
        prefs().settings = Settings(sound = true)
        app()
        assertTrue(source.running)
        assertTrue(beeper.running)
        rule.onNodeWithContentDescription("Pause").performClick()
        rule.waitForIdle()
        assertFalse(source.running)
        assertFalse(beeper.running)
        rule.onNodeWithContentDescription("Resume").performClick()
        rule.waitForIdle()
        assertTrue(source.running)
        assertTrue(beeper.running)
    }

    // pausiert: Nullen misst weiter und schwingt neu ein
    @Test fun zeroWhilePausedResumes() {
        app()
        rule.onNodeWithContentDescription("Pause").performClick()
        rule.waitForIdle()
        assertFalse(source.running)
        rule.onNodeWithText("Zero", ignoreCase = true).performClick()
        rule.waitForIdle()
        assertTrue(source.running)
    }

    @Test fun soundToggleIsRemembered() {
        app()
        assertFalse(beeper.running)
        rule.onNodeWithContentDescription("Sound").performClick()
        rule.waitForIdle()
        assertTrue(beeper.running)
        assertTrue(prefs().settings.sound)
        rule.onNodeWithContentDescription("Sound").performClick()
        rule.waitForIdle()
        assertFalse(beeper.running)
        assertFalse(prefs().settings.sound)
    }

    @Test fun strengthReachesBeeper() {
        prefs().settings = Settings(sound = true)
        app()
        emit(48f, 50)
        emit(90f, 25)
        assertEquals(1f, beeper.strength, 0f)
    }

    // Kein Sensor kommt, nichts wird gezeichnet: der Zaehler steht
    @Test fun pausedHasNoTicks() {
        app()
        emit(48f, 50)
        rule.onNodeWithContentDescription("Pause").performClick()
        rule.waitForIdle()
        val starts = source.starts
        rule.mainClock.advanceTimeBy(2_000)
        emit(70f, 10)
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals(starts, source.starts)
        assertFalse(source.running)
    }

    @Test fun introShownOnceAndDismissed() {
        prefs().introSeen = false
        app()
        rule.onNodeWithText("Got it", ignoreCase = true).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Got it", ignoreCase = true).assertDoesNotExist()
        assertTrue(prefs().introSeen)
        // Fusszeile oeffnet sie wieder
        rule.onNodeWithText("Finds iron", substring = true).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Got it", ignoreCase = true).assertExists()
    }

    @Test fun noSensorShowsHint() {
        rule.runOnUiThread { rule.activity.setContent { key("test") { MagnrApp({ FakeSource(isAvailable = false) }, { beeper }) } } }
        rule.waitForIdle()
        rule.onNodeWithText("No magnetic sensor").assertExists()
        rule.onNodeWithText("Zero", ignoreCase = true).assertDoesNotExist()
    }

    @Test fun sensitivityChoice() {
        var s by mutableStateOf(Settings())
        rule.runOnUiThread { rule.activity.setContent { AappsTheme(MagnrAccent) { MagnrSettings(s, { s = it }, {}, {}, {}, {}) } } }
        rule.waitForIdle()
        rule.onNodeWithText("High").performClick()
        rule.waitForIdle()
        assertEquals(Sensitivity.HIGH, s.sensitivity)
        rule.onNodeWithText("Full scale at 12 µT deviation").assertExists()
        rule.onNodeWithText("Follow slow drift").performClick()
        rule.waitForIdle()
        assertFalse(s.drift)
    }
}
