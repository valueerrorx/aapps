package solutions.xapient.thinkr.ui.switches

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.thinkr.R
import solutions.xapient.thinkr.domain.Spring
import solutions.xapient.thinkr.domain.switches.Kind
import solutions.xapient.thinkr.domain.switches.Net
import solutions.xapient.thinkr.domain.switches.Pt
import solutions.xapient.thinkr.domain.switches.SwitchesState
import solutions.xapient.thinkr.ui.Amber
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val RailOn = Color(0xFFCFDDE8)
private val RailOff = Color(0xFF3C4955)
private val Hollow = Color(0xFF131B22)
private val Knob = Color(0xFF1E2932)
private val KnobRim = Color(0xFF3A4752)
private val Smoke = Color(0xFF8A97A2)

// Rand um das Netz in Rasterfeldern (Tore, Lichthof)
private const val MARGIN = 1.3f

// Netz auf die Flaeche gelegt: Ausrichtung mit dem groesseren Raster, quer wird das Netz gedreht
private class NetLayout(val net: Net, w: Float, h: Float, maxUnit: Float) {
    val turned: Boolean
    val unit: Float
    private val ox: Float
    private val oy: Float

    init {
        val nw = net.width.toFloat()
        val nh = net.height.toFloat()
        val upright = min(w / (nw + 2 * MARGIN), h / (nh + 2 * MARGIN))
        val across = min(w / (nh + 2 * MARGIN), h / (nw + 2 * MARGIN))
        turned = across > upright * 1.08f
        unit = min(if (turned) across else upright, maxUnit)
        val gw = (if (turned) nh else nw) * unit
        val gh = (if (turned) nw else nh) * unit
        ox = (w - gw) / 2
        oy = (h - gh) / 2
    }

    fun map(p: Pt): Offset =
        if (turned) Offset(ox + p.y.toFloat() * unit, oy + p.x.toFloat() * unit) else Offset(ox + p.x.toFloat() * unit, oy + p.y.toFloat() * unit)

    val nodes: List<Offset> = net.nodes.map { map(it.at) }

    val rails: List<Path> = net.edges.map { e ->
        Path().apply {
            e.track.sample(0.12).forEachIndexed { i, p ->
                val o = map(p)
                if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
            }
        }
    }

    // Bildschirm-Winkel des Abgangs einer Kante (Weichenpfeil)
    val heading: List<Float> = net.edges.map { e ->
        val a = map(e.track.points[0])
        val b = map(e.track.points[1])
        atan2(b.y - a.y, b.x - a.x)
    }

    // Richtung, aus der die Perle ins Tor faehrt
    val arrival: List<Float> = net.edges.map { e ->
        val p = e.track.points
        val a = map(p[p.size - 2])
        val b = map(p.last())
        atan2(b.y - a.y, b.x - a.x)
    }

    // naechste Weiche im Tippradius
    fun hit(o: Offset, radius: Float): Int? {
        var best: Int? = null
        var bestD = radius
        for (n in net.switches) {
            val d = (nodes[n] - o).getDistance()
            if (d <= bestD) { bestD = d; best = net.nodes[n].slot }
        }
        return best
    }
}

// Weichen-Brett: alles haengt an der Spielzeit (s.round.now) und steht darum in der Pause still
@Composable
fun SwitchesBoard(s: SwitchesState, live: Boolean, count: Int, onSwitch: (Int) -> Unit) {
    val c = Aapps.colors
    val density = LocalDensity.current
    val tap by rememberUpdatedState(onSwitch)
    val desc = stringResource(R.string.switches_board, s.net.gateCount)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val maxUnit = with(density) { 40.dp.toPx() }
        val minTap = with(density) { 26.dp.toPx() }
        val layout = remember(s.net, w, h, maxUnit) { NetLayout(s.net, w, h, maxUnit) }
        Canvas(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = desc }
                .pointerInput(live, layout) {
                    if (!live) return@pointerInput
                    // loest beim Aufsetzen aus: im dichten Takt zaehlt jede Millisekunde
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        layout.hit(down.position, max(minTap, layout.unit * 1.15f))?.let { tap(it) }
                    }
                },
        ) {
            drawNet(s, layout, c.accent)
        }
        if (count > 0) {
            Box(Modifier.fillMaxSize().background(c.bg.copy(alpha = 0.62f)), contentAlignment = Alignment.Center) {
                // eigener Grund, damit die Frage nicht auf den Schienen liegt
                Column(
                    Modifier
                        .padding(24.dp)
                        .background(c.bg.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText(
                        stringResource(R.string.switches_prompt),
                        style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center),
                        maxLines = 3,
                    )
                    Spacer(Modifier.height(12.dp))
                    BasicText(count.toString(), style = Aapps.type.display.copy(color = c.accent, fontSize = Aapps.type.display.fontSize * 0.8f))
                }
            }
        }
    }
}

private fun DrawScope.drawNet(s: SwitchesState, l: NetLayout, accent: Color) {
    val net = s.net
    val u = l.unit
    val now = s.round.now.toDouble()

    // Schienen: der gestellte Abgang hell, der andere gedimmt; der Wechsel folgt der Feder
    net.edges.forEachIndexed { i, e ->
        val from = net.nodes[e.from]
        val act = if (from.kind == Kind.SWITCH) {
            val p = Spring.step(now - s.flippedAt[from.slot]).toFloat().coerceIn(0f, 1f)
            if (net.next(e.from, s.on[from.slot]) == i) p else 1f - p
        } else {
            1f
        }
        drawPath(l.rails[i], RailOn.copy(alpha = 0.03f + 0.06f * act), style = Stroke(u * 0.7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(l.rails[i], lerp(RailOff, RailOn, act).copy(alpha = 0.55f + 0.4f * act), style = Stroke(u * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // Tore: Ring mit Oeffnung zur Schiene, Form innen; Ankunft schluckt mit Welle, Fehler wackelt bernstein
    for (g in 0 until net.gateCount) {
        val node = net.gates[g]
        val tint = SwitchPalette[s.colors[g]]
        val last = s.arrivals.lastOrNull { it.gate == g }
        val dt = if (last != null) now - last.at else 1e9
        val good = last?.correct == true
        val pop = if (good) 0.2f * Spring.kick(dt, hz = 5.0, decayMs = 150.0).toFloat() else 0f
        val shake = if (!good && last != null) u * 0.22f * Spring.kick(dt, hz = 9.0, decayMs = 140.0).toFloat() else 0f
        val warn = if (!good && last != null) exp(-dt / 260.0).toFloat() else 0f
        val c = l.nodes[node] + Offset(shake, 0f)
        val r = u * 0.78f * (1f + pop)
        drawGlow(tint.color, c, r * 2.1f, 0.2f + 0.25f * pop.coerceAtLeast(0f) * 4f)
        drawCircle(Hollow, r, c)
        val ring = lerp(tint.color, Amber, warn)
        val open = 64f
        val face = Math.toDegrees(l.arrival[net.nodes[node].inEdge].toDouble()).toFloat() + 180f
        drawArc(
            ring, face + open / 2, 360f - open, false,
            Offset(c.x - r + u * 0.05f, c.y - r + u * 0.05f), Size(2 * r - u * 0.1f, 2 * r - u * 0.1f),
            style = Stroke(u * 0.1f, cap = StrokeCap.Round),
        )
        drawMark(tint.mark, c, r * 0.5f, tint.color)
        if (good && dt < 900) {
            // Ring-Welle beim Schlucken
            val e = (1 - exp(-dt / 200.0)).toFloat()
            drawCircle(tint.color.copy(alpha = 0.7f * (1 - e)), r * (1f + 1.2f * e), c, style = Stroke(u * 0.09f * (1 - e) + 1f))
        }
    }

    // Portal: Lichtring, der bei jeder Abfahrt pulst
    val pc = l.nodes[net.portal]
    val since = now - s.lastSpawnAt
    drawGlow(accent, pc, u * 1.7f, 0.3f)
    drawCircle(Hollow, u * 0.66f, pc)
    drawCircle(accent.copy(alpha = 0.3f), u * 0.34f, pc)
    drawCircle(accent, u * 0.58f, pc, style = Stroke(u * 0.11f))
    if (since < 800) {
        val e = (1 - exp(-since / 170.0)).toFloat()
        drawCircle(accent.copy(alpha = 0.6f * (1 - e)), u * 0.58f * (1f + 0.9f * e), pc, style = Stroke(u * 0.07f))
    }

    // Weichen: Knopf mit Pfeil zum gestellten Abgang; Umlegen schnappt mit Ueberschwinger
    for (n in net.switches) {
        val node = net.nodes[n]
        val c = l.nodes[n]
        val onEdge = net.next(n, s.on[node.slot])
        val offEdge = net.next(n, !s.on[node.slot])
        val dt = now - s.flippedAt[node.slot]
        val p = Spring.step(dt).toFloat()
        val a = angleLerp(l.heading[offEdge], l.heading[onEdge], p)
        val kick = Spring.kick(dt, hz = 4.0, decayMs = 120.0).toFloat()
        val r = u * 0.5f * (1f + 0.12f * kick)
        drawGlow(accent, c, u * 1.1f, 0.14f + 0.2f * exp(-dt / 250.0).toFloat())
        drawCircle(Knob, r, c)
        drawCircle(KnobRim, r - u * 0.03f, c, style = Stroke(u * 0.06f))
        val d = Offset(cos(a), sin(a))
        val q = Offset(-d.y, d.x)
        drawLine(accent, c - d * (u * 0.22f), c + d * (u * 0.08f), u * 0.12f, StrokeCap.Round)
        drawPath(
            Path().apply {
                val tip = c + d * (u * 0.36f)
                val base = c + d * (u * 0.04f)
                moveTo(tip.x, tip.y)
                lineTo(base.x + q.x * u * 0.19f, base.y + q.y * u * 0.19f)
                lineTo(base.x - q.x * u * 0.19f, base.y - q.y * u * 0.19f)
                close()
            },
            accent,
        )
    }

    // Perlen: Lichtschweif, Glanz, kurzes Federn nach einer Weiche
    for (b in s.beads) {
        val tint = SwitchPalette[s.colors[b.target]]
        val track = net.edges[b.edge].track
        val at = s.progress(b)
        val prev = if (b.prevEdge >= 0) net.edges[b.prevEdge].track else null
        fun back(d: Double): Offset? = when {
            at - d >= 0 -> l.map(track.at(at - d))
            prev != null && prev.length - (d - at) >= 0 -> l.map(prev.at(prev.length - (d - at)))
            else -> null
        }
        val pos = l.map(track.at(at))
        var last = pos
        for (k in 1..9) {
            val p = back(k * 0.16) ?: break
            val f = 1f - k / 10f
            drawLine(tint.color.copy(alpha = 0.5f * f), last, p, u * 0.34f * f, StrokeCap.Round)
            last = p
        }
        val turned = if (b.prevEdge >= 0) Spring.kick(now - b.enteredAt, hz = 7.0, decayMs = 110.0).toFloat() else 0f
        val r = u * 0.4f * (1f + 0.22f * turned)
        drawGlow(tint.color, pos, u * 1.25f, 0.5f)
        drawMark(tint.mark, pos, r, tint.color)
        drawCircle(Color.White.copy(alpha = 0.55f), r * 0.2f, pos + Offset(-r * 0.32f, -r * 0.36f))
    }

    // Ankuenfte: richtig = Perle schrumpft ins Tor, falsch = verpufft grau
    for (a in s.arrivals) {
        val dt = now - a.at
        if (dt < 0) continue
        val c = l.nodes[net.gates[a.gate]]
        val tint = SwitchPalette[s.colors[a.target]]
        if (a.correct) {
            val k = exp(-dt / 80.0).toFloat()
            if (k > 0.05f) drawMark(tint.mark, c, u * 0.4f * k, tint.color.copy(alpha = k))
        } else {
            val e = (1 - exp(-dt / 140.0)).toFloat()
            val fade = (1 - e).coerceIn(0f, 1f)
            drawCircle(Smoke.copy(alpha = 0.45f * fade), u * (0.4f + 0.7f * e), c)
            drawMark(tint.mark, c, u * 0.4f * fade, Smoke.copy(alpha = fade))
            for (k in 0 until 6) {
                val ang = k * PI / 3 + a.id * 0.7
                val dist = u * (0.5f + 1.1f * e)
                val p = c + Offset(cos(ang).toFloat() * dist, sin(ang).toFloat() * dist)
                drawCircle(Smoke.copy(alpha = 0.8f * fade), u * 0.09f * (0.4f + fade), p)
            }
        }
    }
}

// Kuerzester Weg zwischen zwei Winkeln, t darf ueberschwingen
private fun angleLerp(a: Float, b: Float, t: Float): Float {
    var d = b - a
    while (d > PI) d -= (2 * PI).toFloat()
    while (d < -PI) d += (2 * PI).toFloat()
    return a + d * t
}
