package solutions.xapient.sleepr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.sleepr.COLORS
import solutions.xapient.sleepr.Mix
import solutions.xapient.sleepr.domain.Biquad
import solutions.xapient.sleepr.domain.FS
import solutions.xapient.sleepr.domain.FSpec
import solutions.xapient.sleepr.domain.FType
import solutions.xapient.sleepr.domain.GREEN_FILTERS
import solutions.xapient.sleepr.domain.GREY_FILTERS
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

private const val FN = 200
private val F = DoubleArray(FN) { 20.0 * 1000.0.pow(it / (FN - 1.0)) }

// Theoretisches Leistungsspektrum je Farbe, normiert (wie SpectrumView auf Android)
private val SPEC: Array<DoubleArray> by lazy {
    val fl = DoubleArray(2398) { 20.0 + it * 10 }
    val grey = GREY_FILTERS.map { Biquad(it) }
    val green = GREEN_FILTERS.map { Biquad(it) }
    fun raw(id: Int, f: Double): Double {
        var s = when (id) { 0, 3 -> 1.0; 2 -> 1 / (1 + (f / 150).pow(2)); else -> 1 / f }
        if (id == 3) for (b in grey) s *= b.magSq(f)
        if (id == 4) for (b in green) s *= b.magSq(f)
        return s
    }
    Array(5) { id -> val total = fl.sumOf { raw(id, it) }; DoubleArray(FN) { raw(id, F[it]) / total } }
}

/** Zeichnet das Spektrum der Mischung (dB ueber log-Frequenz); bei mehreren Farben die Anteile gestrichelt */
@Composable
fun Spectrum(levels: IntArray, tone: Int, emptyText: String, description: String, modifier: Modifier = Modifier) {
    val c = Aapps.colors
    val measurer = rememberTextMeasurer()
    val small = Aapps.type.body.copy(color = c.fgMuted, fontSize = 10.sp, lineHeight = 12.sp)
    Canvas(modifier.semantics { contentDescription = description }) {
        val d = density
        val w = size.width; val h = size.height
        val l = 30 * d; val r = 6 * d; val t = 8 * d; val b = 20 * d
        val pw = w - l - r; val ph = h - t - b
        fun x(f: Double) = (l + pw * ln(f / 20) / ln(1000.0)).toFloat()
        fun y(db: Double) = (t + ph * (-db / 60)).toFloat()

        for ((f, lab) in listOf(50.0 to "50", 100.0 to "100", 200.0 to "200", 500.0 to "500",
            1000.0 to "1k", 2000.0 to "2k", 5000.0 to "5k", 10000.0 to "10k")) {
            drawLine(c.line, Offset(x(f), t), Offset(x(f), t + ph), d)
            label(measurer, lab, small, x(f), h - 5 * d, Align.CENTER)
        }
        for (db in listOf(0.0, -20.0, -40.0, -60.0)) {
            drawLine(c.line, Offset(l, y(db)), Offset(l + pw, y(db)), d)
            label(measurer, if (db == 0.0) "0" else "−${(-db).toInt()}", small, l - 5 * d, y(db) + 3 * d, Align.RIGHT)
        }

        val lp = Biquad(FSpec(FType.LOWPASS, Mix.toneHzOf(tone).coerceAtMost(0.45 * FS)))
        val tr = DoubleArray(FN) { lp.magSq(F[it]) }
        val sum = DoubleArray(FN)
        val comps = (0..4).filter { levels[it] > 0 }.map { id ->
            val k = Mix.gain(levels[id]).pow(2)
            val a = DoubleArray(FN) { k * SPEC[id][it] * tr[it] }
            for (i in 0 until FN) sum[i] += a[i]
            id to a
        }
        val mx = sum.maxOrNull() ?: 0.0
        if (mx <= 0) {
            label(measurer, emptyText, small.copy(fontSize = 13.sp), l + pw / 2, t + ph / 2, Align.CENTER)
            return@Canvas
        }
        fun db(v: Double) = max(-60.0, 10 * log10(max(v, 1e-30) / mx))
        fun build(a: DoubleArray) = Path().apply {
            for (i in 0 until FN) {
                val px = x(F[i]); val py = y(db(a[i]))
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
        }
        val fill = build(sum).apply { lineTo(x(F[FN - 1]), t + ph); lineTo(x(F[0]), t + ph); close() }
        drawPath(fill, c.accent.copy(alpha = 0x2A / 255f))
        if (comps.size > 1) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(4 * d, 3 * d))
            for ((id, a) in comps) drawPath(build(a), COLORS[id].color, style = Stroke(1.5f * d, pathEffect = dash))
        }
        drawPath(build(sum), c.accent, style = Stroke(2.25f * d))
    }
}

private enum class Align { CENTER, RIGHT }

// Text mit Grundlinie bei baseY wie Canvas.drawText auf Android
private fun DrawScope.label(m: TextMeasurer, s: String, style: TextStyle, px: Float, baseY: Float, align: Align) {
    val r = m.measure(s, style)
    val left = if (align == Align.CENTER) px - r.size.width / 2f else px - r.size.width
    drawText(r, topLeft = Offset(left, baseY - r.firstBaseline))
}
