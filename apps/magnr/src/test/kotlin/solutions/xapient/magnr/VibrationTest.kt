package solutions.xapient.magnr

import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.key
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowVibrator

// Fund vibriert ueber den Vibrator (Wecker-Kategorie), nicht ueber die Touch-Haptik der View
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class VibrationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val source = FakeSource()

    // Erdfeld einschwingen, dann Metall: Stufe steigt bis "sehr nah"
    private fun find(haptics: Boolean): ShadowVibrator {
        val v = shadowOf(rule.activity.getSystemService(VibratorManager::class.java).defaultVibrator)
        v.setHasVibrator(true)
        Prefs(rule.activity).apply {
            introSeen = true
            settings = Settings(haptics = haptics)
        }
        rule.runOnUiThread {
            rule.activity.setContent { key("test") { MagnrRoot({ source }, { FakeBeeper() }) } }
        }
        rule.waitForIdle()
        rule.runOnUiThread { source.emit(48f, 50) }
        rule.waitForIdle()
        rule.runOnUiThread { source.emit(90f, 25) }
        rule.waitForIdle()
        return v
    }

    // ab API 31 speichert der Shadow vordefinierte Effekte als Segmente (PrebakedSegment, versteckte API)
    private fun lastEffectId(v: ShadowVibrator): Int {
        val f = ShadowVibrator::class.java.getDeclaredField("vibrationEffectSegments").apply { isAccessible = true }
        val seg = (f.get(v) as List<*>).single()!!
        return seg.javaClass.getMethod("getEffectId").invoke(seg) as Int
    }

    @Test fun strongFindVibratesAsAlarm() {
        val v = find(haptics = true)
        assertEquals(VibrationAttributes.USAGE_ALARM, (v.vibrationAttributesFromLastVibration as VibrationAttributes).usage)
        // Tick bei "schwach", der kraeftige Klick fuer "sehr nah" ueberholt die Entprellung
        assertEquals(VibrationEffect.EFFECT_HEAVY_CLICK, lastEffectId(v))
    }

    @Test fun switchedOffStaysStill() {
        assertFalse(find(haptics = false).isVibrating)
    }
}
