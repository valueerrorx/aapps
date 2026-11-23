package solutions.xapient.magnr

import androidx.activity.compose.setContent
import androidx.compose.runtime.key
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

// Sensor und Ton nur im Vordergrund: ON_STOP stoppt, ON_START startet wieder; Pause haelt beides aus
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class LifecycleTest {
    @get:Rule(order = 0) val setup = object : ExternalResource() {
        override fun before() {
            Prefs(RuntimeEnvironment.getApplication()).apply {
                introSeen = true
                settings = Settings(sound = true)
            }
        }
    }

    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    private val source = FakeSource()
    private val beeper = FakeBeeper()

    private fun settle() {
        repeat(3) {
            rule.mainClock.advanceTimeByFrame()
            shadowOf(android.os.Looper.getMainLooper()).idle()
        }
    }

    @Test fun sensorAndSoundFollowLifecycle() {
        rule.runOnUiThread { rule.activity.setContent { key("test") { MagnrApp({ source }, { beeper }) } } }
        settle()
        assertTrue(source.running)
        assertTrue(beeper.running)
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        settle()
        assertFalse(source.running)
        assertFalse(beeper.running)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        settle()
        assertTrue(source.running)
        assertTrue(beeper.running)
        assertEquals(2, source.starts)
        rule.onNodeWithContentDescription("Pause").performClick()
        settle()
        assertFalse(source.running)
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        settle()
        // pausiert bleibt pausiert, auch nach Rueckkehr
        assertFalse(source.running)
        assertFalse(beeper.running)
    }
}
