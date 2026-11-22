package solutions.xapient.dozr.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsColors
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.dozr.R
import solutions.xapient.dozr.domain.FadeCurve
import solutions.xapient.dozr.domain.Mode
import solutions.xapient.dozr.domain.Moon
import solutions.xapient.dozr.domain.MoonDrag
import solutions.xapient.dozr.domain.Phase
import solutions.xapient.dozr.domain.Sleep
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Geometrie aus der Groesse: Fenster, Horizont, Mondbahn um einen Punkt auf dem Horizont
class DialGeo(width: Float, height: Float, density: Float) {
    val center = Offset(width / 2f, height / 2f)
    val r = min(width, height) / 2f - 6f * density
    val horizonY = center.y + 0.22f * r
    val pivot = Offset(center.x, horizonY)
    val path = 0.74f * r
    val moonR = 0.105f * r

    fun moonAt(frac: Float): Offset {
        val a = Moon.angle(frac)
        return Offset(pivot.x + (cos(a) * path).toFloat(), pivot.y - (sin(a) * path).toFloat())
    }
}

// Was sich pro Bild aendert
class MoonLook(
    val frac: Float, // Mondposition 0..1
    val ghost: Float, // eingestellte Zeit beim Lauf, < 0 = aus
    val dip: Float, // 0..1 wie weit der Mond im Wasser ist
    val glow: Float, // 0..1 Helligkeit (folgt der Lautstaerke)
    val lit: Boolean, // Bogen leuchtet (Lauf)
    val dragging: Boolean,
)

// Umriss des Schatten-Monds: gegen den Himmel noch >= 3:1 (ContrastTest)
const val GHOST_ALPHA = 0.42f

// Leuchtender Bogen am Ende des Ausblendens: noch >= 3:1
const val ARC_MIN_ALPHA = 0.6f

private class Star(val pos: Offset, val size: Float, val alpha: Float, val big: Boolean)

private class Label(val pos: Offset, val layout: TextLayoutResult)

// Alles, was nur von Groesse und Farben abhaengt: einmal pro Groesse
class MoonParts(scope: CacheDrawScope, val c: AappsColors, measurer: TextMeasurer, labelStyle: TextStyle) {
    val dp = scope.density
    val g = DialGeo(scope.size.width, scope.size.height, dp)
    val visible = g.r > 40f * dp
    private val r = g.r
    private val center = g.center

    val window = Path().apply { addOval(Rect(center, r)) }

    private val sky = Brush.verticalGradient(
        0f to SkyTop, 0.55f to lerp(SkyTop, SkyLow, 0.55f), 1f to SkyLow,
        startY = center.y - r, endY = g.horizonY,
    )
    private val water = Brush.verticalGradient(
        0f to lerp(Water, SkyLow, 0.25f), 1f to Water,
        startY = g.horizonY, endY = center.y + r,
    )
    private val sheen = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.05f), 1f to Color.Transparent,
        center = center - Offset(r * 0.35f, r * 0.5f), radius = r * 0.9f,
    )

    // Sterne fest verteilt (gleicher Himmel bei jedem Start), nur ueber dem Horizont
    private val stars: List<Star> = run {
        val rnd = Random(11)
        val list = ArrayList<Star>()
        while (list.size < 54) {
            val p = Offset(center.x + (rnd.nextFloat() * 2f - 1f) * r, center.y - r + rnd.nextFloat() * (g.horizonY - center.y + r - 10f * dp))
            if ((p - center).getDistance() > r * 0.94f) continue
            // nicht direkt auf der Mondbahn, sonst unruhig
            if (abs((p - g.pivot).getDistance() - g.path) < g.moonR * 0.6f) continue
            val big = list.size < 6
            list += Star(p, (if (big) 1.5f else 0.5f + rnd.nextFloat() * 0.8f) * dp, 0.3f + rnd.nextFloat() * 0.55f, big)
        }
        list
    }

    // Huegel links am Horizont fuer Tiefe
    private val hills = Path().apply {
        val y = g.horizonY
        moveTo(center.x - r, y)
        lineTo(center.x - r, y - r * 0.05f)
        cubicTo(center.x - r * 0.8f, y - r * 0.13f, center.x - r * 0.62f, y - r * 0.12f, center.x - r * 0.5f, y - r * 0.05f)
        cubicTo(center.x - r * 0.42f, y - r * 0.01f, center.x - r * 0.35f, y - r * 0.08f, center.x - r * 0.2f, y - r * 0.03f)
        cubicTo(center.x - r * 0.12f, y, center.x - r * 0.05f, y, center.x, y)
        close()
    }

    // Sichel: Scheibe minus versetzte Scheibe, um den Ursprung
    private val crescent: Path = Path().apply {
        val m = g.moonR
        val disc = Path().apply { addOval(Rect(Offset.Zero, m)) }
        val cut = Path().apply { addOval(Rect(Offset(m * 0.42f, -m * 0.26f), m * 0.86f)) }
        op(disc, cut, PathOperation.Difference)
    }
    private val moonBrush = Brush.radialGradient(
        0f to Color.White, 0.45f to MoonLight, 1f to MoonShade,
        center = Offset(-g.moonR * 0.45f, g.moonR * 0.2f), radius = g.moonR * 1.3f,
    )

    // Rasten alle 5 min, alle 15 min groesser
    private val dots: List<Pair<Offset, Float>> = (0..Moon.STEPS).map { i ->
        g.moonAt(i / Moon.STEPS.toFloat()) to (if (i % 3 == 0) 1.7f else 1f) * dp
    }

    private val labels: List<Label> = if (!visible) emptyList() else with(scope) {
        val style = labelStyle.copy(fontSize = (r * 0.068f).toSp(), color = SkyLabel)
        listOf(30, 60, 90).map { m ->
            val layout = measurer.measure(m.toString(), style)
            val a = Moon.angle(Moon.fractionOf(m))
            val rr = g.path + g.moonR * 1.9f
            val p = Offset(g.pivot.x + (cos(a) * rr).toFloat(), g.pivot.y - (sin(a) * rr).toFloat())
            Label(Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f), layout)
        }
    }

    fun DrawScope.drawSky(look: MoonLook, moon: Offset) {
        drawRect(sky, topLeft = Offset(center.x - r, center.y - r), size = Size(2 * r, g.horizonY - center.y + r))
        // Mondschein am Himmel, tief stehend auch am Horizont
        drawGlow(DozrAccent, moon, g.moonR * 5.5f, 0.30f * look.glow)
        val low = 1f - ((g.horizonY - moon.y) / g.path).coerceIn(0f, 1f)
        drawGlow(DozrAccent, Offset(moon.x, g.horizonY), r * 0.8f, 0.16f * look.glow * low)
        // Sterne treten hervor, wenn der Mond verblasst
        val starBoost = 0.65f + 0.35f * (1f - look.glow)
        for (s in stars) {
            if (s.big) drawGlow(MoonLight, s.pos, s.size * 4f, 0.22f * starBoost)
            drawCircle(MoonLight.copy(alpha = (s.alpha * starBoost).coerceAtMost(1f)), s.size, s.pos)
        }
    }

    fun DrawScope.drawTrack(look: MoonLook) {
        for ((p, s) in dots) drawCircle(TrackDot, s, p)
        for (l in labels) drawText(l.layout, topLeft = l.pos)
        if (look.lit && look.frac > 0f) {
            val tl = g.pivot - Offset(g.path, g.path)
            val sz = Size(g.path * 2f, g.path * 2f)
            val sweep = 180f * look.frac
            drawArc(DozrAccent.copy(alpha = 0.16f * look.glow + 0.04f), -sweep, sweep, false, tl, sz, style = Stroke(9f * dp, cap = StrokeCap.Round))
            drawArc(DozrAccent.copy(alpha = ARC_MIN_ALPHA + (1f - ARC_MIN_ALPHA) * look.glow), -sweep, sweep, false, tl, sz, style = Stroke(2.4f * dp, cap = StrokeCap.Round))
        }
    }

    fun DrawScope.drawGhost(ghost: Float) {
        if (ghost < 0f) return
        val p = g.moonAt(ghost)
        drawCircle(MoonLight.copy(alpha = 0.06f), g.moonR, p)
        drawCircle(MoonLight.copy(alpha = GHOST_ALPHA), g.moonR, p, style = Stroke(1.2f * dp))
    }

    fun DrawScope.drawMoon(look: MoonLook, moon: Offset) {
        if (look.dragging) drawCircle(DozrAccent.copy(alpha = 0.7f), g.moonR * 1.45f, moon, style = Stroke(1.6f * dp))
        // aschgraues Licht: ganze Scheibe zart sichtbar, Griff bleibt erkennbar
        drawCircle(MoonLight.copy(alpha = 0.12f + 0.06f * look.glow), g.moonR, moon)
        translate(moon.x, moon.y) {
            drawPath(crescent, moonBrush, alpha = 0.35f + 0.65f * look.glow)
        }
    }

    fun DrawScope.drawWater(look: MoonLook, moon: Offset) {
        drawRect(water, topLeft = Offset(center.x - r, g.horizonY), size = Size(2 * r, center.y + r - g.horizonY))
        drawPath(hills, Hills)
        drawLine(WaterLine, Offset(center.x - r, g.horizonY), Offset(center.x + r, g.horizonY), 1f * dp)
        // Spiegelung: kurze Lichtstreifen unter dem Mond
        if (look.glow > 0f) {
            val gap = g.moonR * 0.42f
            for (i in 0 until 7) {
                val y = g.horizonY + gap * (i + 0.7f)
                val half = g.moonR * (1.05f - i * 0.1f) * (if (i % 2 == 0) 1f else 0.7f)
                val a = 0.4f * look.glow * (1f - i / 7f)
                drawLine(MoonLight.copy(alpha = a), Offset(moon.x - half, y), Offset(moon.x + half, y), 1.6f * dp, StrokeCap.Round)
            }
        }
    }

    fun DrawScope.drawRim() {
        drawCircle(sheen, r, center)
        drawCircle(c.line, r - 0.5f * dp, center, style = Stroke(1.2f * dp))
        drawCircle(Color.White.copy(alpha = 0.05f), r - 2.5f * dp, center, style = Stroke(1f * dp))
    }

    fun moonPos(look: MoonLook): Offset = g.moonAt(look.frac) + Offset(0f, look.dip * g.moonR * 2.3f)
}

fun DrawScope.drawMoonDial(parts: MoonParts, look: MoonLook) = with(parts) {
    if (!visible) return@with
    val moon = moonPos(look)
    drawGlow(DozrAccent, g.center, g.r * 1.22f, 0.06f + 0.1f * look.glow)
    clipPath(window) {
        drawSky(look, moon)
        drawTrack(look)
        drawGhost(look.ghost)
        drawMoon(look, moon)
        drawWater(look, moon)
    }
    drawRim()
}

// Eingaben als States: Laufen und Ziehen zeichnen nur neu, ohne neu zu komponieren
class DialInputs(
    val sleep: State<Sleep>,
    val fadeMs: State<Long>,
    val heroNow: MutableLongState,
    val drag: MutableState<Int?>,
    val idle: Animatable<Float, *>,
    val pathPx: FloatArray,
    // zuletzt gezeichnete Position, Start fuer die Feder nach dem Stoppen
    val lastFrac: FloatArray,
) {
    fun look(): MoonLook {
        val s = sleep.value
        val now = heroNow.longValue
        val dragging = drag.value != null
        return when (s.mode) {
            Mode.RUNNING -> {
                val fade = fadeMs.value
                val phase = s.phase(now, fade)
                val p = s.fadeProgress(now, fade).toFloat()
                val glow = when (phase) {
                    Phase.FADING -> FadeCurve.level(p.toDouble()).toFloat()
                    Phase.ENDED -> 0f
                    else -> 1f
                }
                val frac = Moon.fraction(s.remaining(now))
                lastFrac[0] = frac
                MoonLook(frac, Moon.fraction(s.totalMs), if (phase == Phase.RUNNING) 0f else p, glow, true, false)
            }
            Mode.DONE -> MoonLook(0f, -1f, 1f, 0f, false, false)
            Mode.IDLE -> MoonLook(idle.value, -1f, 0f, 1f, false, dragging)
        }
    }

    fun shownMinutes(): Int = drag.value ?: sleep.value.setMin
}

// Der Himmel: Ziehen zieht den Mond auf, TalkBack per Regler
@Composable
fun MoonDial(inputs: DialInputs, haptics: Haptics?, onChoose: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = Aapps.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = Aapps.type.number
    val density = LocalDensity.current.density
    val running = inputs.sleep.value.mode == Mode.RUNNING
    val minutes by remember(inputs) { derivedStateOf { inputs.shownMinutes() } }
    val runningMin by remember(inputs) {
        derivedStateOf { ((inputs.sleep.value.remaining(inputs.heroNow.longValue) + 59_999L) / 60_000L).toInt() }
    }
    val spokenMin = if (running) runningMin else minutes
    val spoken = pluralStringResource(R.plurals.minutes_n, spokenMin, spokenMin)
    val desc = stringResource(R.string.dial_desc)

    val draw = remember(inputs, colors, measurer, labelStyle) {
        Modifier.drawWithCache {
            val parts = MoonParts(this, colors, measurer, labelStyle)
            onDrawBehind { drawMoonDial(parts, inputs.look()) }
        }
    }

    Box(
        modifier
            .aspectRatio(1f)
            // Bahnradius fuer das Neu-Zeichnen pro Pixel
            .onSizeChanged { inputs.pathPx[0] = DialGeo(it.width.toFloat(), it.height.toFloat(), density).path }
            .semantics {
                contentDescription = desc
                stateDescription = spoken
                progressBarRangeInfo = ProgressBarRangeInfo(
                    (minutes / Sleep.STEP_MIN).toFloat(),
                    (Sleep.MIN_MIN / Sleep.STEP_MIN).toFloat()..Moon.STEPS.toFloat(),
                    Moon.STEPS - Sleep.MIN_MIN / Sleep.STEP_MIN - 1,
                )
                if (!running) {
                    setProgress { v ->
                        onChoose(Sleep.clampMin(Math.round(v) * Sleep.STEP_MIN))
                        true
                    }
                }
            }
            .pointerInput(inputs) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (inputs.sleep.value.mode == Mode.RUNNING) return@awaitEachGesture
                    val g = DialGeo(size.width.toFloat(), size.height.toFloat(), density)
                    if ((down.position - g.center).getDistance() > g.r) return@awaitEachGesture
                    // Tippen aendert nichts, erst echtes Ziehen
                    val slop = awaitTouchSlopOrCancellation(down.id) { ch, _ -> ch.consume() } ?: return@awaitEachGesture
                    val start = inputs.shownMinutes()
                    val drag = MoonDrag(Moon.fractionOf(start).toDouble())
                    var last = start
                    fun follow(p: Offset) {
                        val d = p - g.pivot
                        // nahe am Drehpunkt ist der Winkel unruhig
                        if (d.getDistance() < g.r * 0.12f) return
                        val m = Moon.snap(drag.move(d.x, -d.y))
                        if (m != last) {
                            haptics?.tick()
                            last = m
                        }
                        inputs.drag.value = m
                    }
                    follow(slop.position)
                    val id = slop.id
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == id } ?: break
                        if (!ch.pressed) break
                        ch.consume()
                        follow(ch.position)
                    }
                    val result = inputs.drag.value
                    if (result != null) onChoose(result)
                    inputs.drag.value = null
                }
            }
            .then(draw),
    )
}
