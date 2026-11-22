package solutions.xapient.pulsr

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import solutions.xapient.pulsr.domain.Settings
import solutions.xapient.pulsr.domain.Subdivision
import solutions.xapient.pulsr.ui.PulseScreen
import solutions.xapient.pulsr.ui.PulsrAccent
import kotlin.math.cos
import kotlin.math.sin

// Gesten auf dem Metronom ueber den echten Store, ohne Audio
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GestureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val player = FakePlayer()
    private var pulseCalls = 0

    @After fun reset() = PulseStore.install(null)

    // Eigener Inhalt statt der App; ComponentActivity ist im Test-Manifest nicht registriert
    private fun show(content: @Composable () -> Unit) {
        rule.runOnUiThread { rule.activity.setContent { AappsTheme(PulsrAccent) { content() } } }
        rule.waitForIdle()
    }

    private fun screen(): PulseStore {
        val store = PulseStore({ Settings() }, {}, player) { rule.mainClock.currentTime }
        show {
            val s by store.settings.collectAsState()
            val p by store.playing.collectAsState()
            PulseScreen(
                s, p, { pulseCalls++; store.pulse(it) }, null,
                onNudge = store::nudge, onBpm = store::setBpm, onTap = { store.tap() }, onBeats = store::setBeats,
                onSub = { store.set(store.settings.value.copy(sub = it)) }, onToggle = store::toggle, onSettings = {},
            )
        }
        return store
    }

    // Viertelkreis im Uhrzeigersinn = 15 BPM schneller, zurueck = langsamer
    @Test fun rotateWheel() {
        val store = screen()
        rule.onNodeWithContentDescription("Tempo").performTouchInput {
            val r = minOf(width, height) * 0.35f
            fun at(deg: Float) = center + Offset(r * cos(Math.toRadians(deg.toDouble())).toFloat(), r * sin(Math.toRadians(deg.toDouble())).toFloat())
            down(at(-90f))
            for (d in 1..30) moveTo(at(-90f + d * 3f))
            up()
        }
        rule.waitForIdle()
        assertEquals(135f, store.settings.value.bpm.toFloat(), 1f)
        rule.onNodeWithContentDescription("Tempo").performTouchInput {
            val r = minOf(width, height) * 0.35f
            fun at(deg: Float) = center + Offset(r * cos(Math.toRadians(deg.toDouble())).toFloat(), r * sin(Math.toRadians(deg.toDouble())).toFloat())
            down(at(0f))
            for (d in 1..60) moveTo(at(-d * 3f))
            up()
        }
        rule.waitForIdle()
        assertEquals(105f, store.settings.value.bpm.toFloat(), 1f)
    }

    @Test fun tapOnWheelChangesNothing() {
        val store = screen()
        rule.onNodeWithContentDescription("Tempo").performClick()
        rule.waitForIdle()
        assertEquals(120, store.settings.value.bpm)
    }

    @Test fun plusMinusAndHold() {
        val store = screen()
        rule.onNodeWithContentDescription("Faster").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(121, store.settings.value.bpm)
        rule.onNodeWithContentDescription("Slower").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(120, store.settings.value.bpm)
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Faster").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(1_500)
        rule.onNodeWithContentDescription("Faster").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(500)
        val after = store.settings.value.bpm
        assertTrue("nur $after", after >= 130)
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(after, store.settings.value.bpm)
    }

    @Test fun tapTempo() {
        val store = screen()
        rule.mainClock.autoAdvance = false
        repeat(4) {
            rule.onNodeWithContentDescription("Tap tempo").performTouchInput { down(center); up() }
            rule.mainClock.advanceTimeBy(400)
        }
        assertEquals(150, store.settings.value.bpm)
    }

    @Test fun meterAndSubdivision() {
        val store = screen()
        rule.onNodeWithContentDescription("Fewer beats").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(3, store.settings.value.beats)
        rule.onNodeWithContentDescription("Triplets").performClick()
        rule.waitForIdle()
        assertEquals(Subdivision.TRIPLET, store.settings.value.sub)
    }

    @Test fun startStop() {
        val store = screen()
        // beim Spielen laeuft jedes Bild: Uhr von Hand weiterstellen statt auf Leerlauf zu warten
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Start", ignoreCase = true).performClick()
        rule.mainClock.advanceTimeBy(200)
        assertTrue(store.playing.value)
        rule.onNodeWithText("Stop", ignoreCase = true).performClick()
        rule.mainClock.advanceTimeBy(200)
        assertFalse(store.playing.value)
        rule.onNodeWithText("Start", ignoreCase = true).assertExists()
    }

    // Leerlauf: keine Frames, der Puls wird nicht abgefragt; beim Spielen jedes Bild
    @Test fun framesOnlyWhilePlaying() {
        val store = screen()
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(100)
        val idle = pulseCalls
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(idle, pulseCalls)
        rule.runOnUiThread { store.start() }
        rule.mainClock.advanceTimeBy(500)
        assertTrue("spielt: Frames", pulseCalls > idle + 5)
        rule.runOnUiThread { store.stop() }
        rule.mainClock.advanceTimeBy(100)
        val stopped = pulseCalls
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(stopped, pulseCalls)
    }
}
