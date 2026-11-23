package solutions.xapient.dozr.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.dozr.domain.FadeCurve

// Anteil der Breite fuer das Plateau vor dem Ausblenden
const val KNEE = 0.3f

// Kurve nach dem Ende gedaempft, gegen den Grund noch >= 3:1
const val DIM_ALPHA = 0.62f

// Punkt auf der Kurve: Fortschritt 0..1 des Ausblendens; < 0 = kein Punkt, Plateau = am Knie warten
class GraphLook(val progress: Float, val dim: Boolean, val dot: Boolean)

// Ausblend-Verlauf: Plateau, dann weicher Abfall auf 0; Zeiten fuer Beginn und Ende darunter
@Composable
fun FadeGraph(
    title: String,
    startText: String?,
    endText: String?,
    description: String,
    look: () -> GraphLook,
    modifier: Modifier = Modifier,
    curveHeight: Dp = 52.dp,
) {
    val c = Aapps.colors
    val label = Aapps.type.label.copy(color = c.fgMuted)
    val time = Aapps.type.number.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f)
    Column(modifier.fillMaxWidth().semantics { contentDescription = description }) {
        BasicText(title.uppercase(), Modifier.clearAndSetSemantics { }, style = label, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        val current = rememberUpdatedState(look)
        val draw = remember(c) {
            Modifier.drawWithCache {
                val w = size.width
                val h = size.height
                val dp = density
                val top = 6f * dp
                val bottom = h - 4f * dp
                val knee = w * KNEE
                fun y(level: Double) = (bottom - (bottom - top) * level).toFloat()
                fun x(p: Float) = knee + (w - knee) * p
                val curve = Path().apply {
                    moveTo(0f, y(1.0))
                    lineTo(knee, y(1.0))
                    for (i in 1..48) {
                        val p = i / 48f
                        lineTo(x(p), y(FadeCurve.level(p.toDouble())))
                    }
                }
                val area = Path().apply {
                    addPath(curve)
                    lineTo(w, bottom)
                    lineTo(0f, bottom)
                    close()
                }
                val fill = Brush.verticalGradient(0f to DozrAccent.copy(alpha = 0.22f), 1f to DozrAccent.copy(alpha = 0f), startY = top, endY = bottom)
                val dash = PathEffect.dashPathEffect(floatArrayOf(2f * dp, 4f * dp))
                onDrawBehind {
                    val l = current.value()
                    val a = if (l.dim) DIM_ALPHA else 1f
                    drawLine(c.line, Offset(0f, bottom), Offset(w, bottom), 1f * dp)
                    drawLine(c.fgFaint, Offset(knee, top), Offset(knee, bottom), 1f * dp, pathEffect = dash)
                    drawPath(area, fill, alpha = a)
                    drawPath(curve, DozrAccent.copy(alpha = 0.9f * a), style = Stroke(2f * dp, cap = StrokeCap.Round))
                    if (l.dot) {
                        val p = l.progress.coerceIn(0f, 1f)
                        val pt = Offset(x(p), y(FadeCurve.level(p.toDouble())))
                        drawGlow(DozrAccent, pt, 14f * dp, 0.5f)
                        drawCircle(Color.White, 4f * dp, pt)
                        drawCircle(DozrAccent, 4f * dp, pt, style = Stroke(1.5f * dp))
                    }
                }
            }
        }
        Spacer(Modifier.fillMaxWidth().height(curveHeight).then(draw))
        Spacer(Modifier.height(6.dp))
        // Zeit am Knie und am Ende; das Knie-Label rueckt nur so weit, dass es nicht ueberlappt
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            TimeLabels(startText, endText, time)
        }
    }
}

@Composable
private fun TimeLabels(start: String?, end: String?, style: TextStyle) {
    Layout(
        content = {
            BasicText(start ?: "", style = style, maxLines = 1)
            BasicText(end ?: "", style = style, maxLines = 1)
        },
        modifier = Modifier.fillMaxWidth().padding(top = 0.dp),
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val a = measurables[0].measure(constraints.copy(minWidth = 0))
        val b = measurables[1].measure(constraints.copy(minWidth = 0))
        val h = maxOf(a.height, b.height)
        layout(w, h) {
            val bx = w - b.width
            val ax = (w * KNEE - a.width / 2f).toInt().coerceIn(0, (bx - a.width - 8).coerceAtLeast(0))
            a.place(ax, 0)
            b.place(bx, 0)
        }
    }
}
