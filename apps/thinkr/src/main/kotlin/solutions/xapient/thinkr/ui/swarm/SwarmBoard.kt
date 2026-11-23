package solutions.xapient.thinkr.ui.swarm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.thinkr.R
import solutions.xapient.thinkr.domain.Spring
import solutions.xapient.thinkr.domain.swarm.Dir
import solutions.xapient.thinkr.domain.swarm.Flock
import solutions.xapient.thinkr.domain.swarm.Formation
import solutions.xapient.thinkr.domain.swarm.Formations
import solutions.xapient.thinkr.domain.swarm.SwarmAnswer
import solutions.xapient.thinkr.domain.swarm.SwarmRules
import solutions.xapient.thinkr.domain.swarm.SwarmState
import solutions.xapient.thinkr.ui.Amber
import solutions.xapient.thinkr.ui.CountOverlay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val CometCore = Color(0xFFFFFFFF)
private val CometTint = Color(0xFFFAD3F5)

// Abgang des letzten Schwarms in ms Spielzeit
private const val OUT_MS = 420.0

// Abstand zwischen Kometen in Kometen-Einheiten
private const val SPACING = 1.5f

// Flugzeit, fuer die Platz eingeplant wird (laengste Frist der Hohl-Stufen ohne Gemaechlich)
private const val DRIFT_SPAN_S = 3.2f

// Schwarm-Brett: Wisch in eine von vier Richtungen; alle Bewegung haengt an der Spielzeit
@Composable
fun SwarmBoard(s: SwarmState, live: Boolean, count: Int, onSwipe: (Dir) -> Unit) {
    val c = Aapps.colors
    val swipe by rememberUpdatedState(onSwipe)
    val threshold = with(LocalDensity.current) { 28.dp.toPx() }
    val desc = stringResource(R.string.swarm_board)
    val labels = Dir.entries.associateWith { stringResource(dirLabel(it)) }
    val dust = remember { Random(57).let { r -> List(60) { floatArrayOf(r.nextFloat(), r.nextFloat(), r.nextFloat()) } } }
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(bottom = 16.dp)) {
        val maxUnit = with(LocalDensity.current) { 56.dp.toPx() }
        Canvas(
            Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = desc
                    if (live) customActions = Dir.entries.map { d -> CustomAccessibilityAction(labels.getValue(d)) { swipe(d); true } }
                }
                .pointerInput(live, threshold) {
                    if (!live) return@pointerInput
                    // loest aus, sobald der Finger weit genug gezogen hat: Reaktionszeit zaehlt
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var fired = false
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            val d = ch.position - down.position
                            if (!fired && d.getDistance() > threshold) {
                                fired = true
                                swipe(dirOf(d))
                            }
                            if (fired) ch.consume()
                            if (!ch.pressed) break
                        }
                    }
                },
        ) {
            drawField(s, dust, maxUnit, c.accent)
        }
        if (count > 0) CountOverlay(stringResource(if (s.hollowRule) R.string.swarm_prompt_hollow else R.string.swarm_prompt), count)
    }
}

private fun dirLabel(d: Dir): Int = when (d) {
    Dir.UP -> R.string.dir_up
    Dir.RIGHT -> R.string.dir_right
    Dir.DOWN -> R.string.dir_down
    Dir.LEFT -> R.string.dir_left
}

// Hauptachse des Wischs
private fun dirOf(d: Offset): Dir = if (abs(d.x) > abs(d.y)) {
    if (d.x > 0) Dir.RIGHT else Dir.LEFT
} else {
    if (d.y > 0) Dir.DOWN else Dir.UP
}

private fun Dir.vec(): Offset = Offset(dx.toFloat(), dy.toFloat())

private fun DrawScope.drawField(s: SwarmState, dust: List<FloatArray>, maxUnit: Float, accent: Color) {
    val now = s.round.now.toDouble()
    // ruhiger Staub mit langsamer Parallax-Drift
    dust.forEach { (x, y, z) ->
        val drift = ((y + now * 0.000_004 * (0.4 + z)) % 1.0).toFloat()
        drawCircle(Color.White.copy(alpha = 0.04f + 0.1f * z), (0.5f + 1f * z).dp.toPx(), Offset(x * size.width, (1f - drift) * size.height))
    }
    // Kometengroesse je Runde fest: die groesste Formation dieser Schwarmgroesse muss passen
    val all = Formation.entries.flatMap { Formations.spots(it, s.flock.size) }
    val extX = all.maxOf { abs(it.x) }
    val extY = all.maxOf { abs(it.y) }
    val u = min(min(size.width / (2 * extX * SPACING + 2.6f), size.height / (2 * extY * SPACING + 2.6f)), maxUnit)
    val last = s.last
    if (last != null && now - last.at < OUT_MS && last.index < s.flocks.size) {
        drawFlock(s, s.flocks[last.index], u, accent, shownAt = last.shownAt, until = last.at.toDouble(), out = last, now = now)
    }
    if (now >= s.shownAt) drawFlock(s, s.flock, u, accent, shownAt = s.shownAt, until = now, out = null, now = now)
}

// Ein Schwarm; out != null: tritt nach der Antwort ab (richtig: zieht in Wischrichtung davon, falsch: wackelt bernstein und verblasst)
private fun DrawScope.drawFlock(
    s: SwarmState,
    f: Flock,
    u: Float,
    accent: Color,
    shownAt: Long,
    until: Double,
    out: SwarmAnswer?,
    now: Double,
) {
    val spots = Formations.spots(f.formation, f.size)
    val ex = spots.maxOf { abs(it.x) } * u * SPACING + u * 1.3f
    val ey = spots.maxOf { abs(it.y) } * u * SPACING + u * 1.3f
    // Startpunkt so, dass auch der fliegende Schwarm im Brett bleibt
    val span = u * SwarmRules.DRIFT_SPEED.toFloat() * DRIFT_SPAN_S * (if (s.relaxed) SwarmRules.RELAXED.toFloat() else 1f)
    val dv = f.drift?.vec() ?: Offset.Zero
    val cx = place(f.x, ex + span * maxOf(0f, -dv.x), size.width - ex - span * maxOf(0f, dv.x), size.width)
    val cy = place(f.y, ey + span * maxOf(0f, -dv.y), size.height - ey - span * maxOf(0f, dv.y), size.height)
    // Flug des ganzen Schwarms (nur mit Hohl-Regel), bis zur Antwort
    val flown = ((until - shownAt) / 1000.0).coerceAtLeast(0.0).toFloat()
    val drift = dv * (u * SwarmRules.DRIFT_SPEED.toFloat() * flown)
    val enter = if (out == null) Spring.step(now - shownAt, hz = 3.0, damping = 0.7).toFloat() else 1f
    val p = if (out != null) ((now - out.at) / OUT_MS).toFloat().coerceIn(0f, 1f) else 0f
    val good = out?.correct == true
    val leave = if (good && out?.said != null) out.said.vec() * (u * 7f * p * p) else Offset.Zero
    val shake = if (out != null && !good) Offset(sin(p * 6f * PI.toFloat()) * (1f - p) * u * 0.25f, 0f) else Offset.Zero
    val alpha = (if (out != null) 1f - p else enter).coerceIn(0f, 1f)
    val tint = if (out != null && !good) lerp(CometTint, Amber, 1f - p) else CometTint
    val scale = 0.7f + 0.3f * enter
    spots.forEachIndexed { i, sp ->
        val center = i == 0
        // Unruhe: jedes Glied zittert leicht, je hoeher die Stufe, desto mehr
        val j = s.jitter.toFloat() * u
        val wob = Offset(
            sin(now / 1000.0 * 2 * PI * 1.3 + i * 1.7).toFloat() * j,
            cos(now / 1000.0 * 2 * PI * 1.1 + i * 2.3).toFloat() * j,
        )
        val pos = Offset(cx, cy) + Offset(sp.x, sp.y) * (u * SPACING * scale) + drift + leave + shake + if (center) Offset.Zero else wob
        val own = (if (center) f.point else f.flank).vec()
        // beim Abzug drehen alle Kometen in die Wischrichtung
        val turn = if (good && out?.said != null) (p * 3f).coerceAtMost(1f) else 0f
        val dir = if (turn > 0f) turnTo(own, out!!.said!!.vec(), turn) else own
        drawComet(pos, dir, u * (if (center) 1.3f else 1f), tint, accent, alpha * (if (center) 1f else 0.85f), hollow = center && f.hollow, trail = 1f + 2f * p * (if (good) 1f else 0f))
        if (center) {
            // Ring markiert den Mittleren: Form, nicht Farbe
            drawGlow(accent, pos, u * 1.2f, 0.18f * alpha)
            drawCircle(accent.copy(alpha = 0.9f * alpha), u * 0.72f, pos, style = Stroke(1.8.dp.toPx()))
        }
    }
}

// f (0..1) im Bereich lo..hi; passt es nicht, mittig zwischen lo und hi, notfalls Brettmitte
private fun place(f: Float, lo: Float, hi: Float, full: Float): Float = when {
    hi >= lo -> lo + f * (hi - lo)
    else -> ((lo + hi) / 2).coerceIn(0f, full)
}

// Einheitsvektor von a nach b drehen (kuerzester Winkel), t = 0..1
private fun turnTo(a: Offset, b: Offset, t: Float): Offset {
    val wa = atan2(a.y, a.x)
    var d = atan2(b.y, b.x) - wa
    while (d > PI) d -= (2 * PI).toFloat()
    while (d < -PI) d += (2 * PI).toFloat()
    // genau entgegengesetzt: immer gleich herum drehen
    val w = wa + d * t
    return Offset(cos(w), sin(w))
}

// Komet: Pfeilkopf mit Lichtschweif hinter sich; hohl = nur Umriss
private fun DrawScope.drawComet(p: Offset, d: Offset, u: Float, tint: Color, glow: Color, alpha: Float, hollow: Boolean, trail: Float) {
    if (alpha <= 0.01f) return
    val n = Offset(-d.y, d.x)
    val tip = p + d * (u * 0.34f)
    val back = p - d * (u * 0.2f)
    val notch = p - d * (u * 0.06f)
    val tailEnd = back - d * (u * 0.8f * trail)
    drawPath(
        Path().apply {
            moveTo(back.x + n.x * u * 0.13f, back.y + n.y * u * 0.13f)
            lineTo(tailEnd.x, tailEnd.y)
            lineTo(back.x - n.x * u * 0.13f, back.y - n.y * u * 0.13f)
            close()
        },
        Brush.linearGradient(listOf(glow.copy(alpha = 0.55f * alpha), glow.copy(alpha = 0f)), back, tailEnd),
    )
    drawGlow(glow, p, u * 0.7f, 0.28f * alpha)
    val head = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(back.x + n.x * u * 0.28f, back.y + n.y * u * 0.28f)
        lineTo(notch.x, notch.y)
        lineTo(back.x - n.x * u * 0.28f, back.y - n.y * u * 0.28f)
        close()
    }
    if (hollow) {
        drawPath(head, tint.copy(alpha = alpha), style = Stroke(u * 0.07f, join = StrokeJoin.Round))
    } else {
        drawPath(head, Brush.linearGradient(listOf(CometCore.copy(alpha = alpha), tint.copy(alpha = alpha)), tip, back))
    }
}
