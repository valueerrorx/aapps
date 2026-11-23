package solutions.xapient.focusr

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import android.Manifest
import android.app.NotificationManager
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.robolectric.Shadows.shadowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.focusr.domain.Mode
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Gesten auf der echten Scheibe und die Hauptaktion
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GestureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun dial() = rule.onNodeWithContentDescription("Timer")

    private fun shows(text: String) = dial().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text))

    private fun TouchInjectionScope.at(deg: Double, radius: Float): Offset {
        val a = Math.toRadians(deg)
        return Offset(centerX + (sin(a) * radius).toFloat(), centerY - (cos(a) * radius).toFloat())
    }

    // Finger auf dem Kreis von a nach b (Grad ab 12 Uhr, im Uhrzeigersinn) in kleinen Schritten
    private fun TouchInjectionScope.arc(from: Double, to: Double) {
        val r = min(width, height) / 2f * 0.6f
        down(at(from, r))
        val steps = 40
        for (i in 1..steps) moveTo(at(from + (to - from) * i / steps, r))
        up()
    }

    @Test fun defaultIs25Minutes() {
        shows("25 minutes")
    }

    @Test fun dragFromTwelveToThreeSetsFifteen() {
        dial().performTouchInput { arc(0.0, 90.0) }
        shows("15 minutes")
        assertEquals(15 * 60_000L, Prefs(rule.activity).timer.setMs)
    }

    @Test fun dragPastTwelvePinsFull() {
        dial().performTouchInput { arc(320.0, 400.0) }
        shows("60 minutes")
    }

    @Test fun dragBackPastTwelvePinsEmpty() {
        dial().performTouchInput { arc(40.0, -30.0) }
        shows("0 seconds")
        rule.onNodeWithText("Drag the disc to set a time").assertExists()
    }

    @Test fun tapDoesNothing() {
        dial().performTouchInput { click(Offset(centerX + width * 0.3f, centerY)) }
        shows("25 minutes")
    }

    @Test fun startPauseResumeReset() {
        rule.onNodeWithText("START").performClick()
        rule.onNodeWithText("PAUSE").assertExists()
        // gespeichert: uebersteht Prozess-Tod
        assertEquals(Mode.RUNNING, Prefs(rule.activity).timer.mode)
        rule.onNodeWithText("PAUSE").performClick()
        rule.onNodeWithText("RESUME").assertExists()
        rule.onNodeWithText("PAUSED").assertExists()
        assertEquals(Mode.PAUSED, Prefs(rule.activity).timer.mode)
        rule.onNodeWithContentDescription("Reset").performClick()
        rule.onNodeWithText("START").assertExists()
        shows("25 minutes")
        assertEquals(Mode.IDLE, Prefs(rule.activity).timer.mode)
    }

    @Test fun zeroCannotStart() {
        dial().performTouchInput { arc(40.0, -30.0) }
        rule.onNodeWithText("START").performClick()
        rule.onNodeWithText("START").assertExists()
        assertEquals(Mode.IDLE, Prefs(rule.activity).timer.mode)
    }

    @Test fun dragWhileRunningKeepsRunning() {
        rule.onNodeWithText("START").performClick()
        // Kante steht bei 25 min = 150 Grad
        dial().performTouchInput { arc(150.0, 180.0) }
        rule.onNodeWithText("PAUSE").assertExists()
        val t = Prefs(rule.activity).timer
        assertEquals(Mode.RUNNING, t.mode)
        assertEquals(30 * 60_000L, t.setMs)
    }

    // Beim Lauf greift nur die Kante, sonst verstellt ein Streifen die Restzeit
    @Test fun dragAwayFromEdgeWhileRunningIsIgnored() {
        rule.onNodeWithText("START").performClick()
        dial().performTouchInput { arc(0.0, 90.0) }
        val t = Prefs(rule.activity).timer
        assertEquals(Mode.RUNNING, t.mode)
        assertEquals(25 * 60_000L, t.setMs)
    }

    // TalkBack: Regler in Rasten verstellen
    @Test fun talkBackSetProgressSetsTime() {
        dial().performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }
        shows("30 minutes")
        assertEquals(30 * 60_000L, Prefs(rule.activity).timer.setMs)
    }

    // Im Hintergrund Countdown-Benachrichtigung, beim Zurueckkommen weg
    @Test fun countdownNotificationOnlyInBackground() {
        val app = rule.activity.application
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val nm = app.getSystemService(NotificationManager::class.java)
        rule.onNodeWithText("START").performClick()
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertNotNull(shadowOf(nm).getNotification(Notifier.ID_RUNNING))
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        assertNull(shadowOf(nm).getNotification(Notifier.ID_RUNNING))
    }

    // Leerlauf im Hintergrund: keine Benachrichtigung
    @Test fun noCountdownWhenIdle() {
        val app = rule.activity.application
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertNull(shadowOf(app.getSystemService(NotificationManager::class.java)).getNotification(Notifier.ID_RUNNING))
    }
}
