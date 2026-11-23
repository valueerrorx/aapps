package solutions.xapient.sleepr

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

// Einstellungen: Schalter, Zurück, Drehen, Mischung laden (deutsche Oberfläche)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "de")
class SettingsTest {
    private lateinit var levels: IntArray
    private lateinit var waves: BooleanArray
    private var tone = 0

    @Before fun save() { levels = Mixer.levels.copyOf(); waves = Mixer.waves.copyOf(); tone = Mixer.tone }
    @After fun restore() { levels.copyInto(Mixer.levels); waves.copyInto(Mixer.waves); Mixer.tone = tone }

    private fun content(a: MainActivity): View = a.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)

    private fun all(v: View): List<View> = if (v is ViewGroup) listOf(v) + (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else listOf(v)

    private fun clickText(a: MainActivity, text: String) {
        var v: View? = all(content(a)).first { it is TextView && it.text.toString() == text }
        while (v != null && !v.isClickable) v = v.parent as? View
        assertNotNull(text, v)
        v!!.performClick()
    }

    private fun waveButtons(a: MainActivity) = all(content(a)).filter { it.contentDescription?.startsWith("Wellen ") == true }

    @Test fun gearOpensSettingsAndBackCloses() {
        val a = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        all(content(a)).first { it.contentDescription == "Einstellungen" }.performClick()
        assertEquals(MainActivity.Screen.SETTINGS, a.screen)
        clickText(a, "Info")
        assertEquals(MainActivity.Screen.ABOUT, a.screen)
        @Suppress("DEPRECATION") a.onBackPressed()
        assertEquals(MainActivity.Screen.SETTINGS, a.screen)
        @Suppress("DEPRECATION") a.onBackPressed()
        assertEquals(MainActivity.Screen.MAIN, a.screen)
    }

    @Test fun toggleHidesWaveButtonsAndMixes() {
        val a = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val buttons = waveButtons(a)
        assertEquals(5, buttons.size)
        assertTrue(buttons.all { it.visibility == View.VISIBLE })
        assertTrue(all(content(a)).any { it is TextView && it.text == "Meer" && it.isShown })

        a.show(MainActivity.Screen.SETTINGS)
        clickText(a, "Wellen-Schalter anzeigen")
        clickText(a, "Mischungen anzeigen")
        val d = Display(RuntimeEnvironment.getApplication())
        assertFalse(d.showWaves); assertFalse(d.showMixes)

        a.show(MainActivity.Screen.MAIN)
        assertTrue(buttons.all { it.visibility == View.GONE })
        assertFalse(all(content(a)).any { it is TextView && it.text == "Meer" && it.isShown })
    }

    @Test fun toggleIsAnnouncedAsSwitch() {
        val a = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        a.show(MainActivity.Screen.SETTINGS)
        var row: View? = all(content(a)).first { it is TextView && it.text == "Wellen-Schalter anzeigen" }
        while (row != null && !row.isClickable) row = row.parent as? View
        val info = row!!.createAccessibilityNodeInfo()
        assertEquals("android.widget.Switch", info.className)
        assertTrue(info.isCheckable); assertTrue(info.isChecked)
        row.performClick()
        assertFalse(row.createAccessibilityNodeInfo().isChecked)
    }

    @Test fun rotationKeepsScreenAndSettings() {
        val c = Robolectric.buildActivity(MainActivity::class.java).setup()
        c.get().show(MainActivity.Screen.SETTINGS)
        clickText(c.get(), "Mischungen anzeigen")
        c.recreate()
        assertEquals(MainActivity.Screen.SETTINGS, c.get().screen)
        assertFalse(Display(RuntimeEnvironment.getApplication()).showMixes)
    }

    @Test fun presetFromSettingsLoadsAndReturns() {
        val a = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        a.show(MainActivity.Screen.SETTINGS)
        clickText(a, "Einschlafen")
        assertEquals(MainActivity.Screen.MAIN, a.screen)
        assertTrue(Mixer.matches(Mixer.preset("sleep")))
    }
}
