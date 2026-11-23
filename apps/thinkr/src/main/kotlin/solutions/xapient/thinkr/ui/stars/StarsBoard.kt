package solutions.xapient.thinkr.ui.stars

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.thinkr.R
import solutions.xapient.thinkr.domain.Spring
import solutions.xapient.thinkr.domain.stars.Sky
import solutions.xapient.thinkr.domain.stars.StarsPhase
import solutions.xapient.thinkr.domain.stars.StarsRules
import solutions.xapient.thinkr.domain.stars.StarsState
import solutions.xapient.thinkr.ui.Amber
import solutions.xapient.thinkr.ui.CountOverlay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private val Night = Color(0xFF151B2C)
private val StarCore = Color(0xFFFFFFFF)
private val StarWarm = Color(0xFFFBD9F6)

// Sternbild-Brett: alles haengt an der Spielzeit (s.round.now) und steht darum in der Pause still
@Composable
fun StarsBoard(s: StarsState, live: Boolean, count: Int, onTap: (Int) -> Unit) {
    val c = Aapps.colors
    val tap by rememberUpdatedState(onTap)
    val phase = s.phase()
    val p = s.pass
    val desc = stringResource(R.string.stars_board, p.side, p.side)
    // Staub mit drei Tiefen fuer den Parallax-Zug
    val dust = remember { Random(31).let { r -> List(70) { floatArrayOf(r.nextFloat(), r.nextFloat(), r.nextFloat()) } } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        StatusLine(s, phase, count > 0, Modifier.fillMaxWidth().heightIn(min = 56.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
            val side = min(min(maxWidth, maxHeight), 520.dp)
            Box(Modifier.size(side)) {
                Canvas(
                    Modifier
                        .size(side)
                        .semantics { contentDescription = desc }
                        .pointerInput(live, p.no, p.side) {
                            if (!live) return@pointerInput
                            // loest beim Aufsetzen aus; Zelle unter dem Finger
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val cell = size.width.toFloat() / p.side
                                val col = floor(down.position.x / cell).toInt()
                                val row = floor(down.position.y / cell).toInt()
                                if (col in 0 until p.side && row in 0 until p.side) tap(row * p.side + col)
                            }
                        },
                ) {
                    drawSky(s, phase, dust, c.accent, c.fgFaint)
                }
                if (count > 0) CountOverlay(stringResource(R.string.stars_prompt), count)
            }
        }
    }
}

// Ueber dem Feld: Merken bzw. Fund-Zaehler, rechts die zwei Leben
@Composable
private fun StatusLine(s: StarsState, phase: StarsPhase, counting: Boolean, modifier: Modifier) {
    val c = Aapps.colors
    val p = s.pass
    val lives = StarsRules.LIVES - p.errors
    val livesDesc = stringResource(R.string.stars_lives, lives)
    Box(modifier, contentAlignment = Alignment.Center) {
        if (!counting) {
            when (phase) {
                StarsPhase.LEAD, StarsPhase.SHOW -> BasicText(
                    stringResource(R.string.memorize).uppercase(),
                    style = Aapps.type.label.copy(color = c.accent, fontSize = Aapps.type.label.fontSize * 1.2f),
                )
                else -> BasicText(
                    "${p.found} / ${p.stars.size}",
                    Modifier.semantics { contentDescription = "${p.found} / ${p.stars.size}" },
                    style = Aapps.type.number.copy(color = if (p.perfect) c.accent else c.fg),
                )
            }
            Row(Modifier.align(Alignment.CenterEnd).semantics { contentDescription = livesDesc }) {
                repeat(StarsRules.LIVES) { i ->
                    if (i > 0) Spacer(Modifier.width(8.dp))
                    val on = i < lives
                    Canvas(Modifier.size(14.dp)) {
                        val r = size.minDimension / 2
                        val d = diamond(center, r * 0.9f)
                        if (on) {
                            drawGlow(c.accent, center, r * 2.4f, 0.25f)
                            drawPath(d, c.accent)
                        } else {
                            drawPath(d, Amber, style = Stroke(1.5.dp.toPx()))
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawSky(s: StarsState, phase: StarsPhase, dust: List<FloatArray>, accent: Color, faint: Color) {
    val p = s.pass
    val now = s.round.now.toDouble()
    val cell = size.width / p.side
    fun center(i: Int) = Offset((i % p.side + 0.5f) * cell, (i / p.side + 0.5f) * cell)

    // Nachthimmel: weicher Kreis, Staub zieht langsam (je Tiefe verschieden schnell)
    drawCircle(
        Brush.radialGradient(listOf(Night, Night.copy(alpha = 0f)), center, size.width * 0.72f),
        size.width * 0.72f, center,
    )
    dust.forEach { (x, y, z) ->
        val drift = ((x + now * 0.000_006 * (0.4 + z)) % 1.0).toFloat()
        drawCircle(Color.White.copy(alpha = 0.05f + 0.12f * z), (0.5f + 1.1f * z).dp.toPx(), Offset(drift * size.width, y * size.height))
    }

    // Rasterpunkte
    val dot = 2.6.dp.toPx()
    for (i in 0 until p.side * p.side) {
        val o = center(i)
        drawCircle(faint.copy(alpha = 0.7f), dot, o)
    }

    val r = cell * 0.36f
    val pulse = 1f + 0.06f * sin(now / 1000.0 * 2 * PI * 1.1).toFloat()
    when (phase) {
        StarsPhase.LEAD -> Unit
        StarsPhase.SHOW -> {
            // aufleuchten, kurz vor dem Ende verloeschen
            val inT = ((now - s.showAt) / 160.0).coerceIn(0.0, 1.0).toFloat()
            val outT = ((s.recallAt - now) / 180.0).coerceIn(0.0, 1.0).toFloat()
            val a = minOf(inT, outT)
            p.stars.forEachIndexed { k, i ->
                val pop = 1f + 0.25f * Spring.kick(now - s.showAt - k * 18, hz = 4.0, decayMs = 140.0).toFloat()
                drawStar(center(i), r * pulse * pop, accent, a)
            }
        }
        StarsPhase.RECALL, StarsPhase.REVEAL -> Unit
    }

    // Sternbild-Linien nach einem perfekten Durchgang: wachsen nacheinander mit Feder
    if (p.perfect) {
        val links = Sky.links(p.stars, p.side)
        links.forEachIndexed { k, (a, b) ->
            val f = Spring.step(now - p.endAt - 60.0 * k, hz = 2.6, damping = 0.75).toFloat().coerceIn(0f, 1f)
            if (f <= 0f) return@forEachIndexed
            // Linien enden an den Ringen, nicht im Stern
            val ca = center(a)
            val cb = center(b)
            val len = (cb - ca).getDistance()
            val gap = cell * 0.36f * 1.2f
            if (len <= 2 * gap) return@forEachIndexed
            val u = (cb - ca) / len
            val pa = ca + u * gap
            val pb = pa + u * ((len - 2 * gap) * f)
            drawLine(accent.copy(alpha = 0.22f), pa, pb, cell * 0.12f, StrokeCap.Round)
            drawLine(accent, pa, pb, 1.6.dp.toPx(), StrokeCap.Round)
        }
    }

    // Antippen: Treffer = Stern mit Ring, Fehler = X; nach dem Durchgang die verpassten als gestrichelter Ring
    for (t in p.taps) {
        val o = center(t.cell)
        val dt = now - t.at
        if (t.hit) {
            val pop = 1f + 0.3f * Spring.kick(dt, hz = 5.0, decayMs = 130.0).toFloat()
            drawStar(o, r * pop * (if (p.perfect) pulse else 1f), accent, 1f)
            val ring = (1 - exp(-dt / 160.0)).toFloat()
            drawCircle(accent.copy(alpha = 0.85f), r * (1.05f + 0.15f * ring), o, style = Stroke(1.8.dp.toPx()))
        } else {
            val shake = cell * 0.08f * Spring.kick(dt, hz = 9.0, decayMs = 120.0).toFloat()
            drawCross(o + Offset(shake, 0f), r * 0.62f, Amber, 2.6.dp.toPx())
        }
    }
    if (p.done && !p.perfect) {
        val a = ((now - p.endAt) / 220.0).coerceIn(0.0, 1.0).toFloat()
        for (i in p.stars) {
            if (p.tapped(i) != null) continue
            drawCircle(
                Color.White.copy(alpha = 0.55f * a), r * 0.9f, center(i),
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
            )
        }
    }
}

// Leuchtstern: vierzackiger Funke mit Glow
private fun DrawScope.drawStar(c: Offset, r: Float, glow: Color, alpha: Float) {
    if (alpha <= 0.01f) return
    drawGlow(glow, c, r * 2.6f, 0.45f * alpha)
    drawPath(
        spark(c, r, r * 0.3f),
        Brush.radialGradient(listOf(StarCore.copy(alpha = alpha), StarWarm.copy(alpha = alpha)), c, r),
    )
    drawCircle(StarCore.copy(alpha = alpha), r * 0.2f, c)
}

private fun spark(c: Offset, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 8) {
        val a = -PI / 2 + PI * i / 4
        val rr = if (i % 2 == 0) outer else inner
        val x = c.x + rr * cos(a).toFloat()
        val y = c.y + rr * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun diamond(c: Offset, r: Float): Path = Path().apply {
    moveTo(c.x, c.y - r)
    lineTo(c.x + r * 0.8f, c.y)
    lineTo(c.x, c.y + r)
    lineTo(c.x - r * 0.8f, c.y)
    close()
}

private fun DrawScope.drawCross(c: Offset, r: Float, color: Color, w: Float) {
    drawLine(color, c + Offset(-r, -r), c + Offset(r, r), w, StrokeCap.Round)
    drawLine(color, c + Offset(r, -r), c + Offset(-r, r), w, StrokeCap.Round)
}
