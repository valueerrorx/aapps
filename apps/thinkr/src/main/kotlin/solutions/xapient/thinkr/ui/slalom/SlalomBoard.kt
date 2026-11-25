package solutions.xapient.thinkr.ui.slalom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
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
import solutions.xapient.thinkr.domain.slalom.Kind
import solutions.xapient.thinkr.domain.slalom.Obstacle
import solutions.xapient.thinkr.domain.slalom.SlalomRules
import solutions.xapient.thinkr.domain.slalom.SlalomState
import solutions.xapient.thinkr.ui.Amber
import solutions.xapient.thinkr.ui.CountOverlay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val SkyTop = Color(0xFF0A0E1C)
private val SkyLow = Color(0xFF1D1636)
private val Road = Color(0xFF141A2B)
private val RoadNear = Color(0xFF1A1F35)
private val WallA = Color(0xFF2A2150)
private val WallB = Color(0xFF211A41)
private val WallEdge = Color(0xFF8E7BD6)

// Kristalle (kuehl, klar vom Gleiter in Akzent getrennt) und Felsen
private val CrystalLight = Color(0xFFBFEFFF)
private val CrystalMid = Color(0xFF56B4E9)
private val CrystalDark = Color(0xFF2B5C8A)
private val Rock = Color(0xFF3B3452)
private val RockRim = Color(0xFFCFC3F0)

// Kamera: Abstand, ab dem die Perspektive halbiert, Sichtweite und Wandabschnitt (Weltmass)
private const val CAM = 7.0
private const val DEPTH = 70.0
private const val SEG = 5.0
private const val WALL_H = 3.2

// Slalom-Brett: Pseudo-3D aus flachen Ebenen; Tipp links/rechts wechselt die Spur; alles haengt an der Spielzeit
@Composable
fun SlalomBoard(s: SlalomState, live: Boolean, count: Int, onSteer: (Int) -> Unit) {
    val c = Aapps.colors
    val steer by rememberUpdatedState(onSteer)
    val desc = stringResource(R.string.slalom_board, s.lanes, s.lane + 1, s.lives)
    val left = stringResource(R.string.dir_left)
    val right = stringResource(R.string.dir_right)
    val stars = remember { Random(83).let { r -> List(70) { floatArrayOf(r.nextFloat(), r.nextFloat(), r.nextFloat()) } } }
    Box(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .semantics {
                    contentDescription = desc
                    if (live) customActions = listOf(
                        CustomAccessibilityAction(left) { steer(-1); true },
                        CustomAccessibilityAction(right) { steer(1); true },
                    )
                }
                .pointerInput(live) {
                    if (!live) return@pointerInput
                    // loest beim Aufsetzen aus: linke bzw. rechte Haelfte
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        steer(if (down.position.x < size.width / 2f) -1 else 1)
                    }
                },
        ) {
            drawCanyon(s, stars, c.accent)
        }
        if (count > 0) CountOverlay(stringResource(R.string.slalom_prompt), count)
    }
}

// Projektion: Abstand d vor dem Gleiter -> Bildschirm
private class Cam(w: Float, h: Float, lanes: Int, maxLane: Float) {
    val cx = w / 2
    val horizon = h * 0.34f
    val ground = h * 0.86f
    val lane = min(w * 0.8f / lanes, maxLane)
    val mid = (lanes - 1) / 2f
    val half = lanes / 2f

    fun k(d: Double): Float = (CAM / (d + CAM)).toFloat()
    fun y(d: Double): Float = horizon + (ground - horizon) * k(d)
    fun x(laneF: Double, d: Double): Float = cx + ((laneF - mid) * lane * k(d)).toFloat()
    fun edge(d: Double): Float = half * lane * k(d)
}

private fun DrawScope.drawCanyon(s: SlalomState, stars: List<FloatArray>, accent: Color) {
    val now = s.round.now.toDouble()
    // Rueckstoss beim Zusammenstoss
    val shake = if (s.crashAt >= 0) Spring.kick(now - s.crashAt, hz = 11.0, decayMs = 90.0).toFloat() * 9.dp.toPx() else 0f
    val cam = Cam(size.width, size.height, s.lanes, 120.dp.toPx())
    val fast = ((s.speed / SlalomRules.BASE_SPEED - 1.15) / 0.6).coerceIn(0.0, 1.0).toFloat()

    // Himmel mit Sternen und fahlem Mond ueber dem Schluchtende
    drawRect(Brush.verticalGradient(listOf(SkyTop, SkyLow), endY = cam.horizon))
    stars.forEach { (x, y, z) ->
        drawCircle(Color.White.copy(alpha = 0.15f + 0.4f * z), (0.5f + 0.9f * z).dp.toPx(), Offset(x * size.width, y * cam.horizon * 0.95f))
    }
    val moon = Offset(cam.cx + size.width * 0.18f, cam.horizon * 0.42f)
    drawGlow(Color(0xFFE6DDFB), moon, size.width * 0.22f, 0.18f)
    drawCircle(Color(0xFFE9E2FA).copy(alpha = 0.85f), size.width * 0.045f, moon)
    drawGlow(accent, Offset(cam.cx, cam.horizon), size.width * 0.5f, 0.12f)

    translate(left = shake) {
        // Boden: Strasse aus Licht zwischen den Waenden
        val far = DEPTH
        val road = Path().apply {
            moveTo(cam.cx - cam.edge(far), cam.y(far))
            lineTo(cam.cx + cam.edge(far), cam.y(far))
            lineTo(cam.cx + cam.edge(-1.0) * 1.05f, size.height + 4f)
            lineTo(cam.cx - cam.edge(-1.0) * 1.05f, size.height + 4f)
            close()
        }
        drawPath(road, Brush.verticalGradient(listOf(Road, RoadNear), startY = cam.horizon, endY = size.height))

        // Querlinien und Spurstriche wandern mit der Welt
        val base = floor(s.z / SEG) * SEG
        for (i in 0..(DEPTH / SEG).toInt() + 1) {
            val d = base + i * SEG - s.z
            if (d < -1 || d > DEPTH) continue
            val a = fade(d) * 0.22f
            drawLine(accent.copy(alpha = a), Offset(cam.cx - cam.edge(d), cam.y(d)), Offset(cam.cx + cam.edge(d), cam.y(d)), 1.dp.toPx())
        }
        for (k in 1 until s.lanes) {
            val lx = k - 0.5
            var i = 0
            while (true) {
                val d0 = base + i * 6.0 - s.z
                if (d0 > DEPTH) break
                val d1 = d0 + 2.4
                i++
                if (d1 < -1) continue
                val a = fade(d0) * 0.5f
                drawLine(
                    Color.White.copy(alpha = a), Offset(cam.x(lx, d0), cam.y(d0)), Offset(cam.x(lx, d1), cam.y(d1)),
                    (2.4f * cam.k(d0)).dp.toPx().coerceAtLeast(0.6f), StrokeCap.Round,
                )
            }
        }

        // Schluchtwaende: Kristallsplitter je Abschnitt, von hinten nach vorn
        for (i in (DEPTH / SEG).toInt() + 1 downTo 0) {
            val zA = base + i * SEG
            val d1 = zA - s.z
            val d2 = d1 + SEG
            if (d2 < -2) continue
            val seed = floor(zA / SEG).toInt()
            for (side in listOf(-1, 1)) drawWall(cam, d1, d2, side, seed, fast)
        }

        // Kanten der Strasse leuchten
        for (side in listOf(-1f, 1f)) {
            val a = Offset(cam.cx + side * cam.edge(far), cam.y(far))
            val b = Offset(cam.cx + side * cam.edge(-1.0), cam.y(-1.0))
            drawLine(accent.copy(alpha = 0.25f), a, b, 6.dp.toPx(), StrokeCap.Round)
            drawLine(accent.copy(alpha = 0.8f), a, b, 1.5.dp.toPx(), StrokeCap.Round)
        }

        // Hindernisse von hinten nach vorn
        for (row in s.rows.sortedByDescending { it.z }) {
            val d = row.z - s.z
            if (d < -3 || d > DEPTH) continue
            for (o in row.obstacles) drawObstacle(cam, o, d, row.no)
        }

        drawGlider(s, cam, now, accent)
    }

    // Leben: drei kleine Gleiter oben rechts
    for (i in 0 until SlalomRules.LIVES) {
        val c = Offset(size.width - 20.dp.toPx() - i * 22.dp.toPx(), 22.dp.toPx())
        val p = delta(c, 7.dp.toPx(), 8.dp.toPx())
        if (i < s.lives) drawPath(p, accent) else drawPath(p, Amber.copy(alpha = 0.8f), style = Stroke(1.4.dp.toPx()))
    }
}

private fun fade(d: Double): Float = ((DEPTH - d) / 18.0).coerceIn(0.0, 1.0).toFloat()

// Ein Wandabschnitt: schraeger Splitter mit gezackter Oberkante
private fun DrawScope.drawWall(cam: Cam, d1: Double, d2: Double, side: Int, seed: Int, fast: Float) {
    val r = Random(seed * 31 + side)
    val n1 = cam.k(d1.coerceAtLeast(-1.0))
    val n2 = cam.k(d2)
    val e1 = cam.cx + side * cam.edge(d1.coerceAtLeast(-1.0))
    val e2 = cam.cx + side * cam.edge(d2)
    val y1 = cam.y(d1.coerceAtLeast(-1.0))
    val y2 = cam.y(d2)
    val h1 = (WALL_H + r.nextDouble() * 1.6).toFloat() * cam.lane * n1
    val h2 = (WALL_H + r.nextDouble() * 1.6).toFloat() * cam.lane * n2
    val out1 = side * cam.lane * 2.2f * n1
    val out2 = side * cam.lane * 2.2f * n2
    val path = Path().apply {
        moveTo(e1, y1)
        lineTo(e2, y2)
        lineTo(e2 + out2 * 0.35f, y2 - h2)
        lineTo((e1 + e2) / 2 + (out1 + out2) * 0.25f, (y1 + y2) / 2 - (h1 + h2) / 2 * 1.15f)
        lineTo(e1 + out1 * 0.35f, y1 - h1)
        lineTo(e1 + out1 * 3f, y1 - h1 * 0.6f)
        lineTo(e1 + out1 * 3f, y1)
        close()
    }
    val a = fade(d1)
    val col = lerp(Color(0xFF151A2B), if (seed % 2 == 0) WallA else WallB, a)
    drawPath(path, col)
    // Kristallkante oben, im Tempo mit Schlieren
    drawLine(WallEdge.copy(alpha = 0.55f * a), Offset(e1 + out1 * 0.35f, y1 - h1), Offset(e2 + out2 * 0.35f, y2 - h2), 1.2.dp.toPx())
    drawLine(WallEdge.copy(alpha = 0.3f * a), Offset(e1, y1), Offset(e1 + out1 * 0.35f, y1 - h1), 1.dp.toPx())
    if (fast > 0f && d1 < 25) {
        val ys = y1 - h1 * (0.3f + 0.4f * r.nextFloat())
        drawLine(Color.White.copy(alpha = 0.18f * fast * a), Offset(e1 + out1 * 0.5f, ys), Offset(e2 + out2 * 0.5f, ys - (h2 - h1) * 0.2f), 1.dp.toPx())
    }
}

private fun DrawScope.drawObstacle(cam: Cam, o: Obstacle, d: Double, rowNo: Int) {
    val a = fade(d)
    if (a <= 0.01f) return
    val lf = o.laneAt(d)
    val x = cam.x(lf, d)
    val y = cam.y(d)
    val u = cam.lane * cam.k(d)
    // Schatten am Boden
    drawOval(Color.Black.copy(alpha = 0.35f * a), Offset(x - u * 0.38f, y - u * 0.08f), Size(u * 0.76f, u * 0.16f))
    when (o.kind) {
        Kind.PILLAR -> drawPillar(Offset(x, y), u, a)
        Kind.BOULDER -> drawBoulder(Offset(x, y), u, a, rowNo * 7 + o.lane)
    }
}

// Kristallsaeule: sechseckiges Prisma, zwei sichtbare Flaechen und Kappe
private fun DrawScope.drawPillar(base: Offset, u: Float, a: Float) {
    val w = u * 0.3f
    val h = u * 1.35f
    val top = base.y - h
    drawGlow(CrystalMid, Offset(base.x, base.y - h * 0.4f), u * 0.9f, 0.22f * a)
    val left = Path().apply {
        moveTo(base.x - w, base.y - w * 0.25f); lineTo(base.x, base.y); lineTo(base.x, top + w * 0.25f); lineTo(base.x - w, top); close()
    }
    val right = Path().apply {
        moveTo(base.x, base.y); lineTo(base.x + w, base.y - w * 0.25f); lineTo(base.x + w, top); lineTo(base.x, top + w * 0.25f); close()
    }
    drawPath(left, Brush.verticalGradient(listOf(CrystalMid.copy(alpha = a), CrystalDark.copy(alpha = a)), startY = top, endY = base.y))
    drawPath(right, Brush.verticalGradient(listOf(CrystalLight.copy(alpha = a), CrystalMid.copy(alpha = a)), startY = top, endY = base.y))
    // Spitze
    val tip = Path().apply {
        moveTo(base.x - w, top); lineTo(base.x, top - w * 1.1f); lineTo(base.x + w, top); lineTo(base.x, top + w * 0.25f); close()
    }
    drawPath(tip, CrystalLight.copy(alpha = a))
    drawLine(Color.White.copy(alpha = 0.7f * a), Offset(base.x, base.y), Offset(base.x, top + w * 0.25f), (u * 0.02f).coerceAtLeast(0.8f))
}

// Felsbrocken: unregelmaessig rund, dunkel mit hellem Rand oben
private fun DrawScope.drawBoulder(base: Offset, u: Float, a: Float, seed: Int) {
    val r = Random(seed)
    val rad = u * 0.38f
    val c = Offset(base.x, base.y - rad * 0.9f)
    val path = Path()
    val n = 9
    for (i in 0..n) {
        val ang = 2 * PI * i / n
        val rr = rad * (0.82f + 0.3f * r.nextFloat())
        val p = c + Offset(cos(ang).toFloat() * rr, sin(ang).toFloat() * rr * 0.85f)
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, Brush.radialGradient(listOf(Color(0xFF5A4F78).copy(alpha = a), Rock.copy(alpha = a)), c - Offset(rad * 0.3f, rad * 0.4f), rad * 1.4f))
    drawPath(path, RockRim.copy(alpha = 0.45f * a), style = Stroke((u * 0.025f).coerceAtLeast(0.8f), join = StrokeJoin.Round))
}

// Gleiter: Deltafluegel mit Leuchtschweif, neigt sich beim Spurwechsel mit Feder
private fun DrawScope.drawGlider(s: SlalomState, cam: Cam, now: Double, accent: Color) {
    val dt = now - s.laneAt
    val f = Spring.step(dt, hz = 4.2, damping = 0.72).toFloat()
    val laneF = s.laneFrom + (s.lane - s.laneFrom) * f
    val x = cam.x(laneF.toDouble(), 0.0)
    val y = cam.ground
    val dir = (s.lane - s.laneFrom).coerceIn(-1, 1)
    val bank = 26f * dir * Spring.kick(dt, hz = 2.2, decayMs = 220.0).toFloat()
    val u = cam.lane
    val blink = now < s.safeUntil && ((now / 110).toInt() % 2 == 0)
    val alpha = if (blink) 0.35f else 1f
    val c = Offset(x, y)
    // Schweif aus den Fluegelspitzen
    val tip = u * 0.27f
    for (side in listOf(-1f, 1f)) {
        val p = Offset(x + side * tip, y + u * 0.08f)
        drawLine(
            Brush.verticalGradient(listOf(accent.copy(alpha = 0.6f * alpha), accent.copy(alpha = 0f)), startY = p.y, endY = size.height),
            p, Offset(p.x - side * tip * 0.15f + (s.laneFrom - s.lane) * u * 0.25f * (1 - f), size.height), 2.dp.toPx(),
        )
    }
    drawGlow(accent, c + Offset(0f, u * 0.08f), u * 0.55f, 0.4f * alpha)
    rotate(bank, c) {
        val body = delta(c, u * 0.3f, u * 0.36f)
        drawPath(body, Brush.verticalGradient(listOf(Color.White.copy(alpha = alpha), accent.copy(alpha = alpha)), startY = y - u * 0.3f, endY = y + u * 0.12f))
        drawPath(body, Color.White.copy(alpha = 0.6f * alpha), style = Stroke(1.2.dp.toPx(), join = StrokeJoin.Round))
        drawCircle(Color(0xFF1A1430).copy(alpha = alpha), u * 0.045f, c - Offset(0f, u * 0.08f))
    }
    // Zusammenstoss: Bernstein-Ring; knapp vorbei: kurzer Akzent-Ring
    if (s.crashAt >= 0 && now - s.crashAt < 500) {
        val g = ((now - s.crashAt) / 500).toFloat()
        drawCircle(Amber.copy(alpha = 1f - g), u * (0.3f + 0.6f * g), c, style = Stroke(3.dp.toPx() * (1f - g) + 0.5f))
    }
    if (s.nearAt >= 0 && now - s.nearAt < 400) {
        val g = ((now - s.nearAt) / 400).toFloat()
        drawCircle(accent.copy(alpha = 0.8f * (1f - g)), u * (0.32f + 0.3f * g), c, style = Stroke(1.6.dp.toPx()))
    }
}

// Deltaform: Spitze vorn (oben), Kerbe hinten
private fun delta(c: Offset, halfW: Float, len: Float): Path = Path().apply {
    moveTo(c.x, c.y - len * 0.75f)
    lineTo(c.x + halfW, c.y + len * 0.3f)
    lineTo(c.x, c.y + len * 0.1f)
    lineTo(c.x - halfW, c.y + len * 0.3f)
    close()
}
