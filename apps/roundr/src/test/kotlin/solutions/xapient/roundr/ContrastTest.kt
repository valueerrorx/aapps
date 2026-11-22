package solutions.xapient.roundr

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.roundr.ui.Amber
import solutions.xapient.roundr.ui.EDGE_GLOW
import solutions.xapient.roundr.ui.HATCH_ALPHA
import solutions.xapient.roundr.ui.Ember
import solutions.xapient.roundr.ui.Ice
import solutions.xapient.roundr.ui.Ink
import solutions.xapient.roundr.ui.RoundrAccent
import solutions.xapient.roundr.ui.drained

// WCAG-Kontrast gemessen statt geschaetzt; Text >= 4,5:1
class ContrastTest {
    private val ui = AappsColors(accent = RoundrAccent)
    private val phases = listOf(Ember, Ice, Amber)

    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance() + 0.05
        val l2 = b.luminance() + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    private fun aa(fg: Color, bg: Color, what: String, min: Double = 4.5) {
        val r = ratio(fg, bg)
        assertTrue("$what: %.2f < $min".format(r), r >= min)
    }

    @Test fun inkOnPhaseColours() = phases.forEach { aa(Ink, it, "Tinte auf $it") }

    @Test fun phaseColourOnDrainedArea() = phases.forEach { aa(it, drained(it), "$it auf geleert") }

    // Schein ueber der Fuellkante hellt den geleerten Teil etwas auf
    @Test fun phaseColourOnGlowBand() = phases.forEach { aa(it, it.copy(alpha = EDGE_GLOW).compositeOver(drained(it)), "$it auf Schein") }

    // Angehalten: Schraffur aus Tinte ueber der Farbe
    @Test fun inkOnPausedHatch() = phases.forEach { aa(Ink, Ink.copy(alpha = HATCH_ALPHA).compositeOver(it), "Tinte auf Schraffur $it") }

    @Test fun mutedTextAndWarnings() {
        aa(ui.fgMuted, ui.surface, "fgMuted auf surface")
        aa(ui.fgMuted, ui.bg, "fgMuted auf bg")
        aa(Amber, ui.surface, "Warnung auf surface")
        aa(ui.accent, ui.bg, "Akzent auf bg")
        aa(ui.fg, ui.surfaceHigh, "fg auf gedrueckt")
    }

    // Arbeit und Pause auch in der Helligkeit verschieden, nicht nur im Farbton
    @Test fun workAndRestDifferInLightness() = assertTrue(ratio(Ember, Ice) > 1.3)
}
