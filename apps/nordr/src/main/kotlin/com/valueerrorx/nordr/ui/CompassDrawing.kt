package com.valueerrorx.nordr.ui

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import com.valueerrorx.aapps.core.ui.AappsColors
import com.valueerrorx.aapps.core.ui.drawGlow
import com.valueerrorx.nordr.domain.Angles
import com.valueerrorx.nordr.domain.CompassPhysics
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

val Warn = Color(0xFFF2B84E)

// Punkt auf dem Kreis: Grad im Uhrzeigersinn ab oben
private fun polar(center: Offset, deg: Float, radius: Float): Offset {
    val a = Math.toRadians(deg.toDouble())
    return Offset(center.x + (sin(a) * radius).toFloat(), center.y - (cos(a) * radius).toFloat())
}

private fun triangle(a: Offset, b: Offset, c: Offset) = Path().apply {
    moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close()
}

// Was sich pro Frame aendert
class DialLook(
    val course: Float?, // im gewaehlten Nordbezug
    val level: Float, // 0..1 auf Kurs
    val warn: Float, // 0..1 schlechte Genauigkeit
    val shine: Float, // -1 = aus, sonst 0..1
)

private class Tick(val start: Offset, val end: Offset, val color: Color, val width: Float)

private class Label(val deg: Int, val layout: TextLayoutResult)

// Alles, was nur von Groesse, Farben und Schrift abhaengt: einmal pro Groesse anlegen
class DialParts(scope: CacheDrawScope, letters: List<String>, val c: AappsColors, measurer: TextMeasurer, baseStyle: TextStyle) {
    val dp = scope.density
    val center = Offset(scope.size.width / 2f, scope.size.height / 2f)
    val r = (min(scope.size.width, scope.size.height) / 2f - 24f * dp).coerceAtMost(232f * dp)
    val visible = r > 40f * dp

    val caseBrush = Brush.radialGradient(
        0f to c.surfaceHigh,
        0.7f to c.surface,
        1f to c.bg,
        center = center - Offset(r * 0.2f, r * 0.25f),
        radius = r * 1.3f,
    )

    // Skala: 2er, 10er, 30er
    private val outer = r * 0.94f
    private val ticks = (0 until 360 step 2).map { d ->
        val major = d % 30 == 0
        val mid = d % 10 == 0
        val len = when {
            major -> r * 0.09f
            mid -> r * 0.06f
            else -> r * 0.03f
        }
        val color = when {
            d == 0 -> c.accent
            major -> c.fg.copy(alpha = 0.85f)
            mid -> c.fgMuted.copy(alpha = 0.85f)
            else -> c.fgFaint.copy(alpha = 0.8f)
        }
        Tick(polar(center, d.toFloat(), outer), polar(center, d.toFloat(), outer - len), color, (if (major) 2f else if (mid) 1.4f else 1f) * dp)
    }

    // Zahlen und Hauptrichtungen
    val labelRadius = r * 0.735f
    private val labels: List<Label> = if (!visible) emptyList() else with(scope) {
        val numberStyle = baseStyle.copy(fontSize = (r * 0.066f).toSp(), color = c.fgMuted)
        val letterStyle = baseStyle.copy(fontSize = (r * 0.13f).toSp(), fontWeight = FontWeight.Bold, color = c.fg)
        (0 until 360 step 30).map { d ->
            val cardinal = d % 90 == 0
            val style = when {
                d == 0 -> letterStyle.copy(color = c.accent)
                cardinal -> letterStyle
                else -> numberStyle
            }
            Label(d, measurer.measure(if (cardinal) letters[d / 90] else d.toString(), style))
        }
    }

    // Windrose: (Pfad, Farbe), Nord zuletzt
    private val rose: List<Pair<Path, Color>> = buildList {
        fun point(deg: Float, length: Float, halfWidth: Float, light: Color, dark: Color) {
            val tip = polar(center, deg, length)
            add(triangle(center, tip, polar(center, deg - 90f, halfWidth)) to light)
            add(triangle(center, tip, polar(center, deg + 90f, halfWidth)) to dark)
        }
        for (d in listOf(45f, 135f, 225f, 315f)) point(d, r * 0.3f, r * 0.045f, c.fg.copy(alpha = 0.13f), c.fg.copy(alpha = 0.05f))
        for (d in listOf(90f, 270f)) point(d, r * 0.46f, r * 0.06f, c.fg.copy(alpha = 0.2f), c.fg.copy(alpha = 0.08f))
        point(180f, r * 0.56f, r * 0.075f, c.fgMuted.copy(alpha = 0.75f), c.fgFaint.copy(alpha = 0.75f))
    }
    private val north = listOf(
        triangle(center, polar(center, 0f, r * 0.56f), polar(center, -90f, r * 0.075f)) to lerp(c.accent, Color.White, 0.55f),
        triangle(center, polar(center, 0f, r * 0.56f), polar(center, 90f, r * 0.075f)) to c.accent,
    )

    // Kursmarke bei 0°, wird gedreht
    val courseMark = triangle(polar(center, 0f, r * 0.945f), polar(center, -2.6f, r * 0.995f), polar(center, 2.6f, r * 0.995f))
    val courseTip = polar(center, 0f, r * 0.945f)

    // Steuerstrich oben
    val tipY = center.y - r - 4f * dp
    val indexMark = triangle(Offset(center.x, tipY), Offset(center.x - 7f * dp, tipY - 12f * dp), Offset(center.x + 7f * dp, tipY - 12f * dp))

    // Glas-Lichtfleck in Ruhelage; wandert per translate
    val spotBase = center + Offset(-r * 0.3f, -r * 0.34f)
    val spotBrush = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.075f),
        1f to Color.Transparent,
        center = spotBase,
        radius = r * 0.8f,
    )

    fun DrawScope.drawScale() {
        for (t in ticks) drawLine(t.color, t.start, t.end, strokeWidth = t.width, cap = StrokeCap.Round)
    }

    fun DrawScope.drawLabels(level: Float) {
        for (l in labels) {
            if (l.deg == 0) drawGlow(c.accent, Offset(center.x, center.y - labelRadius), r * 0.16f, 0.25f + 0.2f * level)
            rotate(l.deg.toFloat(), center) {
                drawText(
                    l.layout,
                    topLeft = Offset(center.x - l.layout.size.width / 2f, center.y - labelRadius - l.layout.size.height / 2f),
                )
            }
        }
    }

    // Windrose mit Facetten: Nord eisblau, Sued grau, Ost/West zart, Diagonalen kurz
    fun DrawScope.drawRose(level: Float) {
        drawCircle(c.fg.copy(alpha = 0.06f), r * 0.6f, center, style = Stroke(1f * dp))
        for ((path, color) in rose) drawPath(path, color)
        drawGlow(c.accent, polar(center, 0f, r * 0.3f), r * 0.42f, 0.16f + 0.14f * level)
        for ((path, color) in north) drawPath(path, color)
    }
}

// Kompassrose: Lünette fest, Skala und Windrose drehen sich mit Feder, Glas darueber
fun DrawScope.drawCompass(parts: DialParts, p: CompassPhysics, look: DialLook) = with(parts) {
    if (!visible) return@with
    val dial = p.dial.value
    val level = look.level

    // Licht und Schatten
    drawGlow(c.accent, center, r * 1.4f, 0.07f + 0.17f * level)
    for (i in 1..4) {
        drawCircle(Color.Black.copy(alpha = 0.11f), r + i * 1.5f * dp, center + Offset(0f, i * 3.5f * dp))
    }

    // Gehaeuse und dunkles Skalenband
    drawCircle(caseBrush, r, center)
    drawCircle(Color.Black.copy(alpha = 0.22f), r * 0.885f, center, style = Stroke(r * 0.15f))
    drawCircle(c.fg.copy(alpha = 0.07f), r * 0.81f, center, style = Stroke(1f * dp))

    rotate(-dial, center) {
        drawScale()
        drawLabels(level)
        drawRose(level)

        // Kursmarke am Rand
        look.course?.let { course ->
            rotate(course, center) {
                drawGlow(c.accent, courseTip, r * 0.09f, 0.35f + 0.25f * level)
                drawPath(courseMark, c.accent)
            }
        }
    }

    // Abweichungsbogen vom Steuerstrich zur Kursmarke
    look.course?.let { course ->
        val sweep = Angles.delta(dial, course)
        val rr = r * 0.97f
        drawArc(
            c.accent.copy(alpha = 0.55f * (1f - level) + 0.2f),
            -90f, sweep, false,
            center - Offset(rr, rr), Size(rr * 2f, rr * 2f),
            style = Stroke(3f * dp, cap = StrokeCap.Round),
        )
    }

    // Steuerstrich: fest oben, liest die Richtung ab
    val indexColor = lerp(c.fg, c.accent, level)
    drawLine(
        indexColor, Offset(center.x, center.y - r * 0.995f), Offset(center.x, center.y - r * 0.83f),
        strokeWidth = 2.4f * dp, cap = StrokeCap.Round,
    )
    drawGlow(indexColor, Offset(center.x, tipY - 6f * dp), 22f * dp, 0.18f + 0.2f * level)
    drawPath(indexMark, indexColor)

    // Achse
    drawCircle(c.bg, r * 0.05f, center)
    drawCircle(lerp(c.accent, Color.White, 0.3f), r * 0.05f, center, style = Stroke(1.5f * dp))
    drawCircle(Color.White.copy(alpha = 0.55f), r * 0.014f, center - Offset(r * 0.015f, r * 0.015f))

    // Glas: Lichtfleck und Reflex wandern mit der Neigung
    val gx = p.glintX.value
    val gy = p.glintY.value
    val shift = Offset(-gx * r * 0.25f, gy * r * 0.25f)
    translate(shift.x, shift.y) {
        drawCircle(spotBrush, r - 2f * dp, center - shift)
    }
    val rr = r - 9f * dp
    drawArc(
        Color.White.copy(alpha = 0.13f), 198f + gx * 18f, 52f, false,
        center - Offset(rr, rr), Size(rr * 2f, rr * 2f),
        style = Stroke(3f * dp, cap = StrokeCap.Round),
    )

    // Rand: Akzent auf Kurs, Bernstein bei schlechter Genauigkeit
    val rim = lerp(lerp(c.line, c.accent, level * 0.9f), Warn, look.warn * 0.85f)
    drawCircle(rim, r, center, style = Stroke(1.5f * dp))

    // Glanz-Ring beim Einrasten auf Kurs
    if (look.shine in 0f..1f) {
        val s = look.shine
        drawCircle(c.accent.copy(alpha = 0.5f * (1f - s)), r * (0.2f + 0.8f * s), center, style = Stroke((3f - 2f * s) * dp))
    }
}
