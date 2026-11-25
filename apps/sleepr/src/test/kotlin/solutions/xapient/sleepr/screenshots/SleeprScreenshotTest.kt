package solutions.xapient.sleepr.screenshots

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.billing.Owned
import solutions.xapient.aapps.core.billing.PurchaseSource
import solutions.xapient.aapps.core.billing.TrialGate
import solutions.xapient.aapps.core.billing.TrialPolicy
import solutions.xapient.sleepr.Display
import solutions.xapient.sleepr.MainActivity
import solutions.xapient.sleepr.NoiseEngine
import solutions.xapient.sleepr.NoiseService

// Screenshot-Zustaende laut SPEC.md; record: :apps:sleepr:recordRoborazziDebug
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SleeprScreenshotTest {

    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    @After fun reset() {
        setEngine(null)
        TrialGate.factory = defaultFactory
    }

    @Test fun idle() = shot("idle")
    @Test fun playing() { setEngine(fakeEngine()); shot("playing") }
    // Liste ganz unten: Timer als Abschluss, keine Erklärtexte
    @Test fun idle_bottom() = shot("idle_bottom", bottom = true)
    @Test fun idle_land() = shot("idle_land", "+land")
    @Test fun idle_font200() = shot("idle_font200", fontScale = 2f)
    @Test fun idle_tablet() = shot("idle_tablet", "w800dp-h1280dp-port-xhdpi")

    // Wellen-Schalter und Mischungen in den Einstellungen ausgeblendet
    @Test fun idle_plain() { plain(); shot("idle_plain") }
    @Test fun idle_plain_land() { plain(); shot("idle_plain_land", "+land") }

    @Test fun settings() = shot("settings", screen = MainActivity.Screen.SETTINGS)
    @Test fun settings_land() = shot("settings_land", "+land", screen = MainActivity.Screen.SETTINGS)
    @Test fun settings_font200() = shot("settings_font200", fontScale = 2f, screen = MainActivity.Screen.SETTINGS)
    @Test fun settings_plain() { plain(); shot("settings_plain", screen = MainActivity.Screen.SETTINGS) }
    @Test fun settings_tablet() = shot("settings_tablet", "w800dp-h1280dp-port-xhdpi", screen = MainActivity.Screen.SETTINGS)
    @Test fun about() = shot("about", screen = MainActivity.Screen.ABOUT)
    @Test fun about_font200() = shot("about_font200", fontScale = 2f, screen = MainActivity.Screen.ABOUT)

    // Grundsprache Englisch
    @Test fun idle_en() = shot("idle_en", lang = "en")
    @Test fun settings_en() = shot("settings_en", lang = "en", screen = MainActivity.Screen.SETTINGS)

    // Testphase abgelaufen: Dankes-Bildschirm (Preis da bzw. offline)
    @Test fun thanks() { expired("1,49 €"); shot("thanks") }
    @Test fun thanks_en() { expired("€1.49"); shot("thanks_en", lang = "en") }
    @Test fun thanks_offline() { expired(null); shot("thanks_offline") }
    @Test fun thanks_font200() { expired("1,49 €"); shot("thanks_font200", fontScale = 2f) }

    private val defaultFactory = TrialGate.factory

    private fun expired(price: String?) {
        TrialGate.factory = { ctx ->
            val prefs = ctx.getSharedPreferences("trial-test", Context.MODE_PRIVATE)
            prefs.edit().putLong(TrialGate.KEY_FIRST, 0L).commit()
            TrialGate(prefs, FixedPurchases(price)) { TrialPolicy.TRIAL_MS }
        }
    }

    private class FixedPurchases(private val p: String?) : PurchaseSource {
        override var onUpdate: ((Owned) -> Unit)? = null
        override fun query(onResult: (Owned?) -> Unit) = onResult(if (p == null) null else Owned.NONE)
        override fun price(onResult: (String?) -> Unit) = onResult(p)
        override fun buy(activity: Activity) = false
        override fun close() = Unit
    }

    private fun plain() = Display(RuntimeEnvironment.getApplication()).run { showWaves = false; showMixes = false }

    // Hauptbildschirm als Ganzes rendern (Views, kein Compose); Standard deutsch wie bisher, _en englisch
    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, screen: MainActivity.Screen = MainActivity.Screen.MAIN, lang: String = "de", bottom: Boolean = false) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        RuntimeEnvironment.setQualifiers("+$lang")
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        if (screen != MainActivity.Screen.MAIN) activity.show(screen)
        val root = activity.window.decorView
        val dm = activity.resources.displayMetrics
        root.measure(
            View.MeasureSpec.makeMeasureSpec(dm.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(dm.heightPixels, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, dm.widthPixels, dm.heightPixels)
        if (bottom) scrollAll(root)
        val bmp = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bmp))
        bmp.captureRoboImage("screenshots/$name.png", options)
    }

    // Alle ScrollViews ans Ende scrollen
    private fun scrollAll(v: View) {
        if (v is ScrollView && v.childCount > 0) v.scrollTo(0, maxOf(0, v.getChildAt(0).height - v.height))
        if (v is ViewGroup) for (i in 0 until v.childCount) scrollAll(v.getChildAt(i))
    }

    // Engine mit laufendem Status und Timer, ohne Audio-Thread zu starten
    private fun fakeEngine() = NoiseEngine {}.also { e ->
        fun set(f: String, v: Any) = NoiseEngine::class.java.getDeclaredField(f).apply { isAccessible = true }.set(e, v)
        set("running", true)
        set("endFrame", 48000L * 60 * 24 + 48000L * 17)
        set("fadeStart", 48000L * 60 * 20)
    }

    private fun setEngine(e: NoiseEngine?) =
        NoiseService::class.java.getDeclaredField("engine").apply { isAccessible = true }.set(null, e)
}
