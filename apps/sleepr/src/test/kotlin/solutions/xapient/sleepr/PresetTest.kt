package solutions.xapient.sleepr

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Mixer ist global: Zustand sichern und wiederherstellen
class PresetTest {
    private lateinit var levels: IntArray
    private lateinit var waves: BooleanArray
    private lateinit var waveSec: IntArray
    private var tone = 0

    @Before fun save() { levels = Mixer.levels.copyOf(); waves = Mixer.waves.copyOf(); waveSec = Mixer.waveSec.copyOf(); tone = Mixer.tone }
    @After fun restore() { levels.copyInto(Mixer.levels); waves.copyInto(Mixer.waves); waveSec.copyInto(Mixer.waveSec); Mixer.tone = tone }

    @Test fun appliedPresetMatchesOnlyItself() {
        for (p in Mixer.PRESETS) {
            Mixer.applyPreset(p)
            assertTrue(p.name, Mixer.matches(p))
            assertEquals(p.name, Mixer.PRESETS.filter { Mixer.matches(it) }.map { it.name }, listOf(p.name))
        }
    }

    @Test fun changedLevelNoLongerMatches() {
        val p = Mixer.PRESETS.first()
        Mixer.applyPreset(p)
        Mixer.levels[0] = 1
        assertFalse(Mixer.matches(p))
    }

    @Test fun changedWaveLengthNoLongerMatches() {
        val meer = Mixer.PRESETS.first { it.waves.isNotEmpty() }
        Mixer.applyPreset(meer)
        val i = meer.waves.keys.first()
        Mixer.waveSec[i] = Mixer.waveSec[i] + 1
        assertFalse(Mixer.matches(meer))
    }

    @Test fun describeListsLoudestFirstWithWaves() {
        assertEquals("Braun 70 ∿ · Rosa 35 ∿", Mixer.describe(Mixer.PRESETS.first { it.name == "Meer" }))
        assertEquals("Weiß 45", Mixer.describe(Mixer.PRESETS.first { it.name == "Klassisch weiß" }))
    }
}
