package solutions.xapient.tunr

import android.Manifest
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.tunr.audio.Mic
import solutions.xapient.tunr.domain.Frame
import solutions.xapient.tunr.domain.Instrument
import solutions.xapient.tunr.domain.Reading
import solutions.xapient.tunr.domain.Settings
import solutions.xapient.tunr.ui.InstrumentScreen
import solutions.xapient.tunr.ui.MicUi
import solutions.xapient.tunr.ui.TunerScreen
import solutions.xapient.tunr.ui.TunerState
import solutions.xapient.tunr.ui.TunrAccent
import solutions.xapient.tunr.ui.TunrSettings
import kotlin.math.pow

// Gesten und Lebenszyklus ohne echtes Mikrofon
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GestureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private class FakeMic : Mic {
        var starts = 0
        var stops = 0
        var sink: ((Frame) -> Unit)? = null
        override fun start(sink: (Frame) -> Unit, onError: () -> Unit): Boolean {
            starts++
            this.sink = sink
            return true
        }
        override fun stop() {
            stops++
            sink = null
        }
    }

    private fun hz(midi: Int, cents: Float = 0f) = (440.0 * 2.0.pow((midi - 69) / 12.0 + cents / 1200.0)).toFloat()

    // eigener Inhalt statt der App; ComponentActivity ist im Test-Manifest nicht registriert
    private fun show(content: @Composable () -> Unit) {
        rule.runOnUiThread { rule.activity.setContent { AappsTheme(TunrAccent) { content() } } }
        rule.waitForIdle()
    }

    private fun tuner(state: TunerState, settings: Settings, mic: MicUi = MicUi.ON, onMic: () -> Unit = {}, lockedOut: (Int) -> Unit = {}) = show {
        var locked by remember { mutableIntStateOf(-1) }
        TunerScreen(
            state, settings, mic, locked, null,
            onLock = { locked = it; lockedOut(it); state.lock(it) },
            onInstrument = {}, onSettings = {}, onMicAction = onMic,
        )
    }

    @Test fun tapStringHoldsAndReleases() {
        val state = TunerState(instrument = Instrument.GUITAR)
        var locked = -2
        tuner(state, Settings(instrument = Instrument.GUITAR), lockedOut = { locked = it })
        rule.onNodeWithContentDescription("String 2, A2").performClick()
        rule.waitForIdle()
        assertEquals(1, locked)
        assertEquals(45, state.coarse.target)
        rule.onNodeWithContentDescription("String 2, A2").performClick()
        rule.waitForIdle()
        assertEquals(-1, locked)
    }

    // Leerlauf: keine Frames; Ton: Bildschleife laeuft; nach dem Ton schlaeft sie wieder ein
    @Test fun framesOnlyWhileSounding() {
        val state = TunerState()
        tuner(state, Settings())
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(200)
        val idle = state.frame
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals(idle, state.frame)
        rule.runOnUiThread { repeat(6) { state.push(Frame(hz(57, 20f), -30f)) }; Snapshot.sendApplyNotifications() }
        val pushed = state.frame
        rule.mainClock.advanceTimeBy(500)
        assertTrue("klingt: Frames ${state.frame} vs $pushed, ${state.coarse}", state.frame > pushed + 10)
        rule.runOnUiThread { repeat(10) { state.push(Frame(0f, -80f)) }; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(4_000)
        val settled = state.frame
        rule.mainClock.advanceTimeBy(2_000)
        assertEquals(settled, state.frame)
    }

    @Test fun micButtons() {
        var taps = 0
        tuner(TunerState(), Settings(), MicUi.ASK, onMic = { taps++ })
        rule.onNodeWithText("Allow microphone", ignoreCase = true).performClick()
        tuner(TunerState(), Settings(), MicUi.BLOCKED, onMic = { taps++ })
        rule.onNodeWithText("Open app info", ignoreCase = true).performClick()
        tuner(TunerState(), Settings(), MicUi.ERROR, onMic = { taps++ })
        rule.onNodeWithText("Try again", ignoreCase = true).performClick()
        assertEquals(3, taps)
    }

    // nur Nullen vom Mikrofon: Hinweis, verschwindet mit echtem Signal
    @Test fun silentMicHint() {
        val state = TunerState()
        tuner(state, Settings())
        rule.runOnUiThread { state.push(Frame(0f, -120f, dead = true)) }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Tuner").assertExistsWithState("No sound from the microphone. Blocked or used by another app?")
        rule.runOnUiThread { state.push(Frame(0f, -80f)) }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Tuner").assertExistsWithState("Listening")
    }

    @Test fun pickInstrument() {
        var picked: Instrument? = null
        show { InstrumentScreen(Instrument.CHROMATIC, solutions.xapient.tunr.domain.Notation.SHARP, { picked = it }, {}) }
        rule.onNodeWithText("Ukulele").performClick()
        assertEquals(Instrument.UKULELE, picked)
    }

    @Test fun concertPitchStepper() {
        var s by mutableStateOf(Settings())
        show { TunrSettings(s, { s = it.sane() }, {}, {}) }
        rule.onNodeWithContentDescription("Raise concert pitch").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(441, s.a4)
        rule.onNodeWithContentDescription("Lower concert pitch").performTouchInput { down(center); up() }
        rule.onNodeWithContentDescription("Lower concert pitch").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(439, s.a4)
        // Halten wiederholt, Grenze 466
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Raise concert pitch").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(8_000)
        rule.onNodeWithContentDescription("Raise concert pitch").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(300)
        assertEquals(Settings.A4_MAX, s.a4)
    }

    // Mikrofon nur im Vordergrund: ON_STOP stoppt, ON_START startet neu; Toene kommen an
    @Test fun micFollowsLifecycle() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO)
        val mic = FakeMic()
        show { TunrApp { mic } }
        assertEquals(1, mic.starts)
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { repeat(6) { mic.sink!!(Frame(hz(69, 1f), -30f)) }; Snapshot.sendApplyNotifications() }
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithContentDescription("Tuner").assertExistsWithState("A4, in tune")
        rule.mainClock.autoAdvance = true
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.waitForIdle()
        assertEquals(1, mic.stops)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitForIdle()
        assertEquals(2, mic.starts)
        // Einstellungen: Mikrofon aus
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitForIdle()
        assertEquals(2, mic.stops)
    }

    // Ring sitzt in jedem Modus an derselben Stelle (hoch und quer, auch mit 200 % Schrift)
    private fun assertDialStaysPut() {
        fun dialBounds(instrument: Instrument, r: Reading): androidx.compose.ui.geometry.Rect {
            val s = Settings(instrument = instrument)
            val state = TunerState(s.a4, s.instrument).apply { show(r, 0f, 1f) }
            show { TunerScreen(state, s, MicUi.ON, -1, null, {}, {}, {}, {}, animate = false) }
            return rule.onNodeWithContentDescription("Tuner").fetchSemanticsNode().boundsInRoot
        }
        val inTune = Reading(active = true, heard = true, hz = hz(69, 1f), target = 69, cents = 1f, inTune = true, locks = 1)
        val flat = Reading(active = true, heard = true, hz = hz(45, -23f), target = 45, cents = -23f, string = 1, locks = 1)
        assertEquals(dialBounds(Instrument.CHROMATIC, inTune), dialBounds(Instrument.GUITAR, flat))
    }

    @Test fun dialStaysPut() = assertDialStaysPut()
    @Test @Config(qualifiers = "+land") fun dialStaysPutLand() = assertDialStaysPut()
    @Test @Config(fontScale = 2f) fun dialStaysPutFont200() = assertDialStaysPut()
    @Test @Config(qualifiers = "+land", fontScale = 2f) fun dialStaysPutLandFont200() = assertDialStaysPut()

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsWithState(expected: String) {
        val node = fetchSemanticsNode()
        val state = node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.StateDescription) { "" }
        assertEquals(expected, state)
    }
}
