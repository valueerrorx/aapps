package solutions.xapient.hertzr.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.hertzr.domain.Freq
import solutions.xapient.hertzr.domain.Oscillator
import solutions.xapient.hertzr.domain.Wave
import solutions.xapient.hertzr.res.Res
import solutions.xapient.hertzr.res.decimal_sep
import solutions.xapient.hertzr.res.enter_frequency
import solutions.xapient.hertzr.res.hz
import solutions.xapient.hertzr.res.note_names

private const val COLS = 8
private const val ROWS = 6

// Hero: Phosphor-Oszilloskop mit Raster, Vignette und Nachleuchten; Messgeraete-Anzeige links oben.
// hz, frame und progress werden nur in der Zeichenphase gelesen.
@Composable
fun Scope(
    hz: () -> Double,
    // eingestellte bzw. Sweep-Start-Frequenz: Neuzeichnen bei Aenderung
    shownHz: Double,
    wave: Wave,
    playing: Boolean,
    // Abtastrate der Ausgabe: zeigt, was wirklich klingt (hohe Toene werden zum Sinus)
    rate: Int,
    frame: () -> Long,
    // Sweep-Fortschritt 0..1, null = Dauerton
    progress: (() -> Float?)?,
    haptics: Haptics?,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = Aapps.colors
    val notes = stringArrayResource(Res.array.note_names)
    val dec = stringResource(Res.string.decimal_sep).first()
    val unit = stringResource(Res.string.hz)
    val measurer = rememberTextMeasurer()
    val type = Aapps.type
    val fs = LocalDensity.current.fontScale

    // Wellenwechsel morpht im Bild; danach keine Frames mehr
    var shown by remember { mutableStateOf(wave) }
    var prev by remember { mutableStateOf(wave) }
    val morph = remember { Animatable(1f) }
    LaunchedEffect(wave) {
        if (wave != shown) {
            prev = shown
            shown = wave
            morph.snapTo(0f)
            morph.animateTo(1f, Springs.gentle())
        }
    }

    BoxWithConstraints(
        modifier.drawWithCache {
            val g = screen(size)
            val corner = CornerRadius(20.dp.toPx())
            val vignette = Brush.radialGradient(
                0f to Color.Transparent, 0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.5f),
                center = size.center(), radius = maxOf(size.width, size.height) * 0.75f,
            )
            // Raster: 8 x 6 Teilungen, Mittelachsen mit Feinstrichen
            val grid = Path()
            for (i in 1 until COLS) {
                val x = g.left + g.width * i / COLS
                grid.moveTo(x, g.top); grid.lineTo(x, g.bottom)
            }
            for (j in 1 until ROWS) {
                val y = g.top + g.height * j / ROWS
                grid.moveTo(g.left, y); grid.lineTo(g.right, y)
            }
            val fine = Path()
            val tick = 3.dp.toPx()
            val midX = g.left + g.width / 2
            val midY = g.top + g.height / 2
            for (i in 0..COLS * 5) {
                val x = g.left + g.width * i / (COLS * 5)
                fine.moveTo(x, midY - tick); fine.lineTo(x, midY + tick)
            }
            for (j in 0..ROWS * 5) {
                val y = g.top + g.height * j / (ROWS * 5)
                fine.moveTo(midX - tick, y); fine.lineTo(midX + tick, y)
            }
            // Anzeige: feste Groesse fuer den breitesten Wert, damit nichts springt
            val pad = 16.dp.toPx()
            val probe = measurer.measure("00000", type.display.copy(fontSize = 100.sp))
            val numScale = min(g.width * 0.5f / probe.size.width, g.height * 0.16f / probe.size.height)
            val numStyle = type.display.copy(color = c.fg, fontSize = (100.sp.toPx() * numScale).toSp())
            val smallStyle = type.label.copy(color = c.fgMuted, fontSize = min(type.label.fontSize.toPx() * 1.1f * fs, g.height * 0.06f).toSp())
            val unitLayout = measurer.measure(unit, smallStyle.copy(letterSpacing = 0.06.sp))
            // Spur: Nulllinie in der Mitte, Ausschlag bis 1,5 Teilungen, aber nie in die Anzeige
            val readoutBottom = g.top + pad * 0.8f + measurer.measure("A4", smallStyle).size.height + probe.size.height * numScale
            val amp = min(g.height * 1.5f / ROWS, midY - readoutBottom - 10.dp.toPx()).coerceAtLeast(g.height * 0.12f)
            val clip = Path().apply { addRoundRect(RoundRect(0f, 0f, g.width, g.height, corner)) }
            val wavePath = Path()
            val ghost = Path()
            val core = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val halo = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val hot = Stroke(0.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val hotColor = Color.White.copy(alpha = 0.55f)
            onDrawBehind {
                // Sweep laeuft live (Bildzeit), sonst die eingestellte Frequenz
                val f = if (playing && progress != null) hz() else shownHz
                drawRoundRect(ScopeBg, g.topLeft, g.size, corner)
                clipPath(clip) {
                    drawPath(grid, c.line, style = Stroke(1.dp.toPx()))
                    drawPath(fine, c.line, style = Stroke(1.dp.toPx()))
                    // Perioden im Bild wachsen mit der Frequenz (log); beim Spielen laeuft die Spur
                    val pos = Freq.toPos(f)
                    val cycles = 1.25 + 3.5 * pos
                    val shift = if (playing) frame() / 1e9 * SCOPE_SPEED else 0.0
                    val k = morph.value.toDouble()
                    val blend = Oscillator.sineBlend(f / rate)
                    fun shape(w: Wave, t: Double): Double {
                        val raw = Oscillator.naive(w, t)
                        return if (blend <= 0.0 || w == Wave.SINE) raw else (1 - blend) * raw + blend * Oscillator.fundamental(w) * sin(2 * PI * t)
                    }
                    fun trace(path: Path, lag: Double) {
                        path.rewind()
                        val n = 200
                        for (i in 0..n) {
                            val x = i / n.toDouble()
                            var t = (x * cycles - shift + lag) % 1.0
                            if (t < 0) t += 1.0
                            val v = if (k >= 1.0) shape(shown, t) else k * shape(shown, t) + (1 - k) * shape(prev, t)
                            val px = g.left + g.width * x.toFloat()
                            val py = midY - amp * v.toFloat()
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                    }
                    // Nachleuchten: drei verblassende Kopien hinter der laufenden Spur
                    if (playing) {
                        for (j in 3 downTo 1) {
                            trace(ghost, j * GHOST_LAG)
                            drawPath(ghost, c.accent, alpha = 0.32f / (j + 1), style = core)
                        }
                    }
                    trace(wavePath, 0.0)
                    val alpha = if (playing) 1f else WAVE_IDLE_ALPHA
                    drawPath(wavePath, c.accent, alpha = 0.18f * alpha, style = halo)
                    drawPath(wavePath, c.accent, alpha = alpha, style = core)
                    if (playing) drawPath(wavePath, hotColor, style = hot)

                    // Sweep: duenne Zeitachse am unteren Rand
                    if (progress != null) {
                        val y = g.bottom - 10.dp.toPx()
                        val p = progress() ?: 0f
                        drawLine(c.line, Offset(g.left + pad, y), Offset(g.right - pad, y), 2.dp.toPx(), StrokeCap.Round)
                        if (p > 0f) drawLine(c.accent, Offset(g.left + pad, y), Offset(g.left + pad + (g.width - 2 * pad) * p, y), 2.dp.toPx(), StrokeCap.Round)
                    }
                    drawRect(vignette, g.topLeft, g.size)
                }
                drawRoundRect(c.line, g.topLeft, g.size, corner, style = Stroke(1.dp.toPx()))

                // Messgeraete-Anzeige: Notenname, Zahl, Einheit
                val nl = measurer.measure(noteLabel(f, notes), smallStyle)
                drawText(nl, topLeft = Offset(g.left + pad, g.top + pad * 0.8f))
                val num = measurer.measure(Freq.format(f, dec), numStyle)
                val numTop = g.top + pad * 0.8f + nl.size.height
                drawText(num, topLeft = Offset(g.left + pad - num.size.height * 0.04f, numTop))
                drawText(
                    unitLayout,
                    topLeft = Offset(g.left + pad + num.size.width + 6.dp.toPx(), numTop + num.firstBaseline - unitLayout.firstBaseline),
                )
            }
        },
        contentAlignment = Alignment.TopStart,
    ) {
        // Tippen auf die Anzeige: direkte Eingabe
        if (onEdit != null) {
            val density = LocalDensity.current
            val g = screen(Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()))
            val edit = stringResource(Res.string.enter_frequency)
            Box(
                Modifier
                    .size(with(density) { (g.width * 0.62f).toDp() }, with(density) { (g.height * 0.34f).toDp() })
                    .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClickLabel = edit) {
                        haptics?.press()
                        onEdit()
                    }
                    .semantics { contentDescription = edit },
            )
        }
    }
}

// Schirm fuellt die Flaeche; eigene Klasse, damit Zeichnen und Tippflaeche dieselbe Geometrie nutzen
internal class ScopeRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right get() = left + width
    val bottom get() = top + height
    val topLeft get() = Offset(left, top)
    val size get() = Size(width, height)
}

private fun screen(size: Size) = ScopeRect(0f, 0f, size.width, size.height)

private fun Size.center() = Offset(width / 2, height / 2)

// "A4", "A4 +12 ct", unter C-1 ein Strich
internal fun noteLabel(hz: Double, names: List<String>): String {
    val n = Freq.note(hz) ?: return "—"
    val name = names[n.pitchClass] + n.octave
    return when {
        n.cents == 0 -> name
        n.cents > 0 -> "$name  +${n.cents} ct"
        else -> "$name  −${-n.cents} ct"
    }
}

internal fun noteState(hz: Double, names: List<String>, dec: Char): String = "${Freq.short(hz, dec)}, ${noteLabel(hz, names)}"

// Laufgeschwindigkeit der Spur (Perioden je Sekunde im Bild) und Abstand der Nachleucht-Kopien
private const val SCOPE_SPEED = 0.6
private const val GHOST_LAG = 0.035
