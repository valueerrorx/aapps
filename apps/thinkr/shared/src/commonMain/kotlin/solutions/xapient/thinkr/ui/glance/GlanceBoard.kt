package solutions.xapient.thinkr.ui.glance

import solutions.xapient.thinkr.res.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.thinkr.domain.Spring
import solutions.xapient.thinkr.domain.echo.Glyph
import solutions.xapient.thinkr.domain.glance.GlanceInput
import solutions.xapient.thinkr.domain.glance.GlancePhase
import solutions.xapient.thinkr.domain.glance.GlanceState
import solutions.xapient.thinkr.domain.glance.Mark
import solutions.xapient.thinkr.domain.glance.Spots
import solutions.xapient.thinkr.ui.Motion
import solutions.xapient.thinkr.ui.Amber
import solutions.xapient.thinkr.ui.CountOverlay
import solutions.xapient.thinkr.ui.drawGlass
import solutions.xapient.thinkr.ui.drawGlyph
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val Decoy = Color(0xFFBFC8D6)

fun glyphOf(m: Mark): Glyph = if (m == Mark.ORB) Glyph.CIRCLE else Glyph.DIAMOND

// Rundblick-Brett: Sternenfeld mit Ringen, unten die zwei Mitte-Tasten; alles haengt an der Spielzeit
@Composable
fun GlanceBoard(s: GlanceState, live: Boolean, count: Int, onInput: (GlanceInput) -> Unit) {
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val phase = s.phase()
    val asking = live && phase == GlancePhase.ANSWER
    val tr = s.trial
    val now = s.round.now.toDouble()
    val button: @Composable (Mark, Modifier) -> Unit = { m, mod ->
        val chosen = tr.saidMark == m
        val flash = if (chosen && tr.done) exp(-(now - tr.doneAt) / 260.0).toFloat() else 0f
        val good = tr.mark == m || tr.void
        MarkButton(m, asking && tr.saidMark == null, chosen, flash, good, { onInput(GlanceInput.Center(m)) }, mod)
    }
    val field: @Composable (Modifier) -> Unit = { m ->
        Column(m, horizontalAlignment = Alignment.CenterHorizontally) {
            Status(s, phase, count > 0, Modifier.fillMaxWidth().heightIn(min = 48.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Field(s, phase, live, asking && tr.saidSpot == null, { onInput(GlanceInput.Spot(it)) }, Modifier.fillMaxSize())
                if (count > 0) CountOverlay(stringResource(Res.string.glance_prompt), count)
            }
        }
    }
    if (landscape) {
        Row(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            button(Mark.ORB, Modifier.width(140.dp).height(124.dp))
            field(Modifier.weight(1f).fillMaxHeight())
            button(Mark.DIAMOND, Modifier.width(140.dp).height(124.dp))
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            field(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp))
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                button(Mark.ORB, Modifier.weight(1f).heightIn(min = 88.dp))
                button(Mark.DIAMOND, Modifier.weight(1f).heightIn(min = 88.dp))
            }
        }
    }
}

@Composable
private fun Status(s: GlanceState, phase: GlancePhase, counting: Boolean, modifier: Modifier) {
    val c = Aapps.colors
    val tr = s.trial
    Box(modifier, contentAlignment = Alignment.Center) {
        if (counting) return@Box
        val (text, color) = when {
            phase == GlancePhase.FEEDBACK && tr.void -> stringResource(Res.string.glance_void) to c.fgMuted
            phase != GlancePhase.ANSWER -> "" to c.fgFaint
            tr.saidMark == null && tr.saidSpot == null -> stringResource(Res.string.glance_ask_both) to c.accent
            tr.saidMark == null -> stringResource(Res.string.glance_ask_center) to c.accent
            else -> stringResource(Res.string.glance_ask_spot) to c.accent
        }
        BasicText(
            text.uppercase(),
            style = Aapps.type.label.copy(color = color, fontSize = Aapps.type.label.fontSize * 1.15f, textAlign = TextAlign.Center),
            maxLines = 2,
        )
    }
}

@Composable
private fun Field(s: GlanceState, phase: GlancePhase, live: Boolean, tappable: Boolean, onSpot: (Int) -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    val tap by rememberUpdatedState(onSpot)
    val dust = remember { Random(71).let { r -> List(80) { floatArrayOf(r.nextFloat(), r.nextFloat(), r.nextFloat()) } } }
    val desc = stringResource(Res.string.glance_board, Spots.count(s.rings))
    Canvas(
        modifier
            .semantics { contentDescription = desc }
            .pointerInput(tappable, s.trial.no, s.rings) {
                if (!tappable) return@pointerInput
                // naechste Stelle zum Finger, wenn nah genug
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val geo = Geo(size.width.toFloat(), size.height.toFloat())
                    val best = (0 until Spots.count(s.rings)).minBy { (geo.spot(it) - down.position).getDistance() }
                    if ((geo.spot(best) - down.position).getDistance() < geo.reach) tap(best)
                }
            },
    ) {
        drawField(s, phase, live, dust, c.accent, c.fgFaint, c.fg)
    }
}

// Lage der Stellen: Ringe haengen am kuerzeren Mass des Felds
private class Geo(val w: Float, val h: Float) {
    val center = Offset(w / 2, h / 2)
    val half = min(w, h) / 2 * 0.94f
    val glyph = half * 0.17f
    val reach = half * 0.16f

    fun radius(ring: Int) = half * Spots.RADII[ring]

    fun spot(i: Int): Offset {
        val a = Spots.angle(i)
        val r = radius(Spots.ring(i))
        return center + Offset(cos(a).toFloat() * r, sin(a).toFloat() * r)
    }
}

private fun DrawScope.drawField(s: GlanceState, phase: GlancePhase, live: Boolean, dust: List<FloatArray>, accent: Color, faint: Color, fg: Color) {
    val geo = Geo(size.width, size.height)
    val now = s.round.now.toDouble()
    val tr = s.trial
    val n = Spots.count(s.rings)

    // Sternenfeld: ruhig, nur ein Hauch Drift
    dust.forEach { (x, y, z) ->
        val drift = ((x + now * 0.000_003 * (0.3 + z)) % 1.0).toFloat()
        drawCircle(Color.White.copy(alpha = 0.04f + 0.1f * z), (0.5f + 1f * z).dp.toPx(), Offset(drift * size.width, y * size.height))
    }
    // Positionsringe und dezente Punkte
    for (r in 0 until s.rings) {
        drawCircle(faint.copy(alpha = 0.16f), geo.radius(r), geo.center, style = Stroke(1.dp.toPx()))
    }
    val asking = phase == GlancePhase.ANSWER && tr.saidSpot == null
    for (i in 0 until n) {
        drawCircle(faint.copy(alpha = if (asking) 0.75f else 0.4f), (if (asking) 4f else 2.6f).dp.toPx(), geo.spot(i))
    }

    when (phase) {
        GlancePhase.FIX -> {
            // Fixierpunkt atmet leicht; kurz vor dem Blitz ruhig
            val a = 1f - Motion.still(0.35f) + Motion.still(0.35f) * sin(now / 1000.0 * 2 * PI * 0.8).toFloat()
            drawFixation(geo.center, geo.glyph * 0.35f, accent, a)
        }
        GlancePhase.FLASH -> if (live) {
            // scharf, ohne Glow-Nachklang: der Blitz soll kurz sein
            drawGlyph(glyphOf(tr.mark), geo.center, geo.glyph * 2f, glow = Color.Transparent)
            drawGlyph(Glyph.STAR, geo.spot(tr.spot), geo.glyph * 2f, glow = Color.Transparent)
            tr.decoys.forEach { drawDecoy(geo.spot(it), geo.glyph, Decoy) }
        } else {
            drawFixation(geo.center, geo.glyph * 0.35f, accent, 1f)
        }
        GlancePhase.MASK -> {
            // Rauschmuster gegen das Nachbild, an Mitte und allen Stellen
            val rng = Random(tr.no * 31L + (now / 50).toLong())
            drawNoise(geo.center, geo.glyph * 1.1f, rng, fg)
            for (i in 0 until n) drawNoise(geo.spot(i), geo.glyph * 1.1f, rng, fg)
        }
        GlancePhase.ANSWER -> {
            drawFixation(geo.center, geo.glyph * 0.3f, faint, 0.8f)
            tr.saidSpot?.let { drawCircle(accent, geo.reach * 0.55f, geo.spot(it), style = Stroke(2.dp.toPx())) }
        }
        GlancePhase.FEEDBACK -> drawFeedback(s, geo, now, accent)
    }
}

// Aufloesung: Treffer pulsiert und zieht eine Linie zur Mitte; daneben Bernstein-X und das echte Ziel
private fun DrawScope.drawFeedback(s: GlanceState, geo: Geo, now: Double, accent: Color) {
    val tr = s.trial
    val dt = now - tr.doneAt
    val target = geo.spot(tr.spot)
    val hit = tr.saidSpot == tr.spot
    val centerOk = tr.saidMark == tr.mark
    val okColor = if (tr.void) Color.White.copy(alpha = 0.6f) else accent
    // Linie waechst mit Feder von der Mitte zum Ziel
    val f = Spring.step(dt, hz = 2.6, damping = 0.75).toFloat().coerceIn(0f, 1f)
    if (hit && centerOk) {
        val from = geo.center
        val to = from + (target - from) * f
        drawLine(okColor.copy(alpha = 0.25f), from, to, geo.glyph * 0.35f, StrokeCap.Round)
        drawLine(okColor, from, to, 1.6.dp.toPx(), StrokeCap.Round)
    }
    val pop = 1f + 0.3f * Spring.kick(dt, hz = 4.0, decayMs = 180.0).toFloat()
    drawGlyph(Glyph.STAR, target, geo.glyph * 1.7f * (if (hit) pop else 1f), glow = if (hit) okColor else Color.Transparent, alpha = if (hit) 1f else 0.55f)
    if (hit) drawCircle(okColor, geo.reach * 0.75f * pop, target, style = Stroke(2.dp.toPx()))
    tr.saidSpot?.takeIf { !hit }?.let {
        val p = geo.spot(it)
        val shake = Spring.kick(dt, hz = 9.0, decayMs = 120.0).toFloat() * geo.glyph * 0.3f
        drawCross(p + Offset(shake, 0f), geo.glyph * 0.45f, Amber, 2.4.dp.toPx())
        drawCircle(Color.White.copy(alpha = 0.5f), geo.reach * 0.75f, target, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
    }
    // Mitte zur Kontrolle nochmal, Bernstein-Rand wenn falsch
    drawGlyph(glyphOf(tr.mark), geo.center, geo.glyph * 1.5f, glow = if (centerOk) okColor else Amber, alpha = 0.9f)
    if (!centerOk) drawCircle(Amber, geo.glyph * 1.05f, geo.center, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.drawFixation(c: Offset, r: Float, color: Color, alpha: Float) {
    drawGlow(color, c, r * 3f, 0.3f * alpha)
    drawLine(color.copy(alpha = alpha), c - Offset(r, 0f), c + Offset(r, 0f), 2.dp.toPx(), StrokeCap.Round)
    drawLine(color.copy(alpha = alpha), c - Offset(0f, r), c + Offset(0f, r), 2.dp.toPx(), StrokeCap.Round)
}

// Koeder: vierzackiger Funke, aehnlich dem Stern, aber nicht gleich
private fun DrawScope.drawDecoy(c: Offset, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 8) {
        val a = -PI / 2 + PI * i / 4
        val rr = if (i % 2 == 0) r * 0.95f else r * 0.38f
        val x = c.x + rr * cos(a).toFloat()
        val y = c.y + rr * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color)
}

// Rauschen: Gitter aus kleinen Flecken in zufaelligem Grau
private fun DrawScope.drawNoise(c: Offset, r: Float, rng: Random, fg: Color) {
    val k = 5
    val cell = r * 2 / k
    for (y in 0 until k) for (x in 0 until k) {
        val o = c + Offset(-r + cell * x, -r + cell * y)
        if (hypot(o.x + cell / 2 - c.x, o.y + cell / 2 - c.y) > r) continue
        drawRect(fg.copy(alpha = 0.15f + 0.6f * rng.nextFloat()), o, androidx.compose.ui.geometry.Size(cell * 0.9f, cell * 0.9f))
    }
}

private fun DrawScope.drawCross(c: Offset, r: Float, color: Color, w: Float) {
    drawLine(color, c + Offset(-r, -r), c + Offset(r, r), w, StrokeCap.Round)
    drawLine(color, c + Offset(r, -r), c + Offset(-r, r), w, StrokeCap.Round)
}

// Taste fuer die Mitte: zeigt die Form, loest beim Aufsetzen aus
@Composable
private fun MarkButton(m: Mark, enabled: Boolean, chosen: Boolean, flash: Float, good: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    var pressed by remember { mutableStateOf(false) }
    val click by rememberUpdatedState(onClick)
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, Springs.bouncy(), label = "markScale")
    val dim by animateFloatAsState(if (enabled || chosen) 1f else 0.45f, Springs.gentle(), label = "markDim")
    val label = stringResource(if (m == Mark.ORB) Res.string.glyph_circle else Res.string.glyph_diamond)
    val edge = when {
        flash > 0.01f -> (if (good) c.accent else Amber).copy(alpha = 0.3f + 0.7f * flash)
        chosen -> c.accent
        else -> c.line
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = dim }
            .semantics {
                role = Role.Button
                contentDescription = label
                if (enabled) onClick { click(); true }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    click()
                    pressed = true
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        pressed = false
                    }
                }
            }
            .drawBehind { drawGlass(Offset.Zero, size, 26.dp.toPx(), edge) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(40.dp)) { drawGlyph(glyphOf(m), center, size.minDimension, alpha = if (chosen || enabled) 1f else 0.7f) }
    }
}
