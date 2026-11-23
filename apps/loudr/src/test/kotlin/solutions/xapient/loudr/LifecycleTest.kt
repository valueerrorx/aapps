package solutions.xapient.loudr

import android.Manifest
import androidx.activity.compose.setContent
import androidx.compose.runtime.key
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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

// Mikrofon nur im Vordergrund: ON_STOP stoppt, ON_START startet wieder; Pause haelt es aus
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class LifecycleTest {
    // Recht vor dem Start der Activity, wie nach einer frueheren Zustimmung
    @get:Rule(order = 0) val granted = object : ExternalResource() {
        override fun before() {
            val app = RuntimeEnvironment.getApplication()
            shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
            Prefs(app).askedMic = true
        }
    }

    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    private val meter = FakeMeter()

    // Uhr steht (beim Messen laeuft jedes Bild): ein paar Bilder von Hand, Hauptthread leeren
    private fun settle() {
        repeat(3) {
            rule.mainClock.advanceTimeByFrame()
            shadowOf(android.os.Looper.getMainLooper()).idle()
        }
    }

    @Test fun micFollowsLifecycle() {
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { rule.activity.setContent { key("test") { LoudrApp { meter } } } }
        settle()
        assertTrue(meter.running)
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        settle()
        assertFalse(meter.running)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        settle()
        assertTrue(meter.running)
        assertEquals(2, meter.starts)
        rule.onNodeWithText("Pause", ignoreCase = true).performClick()
        settle()
        assertFalse(meter.running)
        rule.onNodeWithText("Resume", ignoreCase = true).assertExists()
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        settle()
        // pausiert bleibt pausiert, auch nach Rueckkehr
        assertFalse(meter.running)
    }
}
