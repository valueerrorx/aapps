package solutions.xapient.magnr.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
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
import solutions.xapient.magnr.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Karte ueber dem Radar: Einfuehrung oder Acht-Bewegung; phase >= 0 friert die Animation ein
@Composable
fun HintCard(hint: Hint, onDismiss: () -> Unit, modifier: Modifier = Modifier, phase: Float = -1f) {
    val c = Aapps.colors
    val intro = hint == Hint.INTRO
    val t = when {
        phase >= 0f -> phase
        // Einfuehrung steht still; die Acht laeuft nur, solange die Karte sichtbar ist
        intro -> 0f
        else -> {
            val anim by rememberInfiniteTransition(label = "eight").animateFloat(
                0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "phase",
            )
            anim
        }
    }
    val title = stringResource(if (intro) R.string.intro_title else R.string.cal_title)
    Column(
        modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(28.dp))
            .padding(start = 18.dp, end = 14.dp, top = 18.dp, bottom = 6.dp),
    ) {
        // bei grosser Schrift scrollt der Text, der Knopf bleibt sichtbar
        Row(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalAlignment = Alignment.Top) {
            Canvas(Modifier.size(76.dp)) {
                if (intro) drawPhone(c) else drawEight(c, t)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                BasicText(title.uppercase(), style = Aapps.type.label.copy(color = if (intro) c.accent else Warn))
                Spacer(Modifier.height(6.dp))
                if (intro) {
                    BasicText(stringResource(R.string.intro_finds), style = Aapps.type.body.copy(color = c.fg))
                    Spacer(Modifier.height(6.dp))
                    BasicText(stringResource(R.string.intro_not), style = Aapps.type.body.copy(color = c.fg))
                    Spacer(Modifier.height(6.dp))
                    BasicText(stringResource(R.string.intro_sensor), style = Aapps.type.body.copy(color = c.fgMuted))
                } else {
                    BasicText(stringResource(R.string.cal_body), style = Aapps.type.body.copy(color = c.fg))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AappsTextButton(stringResource(if (intro) R.string.intro_ok else R.string.hint_hide), onDismiss)
        }
    }
}

// Handy von vorn, Sensorpunkt oben mit Wellen, darueber eine Schraube
private fun DrawScope.drawPhone(c: AappsColors) {
    val dp = density
    val pw = 30f * dp
    val ph = 50f * dp
    val left = center.x - pw / 2
    val top = size.height - ph - 2f * dp
    drawRoundRect(c.surfaceHigh, Offset(left, top), Size(pw, ph), CornerRadius(6f * dp))
    drawRoundRect(c.fgMuted, Offset(left, top), Size(pw, ph), CornerRadius(6f * dp), style = Stroke(1.5f * dp))
    val sensor = Offset(center.x, top + 8f * dp)
    drawGlow(MagnrAccent, sensor, 14f * dp, 0.55f)
    drawCircle(MagnrAccent, 3f * dp, sensor)
    // Wellen nach oben zur Schraube
    for (i in 1..2) {
        val rr = 6f * dp + i * 5f * dp
        drawArc(
            MagnrAccent.copy(alpha = 0.8f - i * 0.25f), 225f, 90f, false,
            Offset(sensor.x - rr, sensor.y - rr), Size(2 * rr, 2 * rr), style = Stroke(1.6f * dp, cap = StrokeCap.Round),
        )
    }
    // Schraube: Kopf mit Schlitz
    val screw = Offset(center.x, 7f * dp)
    drawCircle(c.fg, 5f * dp, screw)
    drawLine(c.surface, Offset(screw.x - 3f * dp, screw.y), Offset(screw.x + 3f * dp, screw.y), 1.4f * dp)
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
    for (i in 1..14) {
        drawCircle(Warn.copy(alpha = 0.5f * (1f - i / 15f)), 1.6f * dp, at(t - i * 0.012f))
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
