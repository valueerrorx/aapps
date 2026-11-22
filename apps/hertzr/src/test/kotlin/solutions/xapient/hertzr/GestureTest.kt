package solutions.xapient.hertzr

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.hertzr.domain.Mode
import solutions.xapient.hertzr.domain.Settings
import solutions.xapient.hertzr.domain.Wave
import solutions.xapient.hertzr.ui.HertzrAccent
import solutions.xapient.hertzr.ui.ToneActions
import solutions.xapient.hertzr.ui.ToneScreen
import solutions.xapient.hertzr.ui.ring
import kotlin.math.cos
import kotlin.math.sin

// Gesten auf dem Generator ueber den echten Store, ohne Audio
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GestureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val player = FakePlayer()
    private var frames = 0

    @After fun reset() = ToneStore.install(null)

    private fun show(content: @Composable () -> Unit) {
        rule.runOnUiThread { rule.activity.setContent { AappsTheme(HertzrAccent) { content() } } }
        rule.waitForIdle()
    }

    private fun screen(load: Settings = Settings()): ToneStore {
        val store = ToneStore({ load }, {}, player)
        show {
            val s by store.settings.collectAsState()
            val p by store.playing.collectAsState()
            ToneScreen(
                s, p, store.maxHz, store::shownHz, headphones = false, haptics = null,
                actions = ToneActions(
                    onHz = store::setHz, onNudge = store::nudge, onOctave = store::octave, onWave = store::setWave,
                    onLevel = store::setLevel, onMode = store::setMode, onSweepFrom = store::setSweepFrom,
                    onSweepTo = store::setSweepTo, onDuration = store::nextDuration, onToggle = store::toggle,
                ),
                onFrame = { frames++ },
            )
        }
        return store
    }

    // Viertelkreis im Uhrzeigersinn = Viertel-Oktave hoeher, zurueck = tiefer
    @Test fun rotateDial() {
        val store = screen()
        fun turn(fromDeg: Float, byDeg: Float) = rule.onNodeWithContentDescription("Frequency").performTouchInput {
            // um die Ringmitte drehen wie der Finger am Bogen
            val g = ring(Size(width.toFloat(), height.toFloat()))
            val r = g.r * 0.9f
            fun at(deg: Float) = g.center + Offset(r * cos(Math.toRadians(deg.toDouble())).toFloat(), r * sin(Math.toRadians(deg.toDouble())).toFloat())
            down(at(fromDeg))
            for (d in 1..30) moveTo(at(fromDeg + byDeg * d / 30))
            up()
        }
        turn(-90f, 90f)
        rule.waitForIdle()
        assertEquals(523.0, store.settings.value.hz, 1.5)
        turn(0f, -180f)
        rule.waitForIdle()
        assertEquals(370.0, store.settings.value.hz, 1.5)
    }

    @Test fun tapOnRingChangesNothing() {
        val store = screen()
        rule.onNodeWithContentDescription("Frequency").performTouchInput { click(Offset(width / 2f, height * 0.1f)) }
        rule.waitForIdle()
        assertEquals(440.0, store.settings.value.hz, 0.0)
    }

    @Test fun fineStepsAndHold() {
        val store = screen()
        rule.onNodeWithContentDescription("Higher").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(441.0, store.settings.value.hz, 0.0)
        rule.onNodeWithContentDescription("Lower").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(440.0, store.settings.value.hz, 0.0)
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Higher").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onNodeWithContentDescription("Higher").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(500)
        val after = store.settings.value.hz
        assertTrue("nur $after", after >= 450)
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(after, store.settings.value.hz, 0.0)
    }

    @Test fun octaves() {
        val store = screen()
        rule.onNodeWithContentDescription("One octave up").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(880.0, store.settings.value.hz, 0.0)
        repeat(2) { rule.onNodeWithContentDescription("One octave down").performTouchInput { down(center); up() } }
        rule.waitForIdle()
        assertEquals(220.0, store.settings.value.hz, 0.0)
    }

    @Test fun pickWave() {
        val store = screen()
        rule.onNodeWithContentDescription("Sawtooth").performClick()
        rule.waitForIdle()
        assertEquals(Wave.SAW, store.settings.value.wave)
    }

    // Ziehen verschiebt den Pegel relativ, Tippen springt nicht
    @Test fun levelDragOnlyNoJump() {
        val store = screen()
        rule.onNodeWithContentDescription("Level").performTouchInput { click(centerRight - Offset(4f, 0f)) }
        rule.waitForIdle()
        assertEquals(-24, store.settings.value.levelDb)
        rule.onNodeWithContentDescription("Level").performTouchInput {
            down(center)
            for (i in 1..20) moveTo(center + Offset(width * 0.25f * i / 20, 0f))
            up()
        }
        rule.waitForIdle()
        val db = store.settings.value.levelDb
        // Beruehrungsschwelle geht verloren, der Rest wirkt relativ
        assertTrue("Pegel $db", db in -16..-8)
    }

    @Test fun directInput() {
        val store = screen()
        rule.onNodeWithContentDescription("Enter frequency").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("1.5k")
        rule.onNodeWithText("Set", ignoreCase = true).performClick()
        rule.waitForIdle()
        assertEquals(1_500.0, store.settings.value.hz, 0.0)
        // Overlay ist zu
        assertEquals(1, rule.onAllNodesWithContentDescription("Enter frequency").fetchSemanticsNodes().size)
    }

    @Test fun sweepModeAndDuration() {
        val store = screen()
        rule.onNodeWithText("Sweep", ignoreCase = true).performClick()
        rule.waitForIdle()
        assertEquals(Mode.SWEEP, store.settings.value.mode)
        rule.onNodeWithContentDescription("Sweep duration", substring = true).performClick()
        rule.waitForIdle()
        assertEquals(20, store.settings.value.sweepSec)
        rule.onNodeWithContentDescription("Sweep end", substring = true).performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("8000")
        rule.onNodeWithText("Set", ignoreCase = true).performClick()
        rule.waitForIdle()
        assertEquals(8_000.0, store.settings.value.sweepTo, 0.0)
    }

    @Test fun startStop() {
        val store = screen()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Play", ignoreCase = true).performClick()
        rule.mainClock.advanceTimeBy(200)
        assertTrue(store.playing.value)
        rule.onNodeWithText("Stop", ignoreCase = true).performClick()
        rule.mainClock.advanceTimeBy(200)
        assertFalse(store.playing.value)
        rule.onNodeWithText("Play", ignoreCase = true).assertExists()
    }

    // Leerlauf: keine Frames; beim Spielen jedes Bild
    @Test fun framesOnlyWhilePlaying() {
        val store = screen()
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(100)
        val idle = frames
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(idle, frames)
        rule.runOnUiThread { store.start() }
        rule.mainClock.advanceTimeBy(500)
        assertTrue("spielt: Frames $idle -> $frames", frames > idle + 5)
        rule.runOnUiThread { store.stop() }
        rule.mainClock.advanceTimeBy(100)
        val stopped = frames
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(stopped, frames)
    }
}
