package solutions.xapient.dozr

import android.Manifest
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.dozr.domain.Mode
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Gesten am echten Himmel und die Bedienung darunter
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GestureTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    // Einzelstuecke pro Prozess vor dem Start der Activity leeren
    @get:Rule val rules: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            SleepStore.reset()
            SleepService.alive = false
        }
    }).around(compose)

    private val app get() = RuntimeEnvironment.getApplication()

    private fun prefs() = Prefs(compose.activity)

    private fun dial() = compose.onNodeWithContentDescription("Sleep timer")

    private fun shows(text: String) = dial().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text))

    // Punkt auf der Mondbahn: Grad ab rechtem Horizont, gegen den Uhrzeigersinn
    private fun TouchInjectionScope.at(deg: Double): Offset {
        val r = min(width, height) / 2f * 0.97f
        val pivotY = centerY + 0.36f * r
        val a = Math.toRadians(deg)
        return Offset(centerX + (cos(a) * r * 0.6f).toFloat(), pivotY - (sin(a) * r * 0.6f).toFloat())
    }

    private fun TouchInjectionScope.arc(from: Double, to: Double) {
        down(at(from))
        val steps = 40
        for (i in 1..steps) moveTo(at(from + (to - from) * i / steps))
        up()
    }

    private fun grant() = shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    @Test fun defaultIsThirtyMinutes() {
        shows("30 minutes")
    }

    @Test fun dragUpToZenithSetsSixty() {
        dial().performTouchInput { arc(45.0, 90.0) }
        shows("60 minutes")
        assertEquals(60, prefs().sleep.setMin)
    }

    @Test fun tapDoesNothing() {
        dial().performTouchInput { click(at(120.0)) }
        shows("30 minutes")
        assertEquals(30, prefs().sleep.setMin)
    }

    // Unter dem Horizont bleibt der Mond am linken Ende, auch wenn der Finger unten herum nach rechts geht
    @Test fun noJumpBelowHorizon() {
        dial().performTouchInput {
            down(at(135.0))
            for (d in 135..360 step 5) moveTo(at(d.toDouble()))
            up()
        }
        shows("120 minutes")
    }

    @Test fun quickChoice() {
        compose.onNodeWithContentDescription("45 minutes").performClick()
        shows("45 minutes")
        assertEquals(45, prefs().sleep.setMin)
    }

    @Test fun talkBackSetsInFiveMinuteSteps() {
        dial().performSemanticsAction(SemanticsActions.SetProgress) { it(18f) }
        shows("90 minutes")
    }

    @Test fun startExtendStop() {
        grant()
        compose.onNodeWithText("START").performClick()
        compose.onNodeWithText("STOP").assertExists()
        val s = prefs().sleep
        assertEquals(Mode.RUNNING, s.mode)
        assertEquals(30 * 60_000L, s.totalMs)
        assertEquals(SleepService::class.java.name, shadowOf(app).nextStartedService.component!!.className)
        compose.onNodeWithText("+10 min").performClick()
        assertEquals(40 * 60_000L, prefs().sleep.totalMs)
        compose.onNodeWithText("STOP").performClick()
        compose.onNodeWithText("START").assertExists()
        assertEquals(Mode.IDLE, prefs().sleep.mode)
    }

    // Beim Lauf verstellt Ziehen nichts
    @Test fun dragWhileRunningIsIgnored() {
        grant()
        compose.onNodeWithText("START").performClick()
        dial().performTouchInput { arc(20.0, 120.0) }
        assertEquals(30 * 60_000L, prefs().sleep.totalMs)
    }

    // Erst fragen, dann starten
    @Test fun asksForNotificationsBeforeFirstStart() {
        compose.onNodeWithText("START").performClick()
        assertNotNull(shadowOf(compose.activity).lastRequestedPermission)
        assertEquals(Mode.IDLE, prefs().sleep.mode)
    }

    // Einmal abgelehnt (System zeigt Hinweis): nicht von selbst fragen, sofort starten
    @Test fun noAutoAskAfterDenial() {
        shadowOf(app.packageManager).setShouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS, true)
        compose.onNodeWithText("START").performClick()
        assertNull(shadowOf(compose.activity).lastRequestedPermission)
        assertEquals(Mode.RUNNING, prefs().sleep.mode)
    }
}
