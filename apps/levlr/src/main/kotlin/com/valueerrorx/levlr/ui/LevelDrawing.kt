package com.valueerrorx.levlr.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import com.valueerrorx.aapps.core.ui.AappsColors
import com.valueerrorx.aapps.core.ui.drawGlow
import com.valueerrorx.levlr.domain.LevelPhysics
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private val EDGE_TICKS = floatArrayOf(0.5f, 1f, 2f, 3f, 5f, 10f)
private val EDGE_LABELS = setOf(2f, 5f, 10f)
private val FLAT_RINGS = floatArrayOf(1f, 2f, 5f)
private val FLAT_LABELS = setOf(2f, 5f)

// Roehrenlibelle mit Fluessigkeits-"Meer", das waagrecht zur Welt bleibt
fun DrawScope.drawEdgeLevel(
    p: LevelPhysics,
    hero: Rect,
    quadrant: Int,
    level: Float,
    shine: Float,
    c: AappsColors,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    val dp = density
    val center = hero.center
    val odd = quadrant % 2 == 1
    val span = if (odd) hero.height else hero.width
    val length = min(span - 40f * dp, 560f * dp)
    val thick = (length * 0.24f).coerceIn(64f * dp, 112f * dp)
    val diag = hypot(size.width, size.height)

    // Meer: Oberflaeche durch die Roehrenmitte, immer waagrecht zur Welt
    rotate(p.surface.value - quadrant * 90f, center) {
        val sea = Path()
        val amp = p.wave * 22f * dp
        val steps = 64
        for (i in 0..steps) {
            val x = center.x - diag + 2f * diag * i / steps
            val u = (x - center.x) / (size.width * 0.75f)
            val y = center.y + amp * sin(u * PI).toFloat()
            if (i == 0) sea.moveTo(x, y) else sea.lineTo(x, y)
        }
        val surfaceLine = Path().apply { addPath(sea) }
        sea.lineTo(center.x + diag, center.y + diag)
        sea.lineTo(center.x - diag, center.y + diag)
        sea.close()
        // heller Meniskus direkt unter der Oberflaeche, darunter tiefe, dunkle Fluessigkeit
        drawPath(
            sea,
            Brush.verticalGradient(
                0f to c.accent.copy(alpha = 0.26f + 0.08f * level),
                0.06f to c.accent.copy(alpha = 0.13f),
                0.4f to c.accent.copy(alpha = 0.05f),
                1f to c.accent.copy(alpha = 0.02f),
                startY = center.y,
                endY = center.y + size.height * 0.7f,
            ),
        )
        drawPath(surfaceLine, c.accent.copy(alpha = 0.06f), style = Stroke(24f * dp))
        drawPath(surfaceLine, c.accent.copy(alpha = 0.16f), style = Stroke(8f * dp))
        drawPath(surfaceLine, lerp(c.accent, Color.White, 0.3f).copy(alpha = 0.75f + 0.25f * level), style = Stroke(1.8f * dp))
    }

    withTransform({ rotate(-quadrant * 90f, center) }) {
        val tube = Rect(center.x - length / 2f, center.y - thick / 2f, center.x + length / 2f, center.y + thick / 2f)
        val radius = CornerRadius(thick / 2f)
        val inset = 4f * dp
        val inner = tube.deflate(inset)
        val innerRadius = CornerRadius(inner.height / 2f)

        // Halo beim Einrasten
        drawGlow(c.accent, center, length * 0.62f, 0.05f + 0.20f * level)

        // weicher Schatten unter dem Glas
        for (i in 1..4) {
            val o = i * 3f * dp
            drawRoundRect(
                Color.Black.copy(alpha = 0.10f),
                Offset(tube.left - o / 2f, tube.top + o),
                Size(tube.width + o, tube.height),
                CornerRadius(radius.x + o),
            )
        }

        drawRoundRect(c.bg, tube.topLeft, tube.size, radius)
        // Fluessigkeit in der Roehre, unten dichter
        drawRoundRect(
            Brush.verticalGradient(
                0f to c.accent.copy(alpha = 0.20f),
                1f to c.accent.copy(alpha = 0.42f),
                startY = inner.top,
                endY = inner.bottom,
            ),
            inner.topLeft, inner.size, innerRadius,
        )
        // dunkle Enden geben Rundung
        drawRoundRect(
            Brush.horizontalGradient(
                0f to Color.Black.copy(alpha = 0.45f),
                0.12f to Color.Transparent,
                0.88f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.45f),
                startX = inner.left,
                endX = inner.right,
            ),
            inner.topLeft, inner.size, innerRadius,
        )

        // Blase
        val bubbleLen0 = thick * 1.55f
        val travel = (inner.width - bubbleLen0) / 2f - 2f * dp
        val stretch = p.edgeStretch
        val bLen = bubbleLen0 * (1f + stretch)
        val bH = (inner.height * 0.62f) * (1f - stretch * 0.35f)
        val bx = center.x + p.bubble.value * travel
        val bTop = inner.top + 3f * dp
        val bubble = Rect(bx - bLen / 2f, bTop, bx + bLen / 2f, bTop + bH)
        val clip = Path().apply { addRoundRect(RoundRect(inner, innerRadius)) }
        clipPath(clip) {
            drawGlow(c.accent, bubble.center, bLen * 0.75f, 0.30f + 0.25f * level)
            val bubbleR = CornerRadius(bH / 2f)
            drawRoundRect(
                Brush.verticalGradient(
                    0f to lerp(c.accent, Color.White, 0.55f).copy(alpha = 0.95f),
                    0.55f to lerp(c.accent, Color.White, 0.15f).copy(alpha = 0.85f),
                    1f to c.accent.copy(alpha = 0.70f),
                    startY = bubble.top,
                    endY = bubble.bottom,
                ),
                bubble.topLeft, bubble.size, bubbleR,
            )
            drawRoundRect(
                lerp(c.accent, Color.White, 0.6f),
                bubble.topLeft, bubble.size, bubbleR,
                style = Stroke(1.2f * dp),
            )
            // Glanzpunkt
            val hl = Rect(
                bubble.left + bLen * 0.18f, bubble.top + bH * 0.16f,
                bubble.left + bLen * 0.52f, bubble.top + bH * 0.34f,
            )
            drawRoundRect(Color.White.copy(alpha = 0.75f), hl.topLeft, hl.size, CornerRadius(hl.height / 2f))

            // Glanz-Wisch beim Einrasten
            if (shine in 0f..1f) {
                val band = thick * 1.6f
                val x = inner.left - band + (inner.width + 2 * band) * shine
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.32f * (1f - shine * 0.5f)),
                        1f to Color.Transparent,
                        startX = x - band / 2f,
                        endX = x + band / 2f,
                    ),
                    Offset(x - band / 2f, inner.top), Size(band, inner.height),
                )
            }
        }

        // Markierungen fuer die Mitte
        val markColor = lerp(c.fgMuted, c.accent, level)
        val gap = bubbleLen0 / 2f + 4f * dp
        for (s in floatArrayOf(-1f, 1f)) {
            drawLine(
                markColor, Offset(center.x + s * gap, inner.top + 3f * dp), Offset(center.x + s * gap, inner.bottom - 3f * dp),
                strokeWidth = 2f * dp, cap = StrokeCap.Round,
            )
        }

        // Glas: Kante und Lichtreflex
        drawLine(
            Color.White.copy(alpha = 0.16f),
            Offset(tube.left + thick * 0.5f, tube.top + thick * 0.16f),
            Offset(tube.right - thick * 0.5f, tube.top + thick * 0.16f),
            strokeWidth = 2.2f * dp, cap = StrokeCap.Round,
        )
        drawLine(
            Color.White.copy(alpha = 0.05f),
            Offset(tube.left + thick * 0.6f, tube.bottom - thick * 0.12f),
            Offset(tube.right - thick * 0.6f, tube.bottom - thick * 0.12f),
            strokeWidth = 1.5f * dp, cap = StrokeCap.Round,
        )
        drawRoundRect(
            lerp(c.line, c.accent, level * 0.9f).copy(alpha = 0.9f),
            tube.topLeft, tube.size, radius, style = Stroke(1.5f * dp),
        )

        // Skala unter der Roehre: Position der Blasenmitte bei a Grad
        val scaleTop = tube.bottom + 14f * dp
        for (a in EDGE_TICKS) {
            val major = a in EDGE_LABELS || a == 1f
            for (s in floatArrayOf(-1f, 1f)) {
                val x = center.x + s * LevelPhysics.soft(a, LevelPhysics.EDGE_SCALE) * travel
                drawLine(
                    c.fgFaint.copy(alpha = if (major) 0.9f else 0.5f),
                    Offset(x, scaleTop), Offset(x, scaleTop + if (major) 10f * dp else 6f * dp),
                    strokeWidth = 1.2f * dp, cap = StrokeCap.Round,
                )
                if (a in EDGE_LABELS) {
                    val label = measurer.measure(a.toInt().toString(), labelStyle)
                    drawText(label, topLeft = Offset(x - label.size.width / 2f, scaleTop + 14f * dp))
                }
            }
        }
        drawLine(markColor, Offset(center.x, scaleTop), Offset(center.x, scaleTop + 14f * dp), strokeWidth = 2f * dp, cap = StrokeCap.Round)
    }
}

// Dosenlibelle: Blase im Kreis, Ringe fuer 1°, 2°, 5°
fun DrawScope.drawFlatLevel(
    p: LevelPhysics,
    hero: Rect,
    level: Float,
    shine: Float,
    c: AappsColors,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    val dp = density
    val center = hero.center
    val r = (min(hero.width, hero.height) / 2f - 16f * dp).coerceAtMost(210f * dp)
    val rb = r * 0.17f
    val travel = r - rb - 8f * dp
    val pos = Offset(center.x + p.bubbleX.value * travel, center.y + p.bubbleY.value * travel)

    drawGlow(c.accent, center + (pos - center) * 0.6f, r * 1.7f, 0.08f + 0.16f * level)
    for (i in 1..4) {
        drawCircle(Color.Black.copy(alpha = 0.10f), r + i * 1.5f * dp, center + Offset(0f, i * 3f * dp))
    }
    drawCircle(c.bg, r, center)
    drawCircle(
        Brush.radialGradient(
            0f to c.accent.copy(alpha = 0.04f),
            0.72f to c.accent.copy(alpha = 0.08f),
            0.93f to c.accent.copy(alpha = 0.20f),
            1f to c.accent.copy(alpha = 0.34f),
            center = center,
            radius = r - 4f * dp,
        ),
        r - 4f * dp, center,
    )
    // Lichtfleck oben links: Glaswoelbung
    drawCircle(
        Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.06f),
            1f to Color.Transparent,
            center = center - Offset(r * 0.35f, r * 0.4f),
            radius = r * 0.7f,
        ),
        r - 4f * dp, center,
    )

    // Fadenkreuz und Ringe
    val faint = c.fg.copy(alpha = 0.10f)
    drawLine(faint, Offset(center.x - r * 0.92f, center.y), Offset(center.x + r * 0.92f, center.y), strokeWidth = 1f * dp)
    drawLine(faint, Offset(center.x, center.y - r * 0.92f), Offset(center.x, center.y + r * 0.92f), strokeWidth = 1f * dp)
    for (a in FLAT_RINGS) {
        val rr = LevelPhysics.soft(a, LevelPhysics.FLAT_SCALE) * travel
        drawCircle(c.fg.copy(alpha = 0.13f), rr, center, style = Stroke(1f * dp))
        if (a !in FLAT_LABELS) continue
        val label = measurer.measure("${a.toInt()}°", labelStyle)
        val at = center + Offset(rr * 0.7071f + 4f * dp, -rr * 0.7071f - label.size.height - 2f * dp)
        drawText(label, topLeft = at)
    }

    // Einrast-Ring
    val ringColor = lerp(c.fgMuted, c.accent, level)
    drawCircle(ringColor, rb + 5f * dp, center, style = Stroke(2f * dp))
    if (shine in 0f..1f) {
        val rr = rb + 5f * dp + (r - rb) * shine
        drawCircle(c.accent.copy(alpha = 0.55f * (1f - shine)), rr, center, style = Stroke((3f - 2f * shine) * dp))
    }

    // Blase, gestreckt in Bewegungsrichtung
    val clip = Path().apply { addOval(Rect(center, r - 4f * dp)) }
    clipPath(clip) {
        drawGlow(c.accent, pos, rb * 2.6f, 0.30f + 0.25f * level)
        val vx = p.bubbleX.velocity
        val vy = p.bubbleY.velocity
        val angle = (atan2(vy, vx) * 180f / PI).toFloat()
        val s = p.flatStretch
        rotate(angle, pos) {
            val w = rb * (1f + s)
            val h = rb * (1f - s * 0.35f)
            val oval = Rect(pos.x - w, pos.y - h, pos.x + w, pos.y + h)
            drawOval(
                Brush.radialGradient(
                    0f to lerp(c.accent, Color.White, 0.6f).copy(alpha = 0.95f),
                    0.65f to lerp(c.accent, Color.White, 0.15f).copy(alpha = 0.85f),
                    1f to c.accent.copy(alpha = 0.75f),
                    center = pos - Offset(w * 0.3f, h * 0.35f),
                    radius = w * 1.4f,
                ),
                oval.topLeft, oval.size,
            )
            drawOval(lerp(c.accent, Color.White, 0.6f), oval.topLeft, oval.size, style = Stroke(1.2f * dp))
        }
        val hl = Rect(pos.x - rb * 0.55f, pos.y - rb * 0.62f, pos.x - rb * 0.05f, pos.y - rb * 0.32f)
        drawOval(Color.White.copy(alpha = 0.7f), hl.topLeft, hl.size)
    }

    // Glasrand mit Reflex
    drawArc(
        Color.White.copy(alpha = 0.14f), 200f, 55f, false,
        center - Offset(r - 10f * dp, r - 10f * dp), Size((r - 10f * dp) * 2, (r - 10f * dp) * 2),
        style = Stroke(3f * dp, cap = StrokeCap.Round),
    )
    drawCircle(lerp(c.line, c.accent, level * 0.9f), r, center, style = Stroke(1.5f * dp))
}
