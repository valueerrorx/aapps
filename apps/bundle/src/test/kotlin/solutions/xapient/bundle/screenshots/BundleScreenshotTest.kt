package solutions.xapient.bundle.screenshots

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
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
import solutions.xapient.bundle.BundleAbout
import solutions.xapient.bundle.BundleAccent
import solutions.xapient.bundle.HomeScreen

// record: ./gradlew :apps:bundle:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class BundleScreenshotTest {
    private val options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))

    // ein typisches Handy: einige Apps schon da
    private val installed = setOf("levlr", "nordr", "focusr", "pulsr", "tunr", "thinkr", "moovr")

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) {
            AappsTheme(BundleAccent) { content() }
        }
    }

    @Composable
    private fun Home(scroll: Int = 0, apps: Set<String> = installed) =
        HomeScreen(apps, {}, {}, {}, ScrollState(scroll))

    @Test fun home() = shot("home") { Home() }

    @Test fun home_de() = shot("home_de", "+de") { Home() }

    @Test fun home_more() = shot("home_more") { Home(scroll = 1400) }

    @Test fun home_more_de() = shot("home_more_de", "+de") { Home(scroll = 1400) }

    @Test fun home_empty() = shot("home_empty") { Home(apps = emptySet()) }

    @Test fun home_land() = shot("home_land", "+land") { Home() }

    @Test fun home_font200() = shot("home_font200", fontScale = 2f) { Home(scroll = 900) }

    @Test fun about() = shot("about") { BundleAbout("1.0.0") {} }

    @Test fun about_de() = shot("about_de", "+de") { BundleAbout("1.0.0") {} }
}
