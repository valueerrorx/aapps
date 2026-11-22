package solutions.xapient.focusr

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.rules.ExternalResource
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.focusr.domain.Mode
import solutions.xapient.focusr.domain.Scale
import solutions.xapient.focusr.domain.Timer
import solutions.xapient.focusr.ui.FocusrAccent
import solutions.xapient.focusr.ui.TimerScreen

// Ticker: im Ruhezustand keine Schritte, im Lauf nur so oft wie noetig (gezaehlt ueber die Uhr-Abfragen)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class TickerTest {
    // leere Activity fuer die Testregel anmelden (kein Test-Manifest im Projekt)
    @get:Rule(order = 0) val host = object : ExternalResource() {
        override fun before() {
            val app = RuntimeEnvironment.getApplication()
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
        }
    }

    @get:Rule(order = 1) val rule = createComposeRule()

    private val base = 1_000_000L
    private val min = Timer.MINUTE
    private var reads = 0

    private fun show(t: Timer, scale: Scale = Scale.MIN60) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AappsTheme(FocusrAccent) {
                TimerScreen(
                    t, scale, haptics = null, onSet = {}, onStart = {}, onPause = {}, onReset = {}, onSettings = {},
                    endText = { "" }, clock = { reads++; base + rule.mainClock.currentTime },
                )
            }
        }
        // Aufbau und Einschwingen abwarten
        rule.mainClock.advanceTimeBy(2_000)
    }

    private fun readsDuring(ms: Long): Int {
        val r0 = reads
        rule.mainClock.advanceTimeBy(ms)
        return reads - r0
    }

    @Test fun idleIsStill() {
        show(Timer(25 * min))
        assertEquals(0, readsDuring(30_000))
    }

    @Test fun pausedIsStill() {
        show(Timer(25 * min, Mode.PAUSED, 0, 9 * min))
        assertEquals(0, readsDuring(30_000))
    }

    @Test fun doneIsStill() {
        show(Timer(25 * min, Mode.RUNNING, base - 1_000, 0))
        assertEquals(0, readsDuring(30_000))
    }

    // 60er-Skala: Rand wandert < 1 px/s, also etwa ein Schritt pro Sekunde (Sekundenanzeige)
    @Test fun runningSixtyTicksAboutOncePerSecond() {
        show(Timer(25 * min).start(base))
        val n = readsDuring(10_000)
        assertTrue("n=$n", n in 9..12)
    }

    // 5er-Skala: oefter, aber nie mehr als ein Schritt pro Frame
    @Test fun runningFiveTicksFasterButBounded() {
        show(Timer(4 * min).start(base), Scale.MIN5)
        val n = readsDuring(10_000)
        assertTrue("n=$n", n in 30..625)
    }

    // Laeuft ab: danach steht der Ticker
    @Test fun stopsAtEnd() {
        show(Timer(5_000).start(base))
        rule.mainClock.advanceTimeBy(10_000)
        assertEquals(0, readsDuring(30_000))
    }
}
