package solutions.xapient.thinkr.ui.drops

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.SpaceGrotesk
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.thinkr.R
import solutions.xapient.thinkr.domain.Spring
import solutions.xapient.thinkr.domain.drops.Burst
import solutions.xapient.thinkr.domain.drops.Drop
import solutions.xapient.thinkr.domain.drops.DropsKeys
import solutions.xapient.thinkr.domain.drops.DropsRules
import solutions.xapient.thinkr.domain.drops.DropsState
import solutions.xapient.thinkr.ui.Amber
import solutions.xapient.thinkr.ui.CountOverlay
import solutions.xapient.thinkr.ui.ThinkrIcons
import solutions.xapient.thinkr.ui.drawGlass
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

private val SkyTop = Color(0xFF0F1524)
private val SkyLow = Color(0xFF1B1630)
private val WaterTop = Color(0xFF3A2C5C)
private val WaterDeep = Color(0xFF151A2E)
private val DropRim = Color(0xFFF9D7F5)
private val Gold = Color(0xFFF7E08A)

// Wasser: Grundpegel und Anstieg je Aufschlag (Anteil der Himmelshoehe)
private const val BASE_WATER = 0.09f
private const val WATER_STEP = 0.11f

// Tropfen-Text waechst mit der Systemschrift nur bis hierher, darueber waechst der Tropfen
private const val MAX_TEXT_SCALE = 1.6f

// Tropfen-Brett: Himmel mit Becken, darunter Anzeige und Ziffernfeld; alles haengt an der Spielzeit
@Composable
fun DropsBoard(s: DropsState, live: Boolean, count: Int, onKey: (Int) -> Unit) {
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val sky: @Composable (Modifier) -> Unit = { m ->
        Box(m) {
            Sky(s, Modifier.fillMaxSize())
            if (count > 0) CountOverlay(stringResource(R.string.drops_prompt), count)
        }
    }
    if (landscape) {
        Row(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            sky(Modifier.weight(1f).fillMaxHeight())
            Column(Modifier.width(280.dp).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Entry(s, Modifier.fillMaxWidth())
                Pad(live, onKey, Modifier.fillMaxWidth().padding(top = 10.dp), keyHeight = 52)
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
            sky(Modifier.weight(1f).fillMaxWidth())
            Entry(s, Modifier.fillMaxWidth().padding(top = 12.dp))
            Pad(live, onKey, Modifier.fillMaxWidth().padding(top = 10.dp), keyHeight = 58)
        }
    }
}

@Composable
private fun Sky(s: DropsState, modifier: Modifier) {
    val c = Aapps.colors
    val measurer = rememberTextMeasurer()
    val desc = stringResource(R.string.drops_board, s.drops.size, DropsRules.LIVES - s.splashes)
    Canvas(modifier.clip(RoundedCornerShape(24.dp)).semantics { contentDescription = desc }) {
        drawSky(s, measurer, c.accent, c.fgFaint)
    }
}

private fun DrawScope.drawSky(s: DropsState, measurer: TextMeasurer, accent: Color, faint: Color) {
    val now = s.round.now
    val nowD = now.toDouble()
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(SkyTop, SkyLow)))

    // Wasserpegel steigt mit Feder bei jedem Aufschlag
    val rise = if (s.splashAt >= 0) Spring.step(nowD - s.splashAt, hz = 1.6, damping = 0.45).toFloat() else 1f
    val filled = if (s.splashes == 0) 0f else s.splashes - 1 + rise
    val surface = h * (1f - BASE_WATER - WATER_STEP * filled)
    val slosh = if (s.splashAt >= 0) Spring.kick(nowD - s.splashAt, hz = 1.8, decayMs = 600.0).toFloat() else 0f

    // Schrift waechst bis MAX_TEXT_SCALE, darueber waechst der Tropfen
    val grow = (fontScale / MAX_TEXT_SCALE).coerceIn(1f, 1.4f)
    val r = 30.dp.toPx() * grow * (if (w < 340.dp.toPx()) 0.9f else 1f)
    val textPx = 15.dp.toPx() * min(fontScale, MAX_TEXT_SCALE) * grow

    // Tropfen: fallen von oben bis zur Wasserlinie, wackeln leicht
    val fallSpan = surface - r * 1.3f
    for (d in s.drops) {
        val p = d.progress(now)
        val enter = Spring.step(nowD - d.spawnAt, hz = 2.4, damping = 0.5).toFloat()
        val sway = sin((nowD - d.spawnAt) / 1000.0 * 2 * PI * 0.7 + d.id).toFloat() * r * 0.12f
        val x = r * 1.2f + d.x * (w - r * 2.4f) + sway
        val y = r * 1.1f + (fallSpan - r * 1.1f) * p
        drawDrop(Offset(x, y), r * (0.6f + 0.4f * enter), d, accent, measurer, textPx, 1f)
    }

    // Becken
    drawWater(surface, slosh, nowD, accent)

    // Platzen: Ringe und Spritzer; Aufschlag: bernsteinfarbener Spritzer an der Oberflaeche
    for (b in s.bursts) drawBurst(b, nowD, r, w, fallSpan, surface, accent)

    // Fuellstand: drei kleine Tropfen oben rechts, voll = Aufschlaege
    for (i in 0 until DropsRules.LIVES) {
        val cx = w - 18.dp.toPx() - i * 16.dp.toPx()
        val cy = 18.dp.toPx()
        val used = i < s.splashes
        val path = dropPath(Offset(cx, cy), 5.5.dp.toPx())
        if (used) drawPath(path, Amber) else drawPath(path, faint.copy(alpha = 0.6f), style = Stroke(1.4.dp.toPx()))
    }
}

private fun DrawScope.drawWater(surface: Float, slosh: Float, now: Double, accent: Color) {
    val w = size.width
    val h = size.height
    val path = Path()
    val n = 48
    val amp = 3.dp.toPx() + 10.dp.toPx() * slosh
    path.moveTo(0f, h)
    for (i in 0..n) {
        val x = w * i / n
        val y = surface +
            sin(now / 1000.0 * 1.6 + i * 0.42).toFloat() * amp * 0.6f +
            sin(now / 1000.0 * -1.1 + i * 0.23).toFloat() * amp * 0.4f
        path.lineTo(x, y)
    }
    path.lineTo(w, h)
    path.close()
    drawPath(path, Brush.verticalGradient(listOf(WaterTop.copy(alpha = 0.95f), WaterDeep), startY = surface, endY = h))
    val line = Path()
    for (i in 0..n) {
        val x = w * i / n
        val y = surface +
            sin(now / 1000.0 * 1.6 + i * 0.42).toFloat() * amp * 0.6f +
            sin(now / 1000.0 * -1.1 + i * 0.23).toFloat() * amp * 0.4f
        if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
    }
    drawPath(line, accent.copy(alpha = 0.18f), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
    drawPath(line, accent.copy(alpha = 0.75f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
}

// Glas-Tropfen mit Rechnung; glow = Leuchttropfen
private fun DrawScope.drawDrop(c: Offset, r: Float, d: Drop, accent: Color, measurer: TextMeasurer, textPx: Float, alpha: Float) {
    if (alpha <= 0.01f) return
    val path = dropPath(c, r)
    val glowColor = if (d.glow) Gold else accent
    drawGlow(glowColor, c, r * (if (d.glow) 2.6f else 1.9f), (if (d.glow) 0.4f else 0.22f) * alpha)
    drawPath(path, Brush.verticalGradient(listOf(Color(0xFF2B2346).copy(alpha = 0.92f * alpha), Color(0xFF1A2036).copy(alpha = 0.92f * alpha)), startY = c.y - r * 1.6f, endY = c.y + r))
    drawPath(path, (if (d.glow) Gold else DropRim).copy(alpha = 0.85f * alpha), style = Stroke(1.6.dp.toPx(), join = StrokeJoin.Round))
    // Glanzlicht links oben
    drawArc(
        Color.White.copy(alpha = 0.45f * alpha), 200f, 60f, false,
        topLeft = Offset(c.x - r * 0.72f, c.y - r * 0.72f), size = Size(r * 1.44f, r * 1.44f),
        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
    )
    val text = d.sum.toString()
    var style = TextStyle(
        color = Color.White.copy(alpha = alpha), fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
        fontSize = (textPx / density / fontScale).sp, textAlign = TextAlign.Center,
        fontFeatureSettings = "tnum",
    )
    var m = measurer.measure(text, style)
    // breite Rechnungen passen sich dem Tropfen an, statt umzubrechen
    val maxW = r * 1.75f
    if (m.size.width > maxW) {
        style = style.copy(fontSize = style.fontSize * (maxW / m.size.width))
        m = measurer.measure(text, style)
    }
    drawText(m, topLeft = Offset(c.x - m.size.width / 2f, c.y - m.size.height / 2f + r * 0.08f))
}

// Tropfenform: Kreis unten, Spitze oben
private fun dropPath(c: Offset, r: Float): Path = Path().apply {
    val tipY = c.y - r * 1.55f
    moveTo(c.x, tipY)
    cubicTo(c.x + r * 0.35f, c.y - r * 1.05f, c.x + r, c.y - r * 0.55f, c.x + r, c.y)
    cubicTo(c.x + r, c.y + r * 0.56f, c.x + r * 0.56f, c.y + r, c.x, c.y + r)
    cubicTo(c.x - r * 0.56f, c.y + r, c.x - r, c.y + r * 0.56f, c.x - r, c.y)
    cubicTo(c.x - r, c.y - r * 0.55f, c.x - r * 0.35f, c.y - r * 1.05f, c.x, tipY)
    close()
}

private fun DrawScope.drawBurst(b: Burst, now: Double, r: Float, w: Float, fallSpan: Float, surface: Float, accent: Color) {
    val dt = now - b.at
    if (dt < 0 || dt > 900) return
    val f = (dt / 900.0).toFloat()
    val x = r * 1.2f + b.drop.x * (w - r * 2.4f)
    val y = if (b.good) r * 1.1f + (fallSpan - r * 1.1f) * b.y else surface
    val color = if (!b.good) Amber else if (b.drop.glow) Gold else accent
    // Wellenringe
    for (k in 0 until 2) {
        val g = ((dt - k * 120) / 700.0).coerceIn(0.0, 1.0).toFloat()
        if (g <= 0f || g >= 1f) continue
        if (b.good) {
            drawCircle(color.copy(alpha = 0.7f * (1f - g)), r * (0.6f + 1.4f * g), Offset(x, y), style = Stroke(2.dp.toPx() * (1f - g) + 0.5f))
        } else {
            drawOval(color.copy(alpha = 0.7f * (1f - g)), Offset(x - r * (0.6f + 1.8f * g), y - r * 0.25f * (0.6f + 1.8f * g)), Size(r * 2 * (0.6f + 1.8f * g), r * 0.5f * (0.6f + 1.8f * g)), style = Stroke(1.6.dp.toPx()))
        }
    }
    // Spritzer: acht Tropfen fliegen auseinander und fallen
    for (k in 0 until 8) {
        val a = (if (b.good) 2 * PI * k / 8 else PI + PI * (k + 0.5) / 8) + b.drop.id * 0.37
        val speed = r * (if (b.good) 2.6f else 2.0f) * (0.7f + 0.3f * ((k * 37 + b.drop.id) % 5) / 4f)
        val px = x + cos(a).toFloat() * speed * f
        val py = y + sin(a).toFloat() * speed * f + r * 2.2f * f * f
        drawCircle(color.copy(alpha = (1f - f) * 0.9f), r * 0.09f * (1f - 0.5f * f), Offset(px, py))
    }
    // geplatzt: die Rechnung blitzt kurz als Lichtpunkt
    if (b.good) drawGlow(color, Offset(x, y), r * 2.2f, 0.45f * exp(-dt / 160.0).toFloat())
}

// Anzeige der Eingabe: Glas-Pille, wackelt bernstein bei falscher Eingabe
@Composable
private fun Entry(s: DropsState, modifier: Modifier) {
    val c = Aapps.colors
    val now = s.round.now.toDouble()
    val shake = if (s.wrongAt >= 0) Spring.kick(now - s.wrongAt, hz = 8.0, decayMs = 110.0).toFloat() else 0f
    val hot = s.wrongAt >= 0 && now - s.wrongAt < 400
    Box(
        modifier
            .heightIn(min = 56.dp)
            .graphicsLayer { translationX = shake * 14.dp.toPx() }
            .drawBehind { drawGlass(Offset.Zero, size, size.height / 2, if (hot) Amber else c.accent.copy(alpha = 0.45f)) },
        contentAlignment = Alignment.Center,
    ) {
        val text = s.entry.ifEmpty { "?" }
        BasicText(
            text,
            style = Aapps.type.headline.copy(
                fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum",
                color = if (s.entry.isEmpty()) c.fgFaint else c.fg, textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}

// Ziffernfeld 3x4: 1–9, dann Loeschen, 0, Bestaetigen
@Composable
private fun Pad(live: Boolean, onKey: (Int) -> Unit, modifier: Modifier, keyHeight: Int) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(DropsKeys.DEL, 0, DropsKeys.OK))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (k in row) Key(k, live, onKey, Modifier.weight(1f).height(keyHeight.dp))
            }
        }
    }
}

@Composable
private fun Key(k: Int, live: Boolean, onKey: (Int) -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    var pressed by remember { mutableStateOf(false) }
    val tap by rememberUpdatedState(onKey)
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, Springs.bouncy(), label = "key")
    val label = when (k) {
        DropsKeys.DEL -> stringResource(R.string.drops_delete)
        DropsKeys.OK -> stringResource(R.string.drops_enter)
        else -> k.toString()
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (live) 1f else 0.55f }
            .semantics {
                role = Role.Button
                contentDescription = label
                if (live) onClick { tap(k); true }
            }
            .pointerInput(live) {
                if (!live) return@pointerInput
                // loest beim Aufsetzen aus: schnelles Tippen zaehlt
                awaitEachGesture {
                    awaitFirstDown()
                    tap(k)
                    pressed = true
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        pressed = false
                    }
                }
            }
            .drawBehind {
                val edge = if (k == DropsKeys.OK) c.accent.copy(alpha = 0.7f) else c.line
                drawGlass(Offset.Zero, size, 18.dp.toPx(), if (pressed) c.accent else edge)
            },
        contentAlignment = Alignment.Center,
    ) {
        when (k) {
            DropsKeys.DEL -> AappsIcon(ThinkrIcons.Erase, null, c.fgMuted, size = 26.dp)
            DropsKeys.OK -> AappsIcon(ThinkrIcons.Yes, null, c.accent, size = 28.dp)
            // Ziffern skalieren nicht ueber die Taste hinaus
            else -> BasicText(
                label,
                style = TextStyle(
                    fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum",
                    fontSize = with(LocalDensity.current) { 26.dp.toSp() }, color = c.fg,
                ),
            )
        }
    }
}
