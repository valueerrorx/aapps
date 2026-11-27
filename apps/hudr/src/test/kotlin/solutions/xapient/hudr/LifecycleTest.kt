package solutions.xapient.hudr

import android.Manifest
import androidx.activity.compose.setContent
import androidx.compose.runtime.key
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

// Standort nur im Vordergrund: ON_STOP stoppt, ON_START startet wieder
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class LifecycleTest {
    // Rechte vor dem Start der Activity, wie nach einer frueheren Zustimmung
    @get:Rule(order = 0) val granted = object : ExternalResource() {
        override fun before() {
            val app = RuntimeEnvironment.getApplication()
            shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            Prefs(app).askedLocation = true
            // echte AndroidGps in hudSurvivesRecreate: Standort im System an
            val lm = app.getSystemService(android.location.LocationManager::class.java)
            shadowOf(lm).setLocationEnabled(true)
            shadowOf(lm).setProviderEnabled(android.location.LocationManager.GPS_PROVIDER, true)
        }
    }

    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    private val gps = FakeGps()

    @Test fun gpsFollowsLifecycle() {
        rule.runOnUiThread { rule.activity.setContent { key("test") { HudrRoot({ gps }, now = { 0L }) } } }
        rule.waitForIdle()
        assertTrue(gps.running)
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        rule.waitForIdle()
        assertFalse(gps.running)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.waitForIdle()
        assertTrue(gps.running)
        assertEquals(2, gps.starts)
    }

    // HUD uebersteht Neuaufbau der Activity (SavedState)
    @Test fun hudSurvivesRecreate() {
        rule.onNodeWithText("HUD", ignoreCase = true).performClick()
        rule.waitForIdle()
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Reset trip").assertDoesNotExist()
        rule.onNodeWithContentDescription("Speed").assertExists()
    }
}
