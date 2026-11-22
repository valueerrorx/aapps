package solutions.xapient.spinr

import solutions.xapient.spinr.domain.Wheel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

// Vorlagen in beiden Sprachen: gueltige, editierbare Raeder
@RunWith(RobolectricTestRunner::class)
class TemplatesTest {
    private fun groups(qualifiers: String): List<TemplateGroup> {
        RuntimeEnvironment.setQualifiers(qualifiers)
        return templateGroups(RuntimeEnvironment.getApplication().resources)
    }

    @Test
    fun everyTemplateIsAValidWheel() {
        val counts = listOf("en", "de").map { lang ->
            val all = groups("+$lang").flatMap { it.wheels }
            for (w in all) {
                assertTrue("$lang ${w.name}: ${w.labels.size}", w.labels.size in Wheel.MIN_ENTRIES..Wheel.MAX_ENTRIES)
                assertEquals("$lang ${w.name}", w.entries, w.labels)
                assertEquals("$lang ${w.name}", w.labels.size, w.labels.toSet().size)
                assertTrue(w.name.isNotBlank() && w.name == Wheel.clean(w.name))
                w.labels.forEach { assertEquals(Wheel.clean(it), it) }
            }
            assertEquals(all.size, all.map { it.name }.toSet().size)
            all.map { it.labels.size }
        }
        // beide Sprachen gleich aufgebaut
        assertEquals(counts[0], counts[1])
        assertEquals(13, counts[0].size)
    }

    @Test
    fun groupsAndRange() {
        val g = groups("+de")
        assertEquals(listOf("Alltag", "Zahlen", "Richtungen", "Spiele"), g.map { it.title })
        assertEquals(listOf("Zahlen"), g.filter { it.withRange }.map { it.title })
        val all = g.flatMap { it.wheels }.associateBy { it.name }
        assertEquals((1..10).map { "$it" }, all.getValue("Zahlen 1–10").labels)
        assertEquals(26, all.getValue("Buchstaben A–Z").labels.size)
        assertEquals(listOf("N", "NO", "O", "SO", "S", "SW", "W", "NW"), all.getValue("Himmelsrichtungen").labels)
    }

    @Test
    fun firstStartKeepsTheFourClassics() {
        RuntimeEnvironment.setQualifiers("+de")
        val names = starterWheels(RuntimeEnvironment.getApplication().resources).map { it.name }
        assertEquals(listOf("Was essen wir?", "Ja oder nein?", "Würfel", "Wer zahlt?"), names)
    }
}
