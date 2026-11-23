package solutions.xapient.sleepr

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

// Englisch als Grundsprache, Deutsch als Übersetzung: Mixer und Einstellungen in beiden Sprachen
@RunWith(RobolectricTestRunner::class)
class LocaleTest {
    private lateinit var levels: IntArray
    private lateinit var waves: BooleanArray
    private var tone = 0
    private var timer = 0

    @Before fun save() { levels = Mixer.levels.copyOf(); waves = Mixer.waves.copyOf(); tone = Mixer.tone; timer = Mixer.timer }
    @After fun restore() { levels.copyInto(Mixer.levels); waves.copyInto(Mixer.waves); Mixer.tone = tone; Mixer.timer = timer }

    private fun all(v: View): List<View> = if (v is ViewGroup) listOf(v) + (0 until v.childCount).flatMap { all(v.getChildAt(it)) } else listOf(v)

    private fun texts(a: MainActivity) = all(a.findViewById<ViewGroup>(android.R.id.content).getChildAt(0))
        .filterIsInstance<TextView>().map { it.text.toString() }

    private fun descs(a: MainActivity) = all(a.findViewById<ViewGroup>(android.R.id.content).getChildAt(0))
        .mapNotNull { it.contentDescription?.toString() }

    private fun screens(): Pair<List<String>, List<String>> {
        val a = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val main = texts(a) + descs(a)
        a.show(MainActivity.Screen.SETTINGS)
        return main to texts(a) + descs(a)
    }

    private fun assertHas(list: List<String>, vararg s: String) = s.forEach { assertTrue("$it fehlt in $list", it in list) }

    @Test fun english() {
        Mixer.timer = 30
        val (main, settings) = screens()
        assertHas(main, "Ready", "START", "Play", "SPECTRUM OF THE MIX", "MIXES", "Ocean", "Classic white", "White", "Brown",
            "  0 dB/oct", "  −6 dB/oct", "Volume", "Treble", "open", "Off", "30 min", "Timer 30 minutes", "Turn on waves for Pink")
        assertTrue(main.any { it.startsWith("Brown 60 · Pink 20") && it.endsWith(" · Timer 30 min") })
        assertHas(settings, "Settings", "Show wave buttons", "Show mixes", "Ocean", "Sleep", "White · 0 dB/oct", "About")
        assertTrue(settings.none { it.contains("Okt") || it.contains("Mischung") })
    }

    @Test @Config(qualifiers = "de") fun german() {
        Mixer.timer = 30
        val (main, settings) = screens()
        assertHas(main, "Bereit", "START", "Abspielen", "SPEKTRUM DER MISCHUNG", "MISCHUNGEN", "Meer", "Klassisch weiß", "Weiß", "Braun",
            "  0 dB/Okt", "  −6 dB/Okt", "Lautstärke", "Höhen", "offen", "Aus", "30 min", "Timer 30 Minuten", "Wellen Rosa ein")
        assertTrue(main.any { it.startsWith("Braun 60 · Rosa 20") && it.endsWith(" · Timer 30 min") })
        assertHas(settings, "Einstellungen", "Wellen-Schalter anzeigen", "Mischungen anzeigen", "Meer", "Einschlafen", "Weiß · 0 dB/Okt", "Info")
    }

    @Test fun describeFollowsLocale() {
        val ctx = RuntimeEnvironment.getApplication()
        assertEquals("Brown 70 ∿ · Pink 35 ∿", Mixer.describe(ctx, Mixer.preset("ocean")))
        assertEquals("White 45", Mixer.describe(ctx, Mixer.preset("white")))
    }

    @Test @Config(qualifiers = "de") fun describeGerman() {
        val ctx = RuntimeEnvironment.getApplication()
        assertEquals("Braun 70 ∿ · Rosa 35 ∿", Mixer.describe(ctx, Mixer.preset("ocean")))
        assertEquals("Weiß 45", Mixer.describe(ctx, Mixer.preset("white")))
    }

    @Test fun treblePerLocale() {
        val ctx = RuntimeEnvironment.getApplication()
        assertEquals("1.5 kHz", Mixer.fmtHz(ctx, 1500.0))
        assertEquals("400 Hz", Mixer.fmtHz(ctx, 400.0))
    }

    @Test @Config(qualifiers = "de") fun trebleGerman() {
        assertEquals("1,5 kHz", Mixer.fmtHz(RuntimeEnvironment.getApplication(), 1500.0))
    }

    // Auswahl hängt an Werten und IDs, nicht am angezeigten Namen: gilt über Sprachwechsel hinweg
    @Test fun presetMatchIsLanguageIndependent() {
        Mixer.applyPreset(Mixer.preset("nature"))
        RuntimeEnvironment.setQualifiers("de")
        assertTrue(Mixer.matches(Mixer.preset("nature")))
    }
}
