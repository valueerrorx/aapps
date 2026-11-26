package solutions.xapient.roundr

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.roundr.domain.Presets
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.domain.Session
import solutions.xapient.roundr.domain.Workout
import solutions.xapient.roundr.ui.EditorScreen
import solutions.xapient.roundr.ui.RoundrAccent
import solutions.xapient.roundr.ui.RunScreen

// Gesten auf Bearbeiten und Lauf
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GestureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    // Eigener Inhalt statt der App; ComponentActivity ist im Test-Manifest nicht registriert
    private fun show(content: @Composable () -> Unit) {
        rule.runOnUiThread { rule.activity.setContent { AappsTheme(RoundrAccent) { content() } } }
        rule.waitForIdle()
    }

    private fun editor(): () -> Int {
        var w by mutableStateOf(Presets.default)
        show { EditorScreen(w, 0, null, { w = it }, { true }, {}) }
        return { w.workSec }
    }

    private fun value(desc: String, text: String) =
        rule.onNodeWithContentDescription(desc).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text))

    @Test fun tapPlusAndMinusStepOnce() {
        val work = editor()
        rule.onNodeWithContentDescription("More Active").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(25, work())
        value("Active", "0:25")
        rule.onNodeWithContentDescription("Less Active").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(20, work())
    }

    @Test fun holdRepeats() {
        val work = editor()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("More Active").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onNodeWithContentDescription("More Active").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(500)
        val after = work()
        assertTrue("nur $after", after >= 60)
        // nach dem Loslassen keine weiteren Schritte
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(after, work())
    }

    // Lauf ueber den echten RunStore mit Test-Uhr, ohne Dienst
    private var now = 0L

    private fun runScreen(): RunStore {
        var saved: Run? = null
        val store = RunStore({ null }, { saved = it }, { now })
        store.start(Presets.default)
        show {
            val r by store.state.collectAsState()
            r?.let {
                RunScreen(
                    it, clock = { now }, haptics = null, onToggle = store::toggle, onSkip = store::skip, onBack = store::back,
                    onStop = store::clear, onAgain = {}, onClose = store::clear, keepOn = false, animate = false,
                )
            }
        }
        return store
    }

    @Test fun pauseAndResume() {
        val store = runScreen()
        now = 4_000
        rule.onNodeWithContentDescription("Pause").performClick()
        rule.waitForIdle()
        assertFalse(store.state.value!!.session.running)
        now = 60_000
        assertEquals(4_000, store.state.value!!.session.elapsed(now))
        rule.onNodeWithContentDescription("Resume").performClick()
        rule.waitForIdle()
        assertTrue(store.state.value!!.session.running)
        assertEquals(5_000, store.state.value!!.session.elapsed(61_000))
    }

    @Test fun skipJumpsToNextPhase() {
        val store = runScreen()
        now = 2_000
        rule.onNodeWithContentDescription("Next phase").performClick()
        rule.waitForIdle()
        assertEquals(10_000, store.state.value!!.session.elapsed(now))
    }

    // Fertig-Ansicht: Frame-Schleife endet, die Uhr wird nicht mehr abgefragt
    @Test fun noFramesAfterDone() {
        val w = Workout(prepSec = 0, workSec = 5, restSec = 0, rounds = 1)
        var calls = 0
        rule.mainClock.autoAdvance = false
        show {
            RunScreen(
                Run(w, Session.start(0), 1), clock = { calls++; now }, haptics = null, onToggle = {}, onSkip = {}, onBack = {},
                onStop = {}, onAgain = {}, onClose = {}, keepOn = false, animate = true,
            )
        }
        rule.mainClock.advanceTimeBy(200)
        val running = calls
        rule.mainClock.advanceTimeBy(200)
        assertTrue("laeuft: keine Frames", calls > running)
        now = 5_000
        rule.mainClock.advanceTimeBy(200)
        val done = calls
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(done, calls)
    }

    @Test fun stopNeedsSecondTap() {
        val store = runScreen()
        rule.onNodeWithContentDescription("Stop").performClick()
        rule.waitForIdle()
        assertTrue(store.state.value != null)
        rule.onNodeWithContentDescription("Stop").performClick()
        rule.waitForIdle()
        assertNull(store.state.value)
    }
}
