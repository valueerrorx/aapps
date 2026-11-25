package solutions.xapient.thinkr

import android.content.res.Configuration
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import solutions.xapient.thinkr.domain.ink.InkColor
import java.util.Locale

// Farbwoerter fuer Tinte: je Sprache vollstaendig, verschieden und nicht nur die englischen
@RunWith(RobolectricTestRunner::class)
class InkWordsTest {
    private val ids = mapOf(
        InkColor.BLUE to R.string.ink_blue,
        InkColor.ORANGE to R.string.ink_orange,
        InkColor.YELLOW to R.string.ink_yellow,
        InkColor.PURPLE to R.string.ink_purple,
        InkColor.RED to R.string.ink_red,
        InkColor.GREEN to R.string.ink_green,
    )

    private fun words(lang: String): List<String> {
        val base = RuntimeEnvironment.getApplication()
        val conf = Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(lang)) }
        val ctx = base.createConfigurationContext(conf)
        return InkColor.entries.map { ctx.getString(ids.getValue(it)) }
    }

    @Test fun everyColourHasAWordInEveryLanguage() {
        assertEquals(InkColor.entries.toSet(), ids.keys)
        for (lang in listOf("en", "de")) {
            val w = words(lang)
            assertTrue(w.all { it.isNotBlank() })
            assertEquals(lang, w.size, w.map { it.lowercase() }.toSet().size)
        }
        assertEquals("Blau", words("de")[0])
        assertEquals("Blue", words("en")[0])
    }
}
