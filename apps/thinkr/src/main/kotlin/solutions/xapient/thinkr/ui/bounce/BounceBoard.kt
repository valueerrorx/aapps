package solutions.xapient.thinkr.ui.bounce

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import solutions.xapient.thinkr.domain.bounce.BouncePhase
import solutions.xapient.thinkr.domain.bounce.BounceRules
import solutions.xapient.thinkr.domain.bounce.BounceState
import solutions.xapient.thinkr.domain.bounce.Cell
import solutions.xapient.thinkr.domain.bounce.Edge
import solutions.xapient.thinkr.domain.bounce.Optics
import solutions.xapient.thinkr.domain.bounce.Port
import solutions.xapient.thinkr.domain.bounce.Prism
import solutions.xapient.thinkr.ui.Amber
import solutions.xapient.thinkr.ui.CountOverlay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

private val Deep = Color(0xFF131A2B)
private val DeepViolet = Color(0xFF1C1430)
private val GlassTop = Color(0xFFFFFFFF)
private val GlassTint = Color(0xFFF6C8F0)

// Regenbogen-Kanten der Prismen (Okabe-Ito: Himmelblau, Gelb)
private val EdgeCool = Color(0xFF56B4E9)
private val EdgeWarm = Color(0xFFF0E442)

// Randlaschen: Tiefe in Zellen
private const val TAB = 0.8f

// Bande-Brett: Feld mit Randlaschen; alles haengt an der Spielzeit und steht in der Pause
@Composable
fun BounceBoard(s: BounceState, live: Boolean, count: Int, onTap: (Port) -> Unit) {
    val c = Aapps.colors
    val tap by rememberUpdatedState(onTap)
    val sh = s.shot
    val phase = s.phase()
    val desc = stringResource(R.string.bounce_board, sh.side, sh.side)
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
            if (count == 0) {
                val label = when (phase) {
                    BouncePhase.LEAD, BouncePhase.SHOW -> stringResource(R.string.memorize)
                    else -> stringResource(R.string.bounce_ask)
                }
                val strong = phase == BouncePhase.SHOW || phase == BouncePhase.ANSWER
                BasicText(
                    label.uppercase(),
                    style = Aapps.type.label.copy(color = if (strong) c.accent else c.fgFaint, fontSize = Aapps.type.label.fontSize * 1.2f),
                    maxLines = 1,
                )
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
            val side = min(min(maxWidth, maxHeight), 540.dp)
            Box(Modifier.size(side)) {
                Canvas(
                    Modifier
                        .size(side)
                        .semantics { contentDescription = desc }
                        .pointerInput(live, sh.no, sh.side) {
                            if (!live) return@pointerInput
                            // loest beim Aufsetzen aus; Randlasche unter dem Finger, knapp im Feld zaehlt auch
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val cell = size.width / (sh.side + 2 * TAB)
                                val gx = down.position.x / cell - TAB
                                val gy = down.position.y / cell - TAB
                                portAt(gx, gy, sh.side)?.let(tap)
                            }
                        },
                ) {
                    drawBounce(s, phase, c.accent, c.fgFaint, c.fgMuted)
                }
                if (count > 0) CountOverlay(stringResource(R.string.bounce_prompt), count)
            }
        }
    }
}

// Randfeld zur Tipp-Stelle (Zellkoordinaten des Felds); Ecken zaehlen nicht
internal fun portAt(gx: Float, gy: Float, side: Int): Port? {
    val inX = gx >= 0f && gx < side
    val inY = gy >= 0f && gy < side
    val reach = 0.4f
    return when {
        gy < 0f && inX && gy > -TAB - 0.5f -> Port(Edge.TOP, floor(gx).toInt())
        gy >= side && inX && gy < side + TAB + 0.5f -> Port(Edge.BOTTOM, floor(gx).toInt())
        gx < 0f && inY && gx > -TAB - 0.5f -> Port(Edge.LEFT, floor(gy).toInt())
        gx >= side && inY && gx < side + TAB + 0.5f -> Port(Edge.RIGHT, floor(gy).toInt())
        inX && inY -> {
            // knapp im Feld: naechste Kante, wenn nah genug
            val d = listOf(gy to Edge.TOP, side - gy to Edge.BOTTOM, gx to Edge.LEFT, side - gx to Edge.RIGHT).minBy { it.first }
            if (d.first >= reach) null else when (d.second) {
                Edge.TOP, Edge.BOTTOM -> Port(d.second, floor(gx).toInt())
                else -> Port(d.second, floor(gy).toInt())
            }
        }
        else -> null
    }
}

private fun DrawScope.drawBounce(s: BounceState, phase: BouncePhase, accent: Color, faint: Color, muted: Color) {
    val sh = s.shot
    val n = sh.side
    val now = s.round.now.toDouble()
    val cell = size.width / (n + 2 * TAB)
    val o = Offset(cell * TAB, cell * TAB)
    fun at(x: Float, y: Float) = o + Offset((x + 0.5f) * cell, (y + 0.5f) * cell)
    fun at(c: Cell) = at(c.x.toFloat(), c.y.toFloat())

    // Feld: dunkles Glas, feines Raster
    val field = Size(n * cell, n * cell)
    val cr = CornerRadius(cell * 0.35f, cell * 0.35f)
    drawRoundRect(Brush.linearGradient(listOf(Deep, DeepViolet), o, o + Offset(field.width, field.height)), o, field, cr)
    drawRoundRect(accent.copy(alpha = 0.22f), o, field, cr, style = Stroke(1.2.dp.toPx()))
    for (k in 1 until n) {
        val p = k * cell
        drawLine(faint.copy(alpha = 0.18f), o + Offset(p, cell * 0.15f), o + Offset(p, field.height - cell * 0.15f), 1.dp.toPx())
        drawLine(faint.copy(alpha = 0.18f), o + Offset(cell * 0.15f, p), o + Offset(field.width - cell * 0.15f, p), 1.dp.toPx())
    }

    val asking = phase == BouncePhase.ANSWER
    val showEntry = phase >= BouncePhase.ANSWER
    val revealed = phase == BouncePhase.HOLD

    // Randlaschen: kleine Pfeile nach aussen; beim Abfragen heller
    for (p in Optics.ports(n)) {
        if (showEntry && p == sh.entry) continue
        val pos = tabCenter(p, n, cell, o)
        val chosen = sh.said == p
        val exit = revealed && p == sh.trace.exit
        val color = when {
            chosen && revealed && !sh.correct -> Amber
            chosen || exit -> accent
            asking -> muted.copy(alpha = 0.75f)
            else -> faint.copy(alpha = 0.45f)
        }
        val pop = if (exit) 1f + 0.35f * Spring.kick(now - s.doneAt, hz = 4.0, decayMs = 160.0).toFloat() else 1f
        if (exit) drawGlow(accent, pos, cell * 0.9f, 0.35f)
        drawChevron(pos, p.edge, cell * 0.2f * pop, color, (if (chosen || exit) 2.4f else 1.6f).dp.toPx())
        if (chosen && revealed && !sh.correct) drawCross(pos, cell * 0.2f, Amber, 2.2.dp.toPx())
    }

    // Prismen zeigen: einblenden mit Feder, kurz vor dem Ausblenden verloeschen
    if (phase == BouncePhase.SHOW) {
        val showAt = sh.startAt + BounceRules.LEAD_MS
        val a = min(((now - showAt) / 160.0).coerceIn(0.0, 1.0), ((s.hideAt - now) / 180.0).coerceIn(0.0, 1.0)).toFloat()
        sh.prisms.entries.forEachIndexed { k, (i, p) ->
            val pop = Spring.step(now - showAt - k * 25.0, hz = 3.2, damping = 0.55).toFloat()
            drawPrism(at((i % n).toFloat(), (i / n).toFloat()), p, cell * pop.coerceAtLeast(0f), accent, a)
        }
    }

    if (!showEntry) return
    val start = at(sh.trace.corners.first())
    if (!sh.answered) {
        // Licht wartet am Eingang und atmet leicht
        val breath = 1f + 0.08f * sin(now / 1000.0 * 2 * PI * 0.9).toFloat()
        drawOrb(start, cell * 0.2f * breath, accent, 1f)
        drawChevron(start + inward(sh.entry.edge) * (cell * 0.52f), opposite(sh.entry.edge), cell * 0.14f, accent, 1.8.dp.toPx())
        return
    }

    // Aufloesung: getroffene Prismen leuchten beim Abprall auf, danach alle gedimmt
    val dt = now - sh.saidAt
    sh.prisms.forEach { (i, p) ->
        val k = sh.trace.hits.indexOf(i)
        val hitAt = if (k >= 0) sh.cornerMs(k + 1) else Double.MAX_VALUE
        val center = at((i % n).toFloat(), (i / n).toFloat())
        when {
            dt >= hitAt -> {
                val pop = 1f + 0.3f * Spring.kick(dt - hitAt, hz = 5.0, decayMs = 120.0).toFloat()
                drawPrism(center, p, cell * pop, accent, if (revealed) 0.85f else 1f)
            }
            revealed -> drawPrism(center, p, cell, accent, 0.28f * ((now - s.doneAt) / 220.0).coerceIn(0.0, 1.0).toFloat())
        }
    }

    // Weg bis zur Kugel: gestrichelt in Akzent; richtig = am Ende hell
    val dist = (dt / 1000.0 * BounceRules.SPEED).toFloat()
    val path = Path()
    var left = dist
    var ball = start
    path.moveTo(start.x, start.y)
    var segStart = 0
    for (k in 1 until sh.trace.corners.size) {
        val a = at(sh.trace.corners[k - 1])
        val b = at(sh.trace.corners[k])
        val len = (abs(sh.trace.corners[k].x - sh.trace.corners[k - 1].x) + abs(sh.trace.corners[k].y - sh.trace.corners[k - 1].y)).toFloat()
        if (left >= len) {
            path.lineTo(b.x, b.y)
            ball = b
            left -= len
            segStart = k
        } else {
            ball = a + (b - a) * (left / len)
            path.lineTo(ball.x, ball.y)
            break
        }
    }
    val glowPath = revealed && sh.correct
    val dash = PathEffect.dashPathEffect(floatArrayOf(cell * 0.16f, cell * 0.12f), -(dist * cell * 0.5f))
    if (glowPath) drawPath(path, accent.copy(alpha = 0.25f), style = Stroke(cell * 0.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(
        path, accent.copy(alpha = if (glowPath) 0.95f else 0.6f),
        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash),
    )

    // Kugel: drueckt sich beim Abprall kurz zusammen, verblasst draussen
    val lastHit = if (segStart in 1..sh.trace.hits.size) sh.cornerMs(segStart) else -1.0
    val squash = if (lastHit >= 0) Spring.kick(dt - lastHit, hz = 7.0, decayMs = 90.0).toFloat() else 0f
    val out = ((now - s.doneAt) / 260.0).coerceIn(0.0, 1.0).toFloat()
    drawOrb(ball, cell * 0.2f * (1f + 0.25f * abs(squash)), accent, 1f - out)
}

// Mitte der Randlasche vor dem Randfeld
private fun tabCenter(p: Port, n: Int, cell: Float, o: Offset): Offset {
    val c = Optics.outside(p, n)
    val pull = (1f - TAB) / 2f
    val shift = when (p.edge) {
        Edge.TOP -> Offset(0f, pull)
        Edge.BOTTOM -> Offset(0f, -pull)
        Edge.LEFT -> Offset(pull, 0f)
        Edge.RIGHT -> Offset(-pull, 0f)
    }
    return o + Offset((c.x + 0.5f + shift.x) * cell, (c.y + 0.5f + shift.y) * cell)
}

private fun inward(e: Edge) = Offset(e.dx.toFloat(), e.dy.toFloat())

private fun opposite(e: Edge) = when (e) {
    Edge.TOP -> Edge.BOTTOM
    Edge.BOTTOM -> Edge.TOP
    Edge.LEFT -> Edge.RIGHT
    Edge.RIGHT -> Edge.LEFT
}

// Pfeil, der von der Kante nach aussen zeigt
private fun DrawScope.drawChevron(c: Offset, e: Edge, r: Float, color: Color, w: Float) {
    val out = -inward(e)
    val n = Offset(-out.y, out.x)
    val tip = c + out * (r * 0.6f)
    drawPath(
        Path().apply {
            moveTo(tip.x - out.x * r + n.x * r, tip.y - out.y * r + n.y * r)
            lineTo(tip.x, tip.y)
            lineTo(tip.x - out.x * r - n.x * r, tip.y - out.y * r - n.y * r)
        },
        color, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

// Glas-Prisma: diagonaler Balken mit Glanz und zwei farbigen Kanten
private fun DrawScope.drawPrism(c: Offset, p: Prism, cell: Float, accent: Color, alpha: Float) {
    if (alpha <= 0.01f || cell <= 0f) return
    val h = cell * 0.34f
    val d = if (p == Prism.SLASH) Offset(h, -h) else Offset(h, h)
    val a = c - d
    val b = c + d
    val len = (b - a).getDistance()
    val nrm = Offset(-(b - a).y / len, (b - a).x / len)
    drawGlow(accent, c, cell * 0.75f, 0.22f * alpha)
    val off = nrm * (cell * 0.075f)
    drawLine(EdgeCool.copy(alpha = 0.75f * alpha), a + off, b + off, 1.4.dp.toPx(), StrokeCap.Round)
    drawLine(EdgeWarm.copy(alpha = 0.6f * alpha), a - off, b - off, 1.4.dp.toPx(), StrokeCap.Round)
    drawLine(
        Brush.linearGradient(listOf(GlassTop.copy(alpha = alpha), GlassTint.copy(alpha = alpha), GlassTop.copy(alpha = 0.85f * alpha)), a, b),
        a, b, cell * 0.11f, StrokeCap.Round,
    )
}

// Lichtkugel mit weichem Schein
private fun DrawScope.drawOrb(c: Offset, r: Float, accent: Color, alpha: Float) {
    if (alpha <= 0.01f) return
    drawGlow(accent, c, r * 4f, 0.45f * alpha)
    drawCircle(Brush.radialGradient(listOf(GlassTop.copy(alpha = alpha), GlassTint.copy(alpha = alpha)), c, r), r, c)
    drawCircle(Color.White.copy(alpha = 0.9f * alpha), r * 0.35f, c - Offset(r * 0.25f, r * 0.25f))
}

private fun DrawScope.drawCross(c: Offset, r: Float, color: Color, w: Float) {
    val k = r * 1.4f
    val m = c + Offset(k, -k)
    drawLine(color, m + Offset(-r * 0.5f, -r * 0.5f), m + Offset(r * 0.5f, r * 0.5f), w, StrokeCap.Round)
    drawLine(color, m + Offset(r * 0.5f, -r * 0.5f), m + Offset(-r * 0.5f, r * 0.5f), w, StrokeCap.Round)
}
