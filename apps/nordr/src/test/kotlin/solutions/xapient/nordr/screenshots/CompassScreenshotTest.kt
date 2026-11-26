package solutions.xapient.nordr.screenshots

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.nordr.NordrAccent
import solutions.xapient.nordr.domain.Accuracy
import solutions.xapient.nordr.domain.CompassReading
import solutions.xapient.nordr.domain.NorthRef
import solutions.xapient.nordr.domain.Region
import solutions.xapient.nordr.domain.Regions
import solutions.xapient.nordr.sensor.AndroidDeclination
import solutions.xapient.nordr.ui.CompassScreen
import solutions.xapient.nordr.ui.CompassState
import solutions.xapient.nordr.ui.NordrAbout
import solutions.xapient.nordr.ui.NordrSettings
import solutions.xapient.nordr.ui.RegionPicker
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Screenshot-Zustaende laut SPEC.md; record: ./gradlew :apps:nordr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class CompassScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(NordrAccent) { content() }
        }
    }

    @Composable
    private fun Compass(
        state: CompassState,
        ref: NorthRef = NorthRef.MAGNETIC,
        shine: Float = -1f,
        phase: Float = 0.3f,
    ) {
        CompassScreen(state, ref, null, {}, {}, animate = false, shineOverride = shine, hintPhase = phase)
    }

    // 247° WSW, Rose schwingt noch nach, Handy leicht gekippt
    private fun headingState() = CompassState().apply {
        show(CompassReading(valid = true, heading = 247f, magnetic = 247f))
        physics.setTilt(0.12f, 0.3f)
        physics.dial.pose(253f, -18f)
    }

    @Test fun heading() = shot("heading") { Compass(headingState()) }

    @Test fun heading_de() = shot("heading_de", "+de") { Compass(headingState()) }

    @Test fun heading_360dp() =
        shot("heading_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Compass(headingState()) }

    @Test fun heading_land() = shot("heading_land", "+land") { Compass(headingState()) }

    @Test fun heading_font130() = shot("heading_font130", fontScale = 1.3f) { Compass(headingState()) }

    @Test fun heading_font200() = shot("heading_font200", fontScale = 2f) { Compass(headingState()) }

    @Test fun true_north() = shot("true_north") {
        Compass(CompassState().apply { show(CompassReading(valid = true, heading = 4f, magnetic = 359f)) }, NorthRef.TRUE)
    }

    private fun courseState() = CompassState().apply {
        show(CompassReading(valid = true, heading = 232f, magnetic = 232f, course = 250f, deviation = 18f))
        physics.setTilt(0f, 0.2f)
    }

    @Test fun course() = shot("course") { Compass(courseState()) }

    @Test fun course_land() = shot("course_land", "+land") { Compass(courseState()) }

    @Test fun course_font200() = shot("course_font200", fontScale = 2f) { Compass(courseState()) }

    @Test fun course_land_font200() = shot("course_land_font200", "+land", fontScale = 2f) { Compass(courseState()) }

    @Test fun on_course() = shot("on_course") {
        Compass(
            CompassState().apply {
                show(CompassReading(valid = true, heading = 250f, magnetic = 250f, course = 250f, onCourse = true))
            },
            shine = 0.4f,
        )
    }

    private fun calibrateState() = CompassState().apply {
        show(CompassReading(valid = true, heading = 81f, magnetic = 81f, accuracy = Accuracy.LOW))
    }

    @Test fun calibrate() = shot("calibrate") { Compass(calibrateState()) }

    @Test fun calibrate_de() = shot("calibrate_de", "+de") { Compass(calibrateState()) }

    @Test fun calibrate_land() = shot("calibrate_land", "+land") { Compass(calibrateState()) }

    @Test fun interference() = shot("interference") {
        Compass(
            CompassState().apply {
                show(CompassReading(valid = true, heading = 140f, magnetic = 140f, accuracy = Accuracy.INTERFERENCE))
            },
            phase = 0.25f,
        )
    }

    // fester Zeitpunkt, damit die Deklination im Bild stabil bleibt
    private fun declination(r: Region) = AndroidDeclination.declination(r.lat, r.lon, FIXED_TIME)

    @Composable
    private fun Settings() {
        val vienna = Regions.byId("vienna")!!
        NordrSettings(
            northRef = NorthRef.TRUE, onNorthRef = {}, region = vienna, declination = declination(vienna),
            onRegion = {}, haptics = true, onHaptics = {}, onShowCalibration = {}, onAbout = {}, onBack = {},
        )
    }

    @Test fun settings() = shot("settings") { Settings() }

    @Test fun settings_de() = shot("settings_de", "+de") { Settings() }

    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings() }

    @Composable
    private fun Region_() = RegionPicker(Regions.byId("vienna")!!, ::declination, {}, {})

    // war bisher deutsch, steht aber in der EN-Liste
    @Test fun region() = shot("region") { Region_() }

    @Test fun region_de() = shot("region_de", "+de") { Region_() }

    // Store DE wie EN
    @Test fun course_de() = shot("course_de", "+de") { Compass(courseState()) }

    @Test fun on_course_de() = shot("on_course_de", "+de") {
        Compass(
            CompassState().apply {
                show(CompassReading(valid = true, heading = 250f, magnetic = 250f, course = 250f, onCourse = true))
            },
            shine = 0.4f,
        )
    }

    @Test fun true_north_de() = shot("true_north_de", "+de") {
        Compass(CompassState().apply { show(CompassReading(valid = true, heading = 4f, magnetic = 359f)) }, NorthRef.TRUE)
    }

    @Test fun heading_land_de() = shot("heading_land_de", "+de-land") { Compass(headingState()) }

    @Test fun about() = shot("about") { NordrAbout("0.1.0") {} }

    @Test fun about_de() = shot("about_de", "+de") { NordrAbout("0.1.0") {} }

    private companion object {
        const val FIXED_TIME = 1_780_272_000_000L // 1. Juni 2026
    }
}
