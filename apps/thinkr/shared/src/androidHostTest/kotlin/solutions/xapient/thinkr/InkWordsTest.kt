package solutions.xapient.thinkr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.thinkr.domain.ink.InkColor
import java.io.File

// Farbwoerter fuer Tinte: je Sprache vollstaendig, verschieden und nicht nur die englischen (direkt aus den Compose-Ressourcen)
class InkWordsTest {
    private val ids = mapOf(
        InkColor.BLUE to "ink_blue",
        InkColor.ORANGE to "ink_orange",
        InkColor.YELLOW to "ink_yellow",
        InkColor.PURPLE to "ink_purple",
        InkColor.RED to "ink_red",
        InkColor.GREEN to "ink_green",
    )

    private fun words(lang: String): List<String> {
        val dir = if (lang == "en") "values" else "values-$lang"
        val xml = File("src/commonMain/composeResources/$dir/strings.xml").readText()
        return InkColor.entries.map { c ->
            Regex("<string name=\"${ids.getValue(c)}\">([^<]*)</string>").find(xml)?.groupValues?.get(1) ?: ""
        }
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
