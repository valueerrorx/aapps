package solutions.xapient.thinkr

import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.Round
import solutions.xapient.thinkr.domain.switches.SwitchesRules
import solutions.xapient.thinkr.ui.ThinkrAccent

// Rahmen einer Runde: Pause bei ON_STOP, Zurueck pausiert bzw. beendet, Rundenende meldet genau einmal
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GameHostTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val finished = mutableListOf<Play>()
    private var quits = 0

    private fun settle() {
        repeat(3) {
            rule.mainClock.advanceTimeByFrame()
            shadowOf(android.os.Looper.getMainLooper()).idle()
        }
    }

    private fun launch(def: GameDef<*, *> = Games.Echo) {
        rule.runOnUiThread {
            rule.activity.setContent {
                AappsTheme(ThinkrAccent) {
                    def.Host(42, 3, false, null, null, onFinish = { finished += it }, onQuit = { quits++ })
                }
            }
        }
        rule.mainClock.autoAdvance = false
        settle()
    }

    private fun paused() = rule.onNodeWithText("END ROUND").assertExists()

    private fun notPaused() = rule.onNodeWithText("END ROUND").assertDoesNotExist()

    private fun countIn() {
        rule.mainClock.advanceTimeBy(3 * 650L + 100)
        settle()
    }

    @Test fun stopPausesAndStaysPausedAfterReturn() {
        launch()
        countIn()
        notPaused()
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        settle()
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        settle()
        paused()
        rule.onNodeWithText("RESUME").performClick()
        settle()
        notPaused()
    }

    @Test fun backPausesThenQuits() {
        launch()
        countIn()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
        paused()
        assertEquals(0, quits)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
        assertEquals(1, quits)
    }

    @Test fun pauseStopsGameTime() {
        launch()
        countIn()
        rule.onNodeWithContentDescription("Pause").performClick()
        settle()
        // eine Minute in der Pause: keine Spielzeit vergeht
        rule.mainClock.advanceTimeBy(70_000)
        settle()
        assertTrue(finished.isEmpty())
        paused()
    }

    @Test fun roundEndsOnceAfterSixtySeconds() {
        launch()
        countIn()
        rule.mainClock.advanceTimeBy(Round.DURATION_MS + 2_000)
        settle()
        assertEquals(1, finished.size)
        assertTrue(finished[0].round.isOver)
        // keine Antworten: nur Fehler, keine Punkte
        assertEquals(0, finished[0].round.score)
        assertTrue(finished[0].round.wrong > 10)
    }

    // Sternbild, Tinte, Schwarm: Pause haelt die Spielzeit an, Ende genau einmal nach 60 s Spielzeit
    private fun pauseThenFinish(def: GameDef<*, *>) {
        launch(def)
        countIn()
        rule.mainClock.advanceTimeBy(20_000)
        settle()
        rule.onNodeWithContentDescription("Pause").performClick()
        settle()
        rule.mainClock.advanceTimeBy(70_000)
        settle()
        assertTrue(finished.isEmpty())
        rule.onNodeWithText("RESUME").performClick()
        settle()
        countIn()
        rule.mainClock.advanceTimeBy(Round.DURATION_MS)
        settle()
        assertEquals(1, finished.size)
        assertEquals(Round.DURATION_MS, finished[0].round.now)
        assertEquals(0, finished[0].round.score)
    }

    @Test fun starsPausesAndEndsOnce() = pauseThenFinish(Games.Stars)

    @Test fun inkPausesAndEndsOnce() = pauseThenFinish(Games.Ink)

    @Test fun swarmPausesAndEndsOnce() = pauseThenFinish(Games.Swarm)

    @Test fun bouncePausesAndEndsOnce() = pauseThenFinish(Games.Bounce)

    // Weichen: 90 s Spielzeit, Pause haelt auch die Perlen an
    @Test fun switchesRoundLastsNinetySeconds() {
        launch(Games.Switches)
        countIn()
        rule.mainClock.advanceTimeBy(Round.DURATION_MS + 1_000)
        settle()
        assertTrue(finished.isEmpty())
        rule.onNodeWithContentDescription("Pause").performClick()
        settle()
        rule.mainClock.advanceTimeBy(60_000)
        settle()
        assertTrue(finished.isEmpty())
        rule.onNodeWithText("RESUME").performClick()
        settle()
        countIn()
        rule.mainClock.advanceTimeBy(30_000)
        settle()
        assertEquals(1, finished.size)
        assertEquals(SwitchesRules.DURATION_MS, finished[0].round.now)
    }
}
