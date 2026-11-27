package solutions.xapient.spinr.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

// Konfetti aus dem Zeiger: Wurf mit Luftwiderstand, geschlossen berechnet, kein Zustand pro Frame
class Confetti(seed: Int, count: Int = 72) {
    private class Bit(
        val vx: Float, val vy: Float, val color: Int, val w: Float, val h: Float,
        val spin: Float, val flutter: Float, val phase: Float, val round: Boolean,
    )

    private val bits = Random(seed).let { r ->
        List(count) {
            val a = toRadians(r.nextDouble(-75.0, 75.0))
            val v = r.nextDouble(420.0, 1150.0)
            Bit(
                vx = (sin(a) * v).toFloat(), vy = (-cos(a) * v).toFloat(),
                color = r.nextInt(SpinrPalette.size),
                w = r.nextDouble(5.0, 9.0).toFloat(), h = r.nextDouble(8.0, 14.0).toFloat(),
                spin = r.nextDouble(-540.0, 540.0).toFloat(), flutter = r.nextDouble(6.0, 14.0).toFloat(),
                phase = r.nextDouble(0.0, 6.28).toFloat(), round = r.nextInt(4) == 0,
            )
        }
    }

    // t in Sekunden; Groessen in dp
    fun DrawScope.draw(origin: Offset, t: Float) {
        if (t < 0f || t > DURATION) return
        val dp = density
        val e = (1 - exp(-DRAG * t)) / DRAG
        val fade = if (t < DURATION * 0.65f) 1f else 1f - (t - DURATION * 0.65f) / (DURATION * 0.35f)
        for (b in bits) {
            // v' = g - c*v, geschlossen geloest
            val x = origin.x + b.vx * e * dp
            val y = origin.y + (GRAVITY * t / DRAG + (b.vy - GRAVITY / DRAG) * e) * dp
            val color = SpinrPalette[b.color].copy(alpha = fade.coerceIn(0f, 1f))
            if (b.round) {
                drawCircle(color, b.w * 0.5f * dp, Offset(x, y))
            } else {
                // Flattern: die Breite schwingt wie ein kippendes Blatt
                val sx = abs(sin(b.phase + b.flutter * t)) * 0.85f + 0.15f
                rotate(b.spin * t, Offset(x, y)) {
                    drawRect(color, Offset(x - b.w * sx * dp / 2f, y - b.h * dp / 2f), Size(b.w * sx * dp, b.h * dp))
                }
            }
        }
    }

    companion object {
        const val DURATION = 1.9f
        private const val GRAVITY = 1500f
        private const val DRAG = 1.9f
    }
}

private fun toRadians(deg: Double): Double = deg * (kotlin.math.PI / 180.0)
