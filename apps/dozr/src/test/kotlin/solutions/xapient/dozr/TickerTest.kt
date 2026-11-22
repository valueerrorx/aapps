package solutions.xapient.dozr

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
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
import solutions.xapient.dozr.domain.FadeLength
import solutions.xapient.dozr.domain.Mode
import solutions.xapient.dozr.domain.Sleep
import solutions.xapient.dozr.domain.Sleep.Companion.MINUTE
import solutions.xapient.dozr.ui.DozrAccent
import solutions.xapient.dozr.ui.SleepScreen

// Ticker: im Ruhezustand keine Schritte, im Lauf etwa einer pro Sekunde (gezaehlt ueber die Uhr-Abfragen)
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

    private val base = 10 * 60 * MINUTE
    private var reads = 0

    private fun show(s: Sleep, fadeMs: Long = FadeLength.M1.ms) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AappsTheme(DozrAccent) {
                SleepScreen(
                    s, fadeMs, haptics = null, onChoose = {}, onStart = {}, onStop = {}, onExtend = {}, onSettings = {},
                    wallText = { "" }, clock = { reads++; base + rule.mainClock.currentTime },
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
        show(Sleep(30))
        assertEquals(0, readsDuring(30_000))
    }

    @Test fun doneIsStill() {
        show(Sleep(30, Mode.DONE, base - 31 * MINUTE, base - MINUTE))
        assertEquals(0, readsDuring(30_000))
    }

    @Test fun runningTicksAboutOncePerSecond() {
        show(Sleep(30).start(base))
        val n = readsDuring(10_000)
        assertTrue("n=$n", n in 9..12)
    }

    // Ausblenden: Mond und Kurve 4/s, nie mehr
    @Test fun fadingTicksFourPerSecond() {
        show(Sleep(5, Mode.RUNNING, base - 4 * MINUTE - 20_000, base + 40_000))
        val n = readsDuring(10_000)
        assertTrue("n=$n", n in 36..55)
    }

    // Laeuft ab: danach steht der Ticker (der Dienst setzt "fertig")
    @Test fun stopsAtEnd() {
        show(Sleep(5, Mode.RUNNING, base - 5 * MINUTE + 5_000, base + 5_000))
        rule.mainClock.advanceTimeBy(10_000)
        assertEquals(0, readsDuring(30_000))
    }
}
