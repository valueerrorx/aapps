package com.valueerrorx.spinr.screenshots

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.valueerrorx.aapps.core.ui.AappsTheme
import com.valueerrorx.spinr.SpinrAccent
import com.valueerrorx.spinr.domain.Palette
import com.valueerrorx.spinr.domain.Wheel
import com.valueerrorx.spinr.domain.WheelMath
import com.valueerrorx.spinr.starterWheels
import com.valueerrorx.spinr.templateGroups
import com.valueerrorx.spinr.ui.EditorScreen
import com.valueerrorx.spinr.ui.SpinScreen
import com.valueerrorx.spinr.ui.SpinState
import com.valueerrorx.spinr.ui.SpinrPalette
import com.valueerrorx.spinr.ui.SpinrAbout
import com.valueerrorx.spinr.ui.SpinrSettings
import com.valueerrorx.spinr.ui.TemplatesScreen
import com.valueerrorx.spinr.ui.WheelsScreen
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Screenshot-Zustaende laut SPEC.md; record: :apps:spinr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SpinScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(SpinrAccent) { content() }
        }
    }

    // Vorlagen in der Sprache des gerade gesetzten Qualifiers
    private fun res() = RuntimeEnvironment.getApplication().resources
    private fun presets() = starterWheels(res())
    private fun food() = presets()[0]

    @Composable
    private fun Spin(state: SpinState, name: String, removed: Int = 0, confetti: Float = -1f) {
        SpinScreen(
            state, name, canRemove = state.labels.size > 2, removedCount = removed, haptics = null, onTick = null,
            onOpenWheels = {}, onOpenSettings = {}, onRemove = {}, onRestore = {},
            animate = false, confettiAt = confetti,
        )
    }

    private fun idle(w: Wheel) = SpinState(w.labels).apply { pose(17f) }

    @Composable
    private fun Idle() = food().let { Spin(idle(it), it.name) }

    @Test fun wheel() = shot("wheel") { Idle() }

    @Test fun wheel_de() = shot("wheel_de", "+de") { Idle() }

    @Test fun wheel_360dp() = shot("wheel_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") { Idle() }

    @Test fun wheel_land() = shot("wheel_land", "+land") { Idle() }

    @Test fun wheel_font130() = shot("wheel_font130", fontScale = 1.3f) { Idle() }

    @Test fun wheel_font200() = shot("wheel_font200", fontScale = 2f) { Idle() }

    // schnell, Zunge gerade von einem Stift mitgenommen
    @Test fun spinning() = shot("spinning") {
        val w = food()
        Spin(SpinState(w.labels).apply { pose(-123f, speed = 1250f, flap = 0.7f) }, w.name)
    }

    @Composable
    private fun Result() {
        val w = food()
        // Sushi, knapp neben der Mitte des Segments
        val angle = WheelMath.angleFor(1, w.labels.size).toFloat() + 9f
        Spin(SpinState(w.labels).apply { pose(angle, winner = 1) }, w.name, confetti = 0.42f)
    }

    @Test fun result() = shot("result") { Result() }

    @Test fun result_de() = shot("result_de", "+de") { Result() }

    @Test fun result_land() = shot("result_land", "+land") { Result() }

    // Gewinner genau unter dem Zeiger: das hervorgehobene Segment muss oben stehen
    @Test fun many() = shot("many") {
        val labels = (1..30).map { "$it" }
        val state = SpinState(labels).apply { pose(WheelMath.angleFor(16, 30).toFloat(), winner = 16) }
        assertEquals(16, state.current)
        Spin(state, "1 – 30")
    }

    // 50 Eintraege, Ruhe: kleinste Schrift
    @Test fun many50() = shot("many50") {
        val labels = (1..50).map { "Option $it" }
        Spin(SpinState(labels).apply { pose(3f) }, "1 – 50")
    }

    @Test fun many_360dp() = shot("many_360dp", "w360dp-h740dp-normal-long-notround-any-xhdpi-keyshidden-nonav") {
        val labels = (1..30).map { "$it" }
        Spin(SpinState(labels).apply { pose(3f) }, "1 – 30")
    }

    @Test fun many50_font200() = shot("many50_font200", fontScale = 2f) {
        val labels = (1..50).map { "Option $it" }
        Spin(SpinState(labels).apply { pose(3f) }, "1 – 50")
    }

    @Test fun removed() = shot("removed") {
        val w = food()
        val ids = w.labels.indices.filter { it != 1 && it != 5 }
        val colors = Palette.colors(ids, w.labels.size, SpinrPalette.size).map { SpinrPalette[it] }
        Spin(SpinState(ids.map { w.labels[it] }, colors).apply { pose(40f) }, w.name, removed = 2)
    }

    @Composable
    private fun Wheels() {
        val p = presets()
        WheelsScreen(p, 0, {}, {}, {}, {}, {})
    }

    @Test fun wheels() = shot("wheels") { Wheels() }

    @Composable
    private fun Templates() = TemplatesScreen(templateGroups(res()), {}, {})

    @Test fun templates_de() = shot("templates_de", "+de") { Templates() }

    @Test fun templates_de_font200() = shot("templates_de_font200", "+de", fontScale = 2f) { Templates() }

    // Vorlage als Rad: Himmelsrichtungen und Buchstaben A-Z (26, kurze Eintraege)
    private fun template(name: String) = templateGroups(res()).flatMap { it.wheels }.first { it.name == name }

    @Test fun compass_de() = shot("compass_de", "+de") { template("Himmelsrichtungen").let { Spin(idle(it), it.name) } }

    @Test fun letters_de() = shot("letters_de", "+de") { template("Buchstaben A–Z").let { Spin(idle(it), it.name) } }

    @Composable
    private fun Editor() {
        val w = food()
        EditorScreen(w.copy(entries = w.entries.take(6) + ""), canDelete = true, onChange = {}, onDelete = {}, onBack = {})
    }

    @Test fun editor() = shot("editor") { Editor() }

    @Test fun editor_de() = shot("editor_de", "+de") { Editor() }

    @Composable
    private fun Settings() {
        SpinrSettings(
            haptics = true, onHaptics = {}, sound = false, onSound = {},
            activeName = food().name, onWheels = {}, onAbout = {}, onBack = {},
        )
    }

    @Test fun settings() = shot("settings") { Settings() }

    @Test fun settings_de() = shot("settings_de", "+de") { Settings() }

    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f) { Settings() }

    @Test fun about() = shot("about") { SpinrAbout("0.1.0") {} }

    @Test fun about_de() = shot("about_de", "+de") { SpinrAbout("0.1.0") {} }
}
