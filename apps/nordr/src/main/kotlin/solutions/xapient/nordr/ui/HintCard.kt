package solutions.xapient.nordr.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.aapps.core.ui.AappsTextButton
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.nordr.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Karte ueber dem Kompass: Acht-Bewegung oder Stoerung; phase >= 0 friert die Animation ein
@Composable
fun HintCard(hint: Hint, manual: Boolean, onDismiss: () -> Unit, phase: Float = -1f) {
    val c = Aapps.colors
    val t = if (phase >= 0f) {
        phase
    } else {
        // laeuft nur, solange die Karte sichtbar ist
        val anim by rememberInfiniteTransition(label = "eight").animateFloat(
            0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "phase",
        )
        anim
    }
    val title = stringResource(if (hint == Hint.INTERFERENCE) R.string.interference_title else R.string.cal_title)
    val body = stringResource(if (hint == Hint.INTERFERENCE) R.string.interference_body else R.string.cal_body)

    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(c.surface.copy(alpha = 0.96f))
            .border(1.dp, c.line, RoundedCornerShape(28.dp))
            .padding(start = 18.dp, end = 14.dp, top = 18.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(76.dp)) {
                if (hint == Hint.INTERFERENCE) drawMagnet(c, t) else drawEight(c, t)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                BasicText(title.uppercase(), style = Aapps.type.label.copy(color = if (manual) c.accent else Warn))
                Spacer(Modifier.height(6.dp))
                BasicText(body, style = Aapps.type.body.copy(color = c.fg))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AappsTextButton(stringResource(if (manual) R.string.hint_done else R.string.hint_hide), onDismiss)
        }
    }
}

// Liegende Acht mit einem Handy, das ihr folgt und dabei kippt
private fun DrawScope.drawEight(c: AappsColors, t: Float) {
    val dp = density
    val w = size.width * 0.42f
    val h = size.height * 0.3f
    fun at(u: Float): Offset {
        val a = u * 2.0 * PI
        return Offset(center.x + (sin(a) * w).toFloat(), center.y + (sin(a) * cos(a) * h * 2).toFloat())
    }
    val path = Path()
    for (i in 0..96) {
        val o = at(i / 96f)
        if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
    }
    drawPath(path, c.line, style = Stroke(2f * dp, cap = StrokeCap.Round))
    // Spur hinter dem Handy
    for (i in 1..14) {
        val o = at(t - i * 0.012f)
        drawCircle(Warn.copy(alpha = 0.5f * (1f - i / 15f)), 1.6f * dp, o)
    }
    val pos = at(t)
    val ahead = at(t + 0.01f)
    val angle = (atan2(ahead.y - pos.y, ahead.x - pos.x) * 180f / PI).toFloat()
    drawGlow(Warn, pos, 16f * dp, 0.35f)
    rotate(angle * 0.35f + 90f * sin(t * 2f * PI).toFloat() * 0.15f, pos) {
        val pw = 11f * dp
        val ph = 19f * dp
        drawRoundRect(c.surfaceHigh, Offset(pos.x - pw / 2, pos.y - ph / 2), Size(pw, ph), CornerRadius(3f * dp))
        drawRoundRect(Warn, Offset(pos.x - pw / 2, pos.y - ph / 2), Size(pw, ph), CornerRadius(3f * dp), style = Stroke(1.5f * dp))
    }
}

// Hufeisenmagnet mit verbogenen Feldlinien
private fun DrawScope.drawMagnet(c: AappsColors, t: Float) {
    val dp = density
    val cx = center.x
    val top = size.height * 0.22f
    val r = size.width * 0.2f
    val legBottom = size.height * 0.72f
    val stroke = 7f * dp
    val pulse = 0.5f + 0.5f * sin(t * 2f * PI).toFloat()
    drawGlow(Warn, Offset(cx, legBottom), size.width * 0.5f, 0.15f + 0.15f * pulse)
    // Feldlinien zwischen den Polen
    for (i in 1..3) {
        val spread = r + i * 6f * dp
        val depth = legBottom + i * 7f * dp * (0.85f + 0.15f * pulse)
        val p = Path().apply {
            moveTo(cx - r, legBottom + 4f * dp)
            cubicTo(cx - spread, depth, cx + spread, depth, cx + r, legBottom + 4f * dp)
        }
        drawPath(p, Warn.copy(alpha = 0.55f - i * 0.12f), style = Stroke(1.4f * dp, cap = StrokeCap.Round))
    }
    val u = Path().apply {
        moveTo(cx - r, legBottom)
        lineTo(cx - r, top + r)
        arcTo(androidx.compose.ui.geometry.Rect(cx - r, top, cx + r, top + 2 * r), 180f, 180f, false)
        lineTo(cx + r, legBottom)
    }
    drawPath(u, c.fgMuted, style = Stroke(stroke, cap = StrokeCap.Butt))
    // Pole
    for (s in floatArrayOf(-1f, 1f)) {
        drawLine(Warn, Offset(cx + s * r, legBottom - 7f * dp), Offset(cx + s * r, legBottom), strokeWidth = stroke)
    }
}
