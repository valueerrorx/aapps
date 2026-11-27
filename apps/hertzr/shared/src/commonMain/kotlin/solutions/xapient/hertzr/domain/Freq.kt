package solutions.xapient.hertzr.domain

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// Notenname zerlegt: Buchstabe (Index 0..11 ab C), Oktave, Cent-Abweichung
data class Note(val pitchClass: Int, val octave: Int, val cents: Int)

// Frequenz: Bereich, Log-Skala, Schritte, Anzeige und Eingabe
object Freq {
    private val THOUSANDS = Regex("""\d{1,3}([.,]\d{3})+""")
    const val MIN = 1.0
    const val MAX = 22_000.0
    const val A4 = 440.0

    // Obergrenze je Abtastrate: knapp unter Nyquist
    fun maxFor(rate: Int): Double = minOf(MAX, floor(rate * 0.49))

    fun clamp(hz: Double, maxHz: Double = MAX): Double =
        if (hz.isNaN()) A4 else hz.coerceIn(MIN, maxHz)

    // Anzeige- und Speicher-Aufloesung: 0,1 Hz unter 100 Hz, sonst 1 Hz; was man sieht, klingt
    fun quantize(hz: Double): Double =
        if (hz < 99.95) (hz * 10).roundToLong() / 10.0 else hz.roundToLong().toDouble()

    // Feinschritt der Tasten
    fun step(hz: Double): Double = when {
        hz < 100.0 -> 0.1
        hz < 10_000.0 -> 1.0
        else -> 10.0
    }

    // einen Schritt auf dem Raster; an Bereichsgrenzen wechselt die Schrittweite passend
    fun nudge(hz: Double, dir: Int, maxHz: Double = MAX): Double {
        // abwaerts zaehlt die Grenze zum feineren Bereich (100 -> 99,9)
        val s = if (dir < 0 && (hz == 100.0 || hz == 10_000.0)) step(hz / 2) else step(hz)
        // naechster Rasterpunkt in Richtung dir, auch von krummen Werten aus
        val next = if (dir > 0) floor(hz / s + 1e-6) + 1 else ceil(hz / s - 1e-6) - 1
        return quantize(clamp(next * s, maxHz))
    }

    fun octave(hz: Double, up: Boolean, maxHz: Double = MAX): Double =
        quantize(clamp(if (up) hz * 2 else hz / 2, maxHz))

    // um Oktaven verschieben (Band ziehen, Halbton = 1/12)
    fun shift(hz: Double, octaves: Double, maxHz: Double = MAX): Double =
        quantize(clamp(hz * 2.0.pow(octaves), maxHz))

    // Band: eine Bildschirmbreite = 2 Oktaven; Finger nach links = hoeher
    const val TAPE_OCTAVES = 2.0

    fun tapeOctaves(dxPx: Float, widthPx: Float): Double = -dxPx / widthPx * TAPE_OCTAVES

    // Lage auf der Log-Skala 0..1 (1 Hz .. 22 kHz)
    fun toPos(hz: Double): Float = (ln(hz / MIN) / ln(MAX / MIN)).toFloat().coerceIn(0f, 1f)

    fun fromPos(pos: Float): Double = MIN * (MAX / MIN).pow(pos.toDouble().coerceIn(0.0, 1.0))

    // Halbton-Index fuer die Haptik beim Drehen
    fun semitone(hz: Double): Int = floor(12 * log2(hz / A4) + 0.5).toInt()

    // naechster Ton (A4 = 440 Hz), null unter C-1
    fun note(hz: Double): Note? {
        val midi = (69 + 12 * log2(hz / A4)).roundToInt()
        if (midi < 0) return null
        val ref = A4 * 2.0.pow((midi - 69) / 12.0)
        return Note((midi).mod(12), (midi).floorDiv(12) - 1, (1200 * log2(hz / ref)).roundToInt())
    }

    // Zahl wie angezeigt: eine Nachkommastelle unter 100 Hz, sonst ganz
    fun format(hz: Double, decimal: Char = '.'): String {
        val q = quantize(hz)
        if (q >= 99.95) return q.roundToLong().toString()
        val tenths = (q * 10).roundToLong()
        return "${tenths / 10}$decimal${abs(tenths % 10)}"
    }

    // kurz mit Einheit, verlustfrei: "55.5 Hz", "440 Hz", "1.5 kHz", "12.345 kHz"
    fun short(hz: Double, decimal: Char = '.'): String {
        val q = quantize(hz)
        if (q < 1_000) return "${format(q, decimal)} Hz"
        val whole = q.roundToLong()
        val frac = (whole % 1_000).toString().padStart(3, '0').trimEnd('0')
        return if (frac.isEmpty()) "${whole / 1_000} kHz" else "${whole / 1_000}$decimal$frac kHz"
    }

    // "440", "1,5k", "12 kHz", "2.5 khz", "1.500" = 1500 Hz (Tausenderpunkt); null bei Unsinn oder <= 0
    fun parse(text: String): Double? {
        var t = text.trim().lowercase().replace(" ", "")
        if (t.endsWith("hz")) t = t.dropLast(2)
        var mul = 1.0
        if (t.endsWith("k")) {
            mul = 1_000.0
            t = t.dropLast(1)
        } else if (THOUSANDS.matches(t)) {
            // drei Nachkommastellen gibt es im Hz-Raster nicht: Trennzeichen sind Tausender
            t = t.filter { it.isDigit() }
        }
        t = t.replace(',', '.')
        if (t.isEmpty() || t.count { it == '.' } > 1 || !t.all { it.isDigit() || it == '.' }) return null
        val v = t.toDoubleOrNull()?.times(mul) ?: return null
        return if (v > 0 && v.isFinite()) v else null
    }
}
