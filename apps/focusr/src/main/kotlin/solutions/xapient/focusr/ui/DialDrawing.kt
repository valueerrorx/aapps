package solutions.xapient.focusr.ui

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.focusr.domain.Scale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Punkt auf dem Kreis: Anteil 0..1 im Uhrzeigersinn ab 12 Uhr
private fun polar(center: Offset, frac: Float, radius: Float): Offset {
    val a = frac * 2.0 * Math.PI
    return Offset(center.x + (sin(a) * radius).toFloat(), center.y - (cos(a) * radius).toFloat())
}

// Pausiert: Sektor gedaempft, aber gegen die Mulde noch >= 3:1 (ContrastTest)
const val DIM_ALPHA = 0.75f

// Was sich pro Frame aendert
class DialLook(
    val fill: Float, // 0..1 Restzeit
    val ghost: Float, // 0..1 eingestellte Zeit, < 0 = aus
    val dim: Boolean, // pausiert
    val pulse: Float, // 0..1 letzte Minute
    val done: Float, // 0..1 Lichtring nach dem Ende
    val dragging: Boolean,
)

private class Tick(val start: Offset, val end: Offset, val color: Color, val width: Float)

private class Label(val pos: Offset, val layout: TextLayoutResult)

// Alles, was nur von Groesse, Skala und Farben abhaengt: einmal pro Groesse anlegen
class DialParts(scope: CacheDrawScope, scale: Scale, val c: AappsColors, measurer: TextMeasurer, labelStyle: TextStyle) {
    val dp = scope.density
    val center = Offset(scope.size.width / 2f, scope.size.height / 2f)
    val r = min(scope.size.width, scope.size.height) / 2f - 6f * dp
    val visible = r > 40f * dp
    val face = r * 0.74f
    private val faceSize = Size(face * 2f, face * 2f)
    private val faceTopLeft = center - Offset(face, face)

    val caseBrush = Brush.radialGradient(
        0f to c.surfaceHigh,
        0.75f to c.surface,
        1f to lerp(c.surface, c.bg, 0.5f),
        center = center - Offset(r * 0.25f, r * 0.3f),
        radius = r * 1.35f,
    )

    // Licht: innen heller, zum Rand satter
    private val sectorBrush = Brush.radialGradient(
        0f to AccentLight,
        0.55f to FocusrAccent,
        1f to AccentDeep,
        center = center,
        radius = face,
    )

    private val wellBrush = Brush.radialGradient(
        0f to lerp(Well, c.bg, 0.6f),
        1f to Well,
        center = center,
        radius = face,
    )

    private val sheen = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.06f),
        1f to Color.Transparent,
        center = center - Offset(r * 0.35f, r * 0.45f),
        radius = r * 0.95f,
    )

    private val knobR = r * 0.1f
    private val knobBrush = Brush.radialGradient(
        0f to lerp(c.surfaceHigh, c.fg, 0.12f),
        1f to c.surface,
        center = center - Offset(knobR * 0.35f, knobR * 0.45f),
        radius = knobR * 1.4f,
    )

    // 60 Rasten, beschriftete laenger und heller
    private val ticks: List<Tick> = run {
        val labelled = scale.labels.map { it.first }.toSet()
        (0 until Scale.STEPS).map { i ->
            val f = i / Scale.STEPS.toFloat()
            val major = i in labelled
            val inner = if (major) r * 0.775f else r * 0.79f
            val outer = if (major) r * 0.835f else r * 0.815f
            Tick(
                polar(center, f, inner), polar(center, f, outer),
                if (major) c.fg.copy(alpha = 0.75f) else c.fgMuted.copy(alpha = 0.55f),
                (if (major) 2f else 1.2f) * dp,
            )
        }
    }

    // Zahlen aufrecht wie bei einer Uhr
    private val labels: List<Label> = if (!visible) emptyList() else with(scope) {
        val style = labelStyle.copy(fontSize = (r * 0.074f).toSp(), color = dialLabel(c))
        scale.labels.map { (i, minutes) ->
            val layout = measurer.measure(minutes.toString(), style)
            val p = polar(center, i / Scale.STEPS.toFloat(), r * 0.912f)
            Label(Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f), layout)
        }
    }

    fun DrawScope.drawCase() {
        for (i in 1..4) drawCircle(Color.Black.copy(alpha = 0.1f), r + i * 1.5f * dp, center + Offset(0f, i * 3f * dp))
        drawCircle(caseBrush, r, center)
        drawCircle(c.line, r - 0.5f * dp, center, style = Stroke(1f * dp))
        for (t in ticks) drawLine(t.color, t.start, t.end, strokeWidth = t.width, cap = StrokeCap.Round)
        for (l in labels) drawText(l.layout, topLeft = l.pos)
        // Mulde
        drawCircle(wellBrush, face, center)
        drawCircle(Color.Black.copy(alpha = 0.35f), face + 1.5f * dp, center, style = Stroke(3f * dp))
        drawCircle(c.fg.copy(alpha = 0.06f), face + 3.5f * dp, center, style = Stroke(1f * dp))
    }

    fun DrawScope.drawGhost(ghost: Float) {
        if (ghost <= 0f) return
        drawArc(FocusrAccent.copy(alpha = 0.1f), -90f, 360f * ghost, true, faceTopLeft, faceSize)
        val end = polar(center, ghost, face)
        drawLine(FocusrAccent.copy(alpha = 0.3f), center, end, strokeWidth = 1.2f * dp, cap = StrokeCap.Round)
    }

    fun DrawScope.drawSector(fill: Float, alpha: Float) {
        if (fill <= 0f) return
        drawArc(sectorBrush, -90f, 360f * fill, true, faceTopLeft, faceSize, alpha = alpha)
        // heller Lichtsaum am Rand und an der Kante
        val rim = face - 1.2f * dp
        drawArc(
            AccentLight.copy(alpha = 0.9f * alpha), -90f, 360f * fill, false,
            center - Offset(rim, rim), Size(rim * 2f, rim * 2f), style = Stroke(2.4f * dp, cap = StrokeCap.Butt),
        )
        drawLine(lerp(AccentLight, Color.White, 0.35f).copy(alpha = alpha), center, polar(center, fill, face), 2f * dp, StrokeCap.Round)
    }

    // letzte Minute: Sektor atmet heller
    fun DrawScope.drawPulse(fill: Float, pulse: Float) {
        if (fill <= 0f) return
        drawArc(Color.White.copy(alpha = 0.22f * pulse), -90f, 360f * fill, true, faceTopLeft, faceSize)
    }

    fun DrawScope.drawHandle(fill: Float, dragging: Boolean, alpha: Float) {
        val p = polar(center, fill, face)
        val size = if (dragging) 11f * dp else 8f * dp
        drawGlow(FocusrAccent, p, size * 3.2f, (if (dragging) 0.55f else 0.35f) * alpha)
        drawCircle(lerp(AccentLight, Color.White, 0.55f).copy(alpha = alpha), size, p)
        drawCircle(AccentDeep.copy(alpha = 0.6f * alpha), size, p, style = Stroke(1.2f * dp))
    }

    fun DrawScope.drawKnob() {
        drawCircle(Color.Black.copy(alpha = 0.35f), knobR + 2f * dp, center + Offset(0f, 2f * dp))
        drawCircle(knobBrush, knobR, center)
        drawCircle(c.line, knobR, center, style = Stroke(1f * dp))
        val k = knobR * 0.72f
        drawArc(
            Color.White.copy(alpha = 0.16f), 200f, 140f, false,
            center - Offset(k, k), Size(k * 2f, k * 2f), style = Stroke(1.4f * dp, cap = StrokeCap.Round),
        )
    }

    fun DrawScope.drawSheen() = drawCircle(sheen, r, center)
}

// Ganze Scheibe: Lichthof, Gehaeuse, Schatten der eingestellten Zeit, Sektor, Griff, Knopf
fun DrawScope.drawDial(parts: DialParts, look: DialLook) = with(parts) {
    if (!visible) return@with
    val alpha = if (look.dim) DIM_ALPHA else 1f
    // Lichthof nimmt mit der Restzeit ab, pulsiert in der letzten Minute; pausiert fast aus
    drawGlow(FocusrAccent, center, r * 1.3f, (0.05f + 0.2f * look.fill) * (if (look.dim) 0.35f else 1f) * (1f + 0.8f * look.pulse))
    if (look.done > 0f) drawGlow(FocusrAccent, center, r * 1.35f, 0.32f * look.done)
    drawCase()
    drawGhost(look.ghost)
    drawSector(look.fill, alpha)
    if (look.pulse > 0f) drawPulse(look.fill, look.pulse)
    if (look.done > 0f) {
        drawCircle(FocusrAccent.copy(alpha = look.done), face - 1f * dp, center, style = Stroke(2.5f * dp))
        drawGlow(FocusrAccent, center, face * 1.05f, 0.2f * look.done)
    }
    drawHandle(look.fill, look.dragging, alpha)
    drawKnob()
    drawSheen()
}
