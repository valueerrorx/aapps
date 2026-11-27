package solutions.xapient.roundr.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.roundr.domain.Phase
import solutions.xapient.roundr.domain.Timeline
import kotlin.math.min

// Ganzes Training als Ring: ein Bogen je Abschnitt, Arbeit breit, Pausen schmal; statisch, ohne Frames
@Composable
fun WorkoutRing(timeline: Timeline, modifier: Modifier = Modifier, glow: Boolean = true, track: Color = Color(0xFF1E2932)) {
    Spacer(
        modifier.drawWithCache {
            val d = min(size.width, size.height)
            val stroke = d * 0.085f
            val radius = d / 2 - stroke / 2 - (if (glow) d * 0.03f else 0f)
            val center = Offset(size.width / 2, size.height / 2)
            val n = timeline.segments.size
            // Spalt in Grad, bei vielen Abschnitten kleiner
            val gap = (360f / n * 0.3f).coerceAtMost(2.4f)
            val usable = 360f - gap * n
            val arcs = ArrayList<Triple<Float, Float, Phase>>(n)
            var a = -90f + gap / 2
            timeline.segments.forEach { s ->
                val sweep = usable * s.durationMs / timeline.totalMs
                arcs += Triple(a, sweep, s.phase)
                a += sweep + gap
            }
            onDrawBehind {
                if (glow) drawGlow(Ember, center, d * 0.62f, 0.16f)
                drawCircle(track, radius, center, style = Stroke(stroke * 1.25f))
                arcs.forEach { (start, sweep, phase) ->
                    val w = if (phase == Phase.WORK) stroke else stroke * 0.55f
                    val r = radius
                    drawArc(
                        phaseColor(phase), start, sweep, false,
                        topLeft = Offset(center.x - r, center.y - r), size = Size(2 * r, 2 * r),
                        style = Stroke(w, cap = StrokeCap.Butt),
                    )
                }
            }
        },
    )
}
