package solutions.xapient.hudr.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.hudr.Settings
import solutions.xapient.hudr.domain.Alert
import solutions.xapient.hudr.domain.GpsPhase
import solutions.xapient.hudr.domain.Reading
import solutions.xapient.hudr.domain.SpeedUnit
import solutions.xapient.hudr.ui.HudScreen
import solutions.xapient.hudr.ui.HudrAbout
import solutions.xapient.hudr.ui.HudrAccent
import solutions.xapient.hudr.ui.HudrSettings
import solutions.xapient.hudr.ui.LocState
import solutions.xapient.hudr.ui.SpeedScreen

// Screenshot-Zustaende laut SPEC.md; record: :apps:hudr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class HudrScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(HudrAccent) { content() }
        }
    }

    // trend = Vorhersage in 3 s (Pfeil am Band)
    private fun fix(speed: Float, max: Float, avg: Float, dist: Double, trend: Float = Float.NaN, alert: Alert = Alert.NONE, phase: GpsPhase = GpsPhase.FIX) =
        Reading(phase, if (phase == GpsPhase.FIX) speed else Float.NaN, speed > 0f, 4.2f, 11, 24, max, avg, dist, alert, trend)

    private val drive = fix(87.4f, 112f, 64f, 23.42, trend = 96f)
    private val near = fix(47f, 52f, 38f, 4.8, trend = 50f, alert = Alert.NEAR)
    private val over = fix(58f, 61f, 41f, 6.1, trend = 55f, alert = Alert.OVER)
    private val stopped = fix(0f, 71f, 43f, 12.7)
    private val searching = Reading.SEARCHING
    private val lost = fix(52f, 74f, 49f, 9.3, phase = GpsPhase.LOST)
    private val mph = fix(54f, 68f, 47f, 18.6)

    private val limit50 = Settings(limitOn = true, limit = 50)

    @Composable
    private fun Main(r: Reading, s: Settings = Settings(), loc: LocState = LocState.READY) {
        SpeedScreen(
            { r }, remember { mutableIntStateOf(0) }, loc, s, null,
            onHud = {}, onReset = {}, onLoc = {}, onSettings = {}, animate = false,
        )
    }

    @Composable
    private fun Hud(r: Reading, s: Settings = Settings()) {
        HudScreen({ r }, remember { mutableIntStateOf(0) }, s, null, onExit = {}, animate = false)
    }

    @Composable private fun Drive() = Main(drive)
    @Composable private fun Ask() = Main(searching, loc = LocState.ASK)

    @Test fun drive() = shot("drive") { Drive() }
    @Test fun drive_de() = shot("drive_de", "+de") { Drive() }
    @Test fun drive_land() = shot("drive_land", "+land") { Drive() }
    @Test fun drive_font200() = shot("drive_font200", fontScale = 2f) { Drive() }
    @Test fun drive_land_font200() = shot("drive_land_font200", "+land", fontScale = 2f) { Drive() }
    @Test fun drive_360dp() = shot("drive_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Drive() }

    @Test fun near() = shot("near") { Main(near, limit50) }
    @Test fun over() = shot("over") { Main(over, limit50) }
    @Test fun stopped() = shot("stopped") { Main(stopped) }
    @Test fun searching() = shot("searching") { Main(searching) }
    @Test fun lost() = shot("lost") { Main(lost) }
    @Test fun mph() = shot("mph") { Main(mph, Settings(SpeedUnit.MPH)) }

    @Test fun hud() = shot("hud") { Hud(drive) }
    @Test fun hud_land() = shot("hud_land", "+land") { Hud(drive) }
    @Test fun hud_over() = shot("hud_over") { Hud(over, limit50) }
    @Test fun hud_searching() = shot("hud_searching") { Hud(searching) }
    @Test fun hud_land_font200() = shot("hud_land_font200", "+land", fontScale = 2f) { Hud(drive, limit50) }
    @Test fun hud_unmirrored() = shot("hud_unmirrored", "+land") { Hud(drive, Settings(mirror = false)) }

    @Test fun loc_ask() = shot("loc_ask") { Ask() }
    @Test fun loc_ask_de() = shot("loc_ask_de", "+de") { Ask() }
    @Test fun loc_ask_land() = shot("loc_ask_land", "+land") { Ask() }
    @Test fun loc_ask_font200() = shot("loc_ask_font200", fontScale = 2f) { Ask() }
    @Test fun loc_ask_land_font200() = shot("loc_ask_land_font200", "+land", fontScale = 2f) { Ask() }
    @Test fun loc_coarse() = shot("loc_coarse") { Main(searching, loc = LocState.COARSE) }
    @Test fun loc_blocked() = shot("loc_blocked") { Main(searching, loc = LocState.BLOCKED) }
    @Test fun gps_off() = shot("gps_off") { Main(searching, loc = LocState.GPS_OFF) }

    @Composable private fun Settings_() = HudrSettings(limit50, {}, null, {}, {})

    @Test fun settings() = shot("settings") { Settings_() }
    @Test fun settings_de() = shot("settings_de", "+de") { Settings_() }
    @Test fun settings_land() = shot("settings_land", "+land") { Settings_() }
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings_() }
    @Test fun settings_land_font200() = shot("settings_land_font200", "+land", fontScale = 2f) { Settings_() }

    @Test fun about() = shot("about") { HudrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { HudrAbout("0.1.0") {} }
    @Test fun about_font200() = shot("about_font200", fontScale = 2f) { HudrAbout("0.1.0") {} }
}
