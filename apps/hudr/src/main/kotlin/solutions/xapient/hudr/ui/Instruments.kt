package solutions.xapient.hudr.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.hudr.domain.Alert
import solutions.xapient.hudr.domain.GpsPhase
import solutions.xapient.hudr.domain.Reading
import solutions.xapient.hudr.domain.Tape
import solutions.xapient.hudr.domain.formatSpeed
import solutions.xapient.hudr.ui.HudFont.drawHud
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

// Farben je Ansicht: Tacho (Akzent auf Dunkel) oder HUD (Cyan auf Schwarz)
class Palette(
    val bg: Color,
    val digit: Color,
    val muted: Color,
    val faint: Color,
    val amber: Color,
    val red: Color,
    val glow: Boolean,
) {
    fun alert(a: Alert): Color = when (a) {
        Alert.NONE -> digit
        Alert.NEAR -> amber
        Alert.OVER -> red
    }
}

@Composable
fun mainPalette(): Palette {
    val c = Aapps.colors
    return Palette(c.bg, c.accent, c.fgMuted, c.fgMuted.copy(alpha = FAINT_ALPHA), Amber, Red, glow = true)
}

val HudPalette = Palette(
    HudBg, HudCyan, HudCyan.copy(alpha = HUD_MUTED_ALPHA), HudCyan.copy(alpha = HUD_FAINT_ALPHA), HudAmber, HudRed, glow = false,
)

private fun ready(r: Reading) = r.phase == GpsPhase.FIX && !r.speed.isNaN()

/**
 * Riesige Ziffern in Strichschrift, rechtsbuendig in drei festen Zellen (nichts springt), Einheit darunter.
 * Ohne Wert "– – –". Alle Werte nur in der Zeichenphase.
 */
@Composable
fun Digits(
    display: SpeedDisplay,
    update: () -> Unit,
    unit: String,
    palette: Palette,
    modifier: Modifier = Modifier,
    // ohne Wert statt der Einheit (HUD: kein Statusbalken)
    status: ((GpsPhase) -> String)? = null,
) {
    val measurer = rememberTextMeasurer()
    val unitStyle = Aapps.type.label.copy(fontSize = cappedSp(14f, 18f))
    Spacer(
        modifier.drawWithCache {
            val unitL = measurer.measure(unit.uppercase(), unitStyle)
            val gap = 8.dp.toPx()
            // Hoehe aus der Flaeche; Breite der drei Zellen darf hoechstens die Breite fuellen
            val avail = size.height - unitL.size.height - gap
            // Lichthof ragt ueber die Zellen hinaus
            val halo = if (palette.glow) HudFont.WEIGHT * (HudFont.GLOW_WIDTH - 1) else 0f
            val wPerH = 3 * MIN_ASPECT + 2 * HudFont.GAP + HudFont.WEIGHT + halo
            val h = min(avail * 0.94f, size.width / wPerH)
            // schmal und hoch wie im HUD, aber nie breiter als die Grundform
            val aspect = min(HudFont.ASPECT, (size.width / h - 2 * HudFont.GAP - HudFont.WEIGHT - halo) / 3)
            val blockH = h + gap + unitL.size.height
            val top = (size.height - blockH) / 2
            onDrawBehind {
                update()
                val r = display.reading ?: return@onDrawBehind
                val live = ready(r)
                val text = if (live) formatSpeed(r.speed) else "–––"
                val col = if (live) palette.alert(r.alert) else palette.muted
                // feste Zellbreite (nichts zittert), zentriert nach Stellenzahl
                val w = HudFont.width(text, h, aspect)
                val x = (size.width - w) / 2
                drawHud(text, Offset(x, top), h, col, palette.glow && live, aspect)
                val under = if (!live && status != null) measurer.measure(status(r.phase).uppercase(), unitStyle) else unitL
                drawText(under, palette.muted, Offset(x + w - under.size.width, top + h + gap))
            }
        },
    )
}

/**
 * Laufendes Geschwindigkeitsband unter fester Zeiger-Box (Feder), Striche je 5, Zahlen je 10/20.
 * Warnschwelle als Bernstein-Band ab 90 % und Rot darueber, Max als Dreieck, Trend als Pfeil (3 s).
 */
@Composable
fun SpeedTape(
    display: SpeedDisplay,
    update: () -> Unit,
    vertical: Boolean,
    limit: Int?,
    palette: Palette,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Aapps.type.number.fontFamily, fontSize = cappedSp(13f, 17f), fontFeatureSettings = "tnum")
    Spacer(
        modifier.clipToBounds().drawWithCache {
            val len = if (vertical) size.height else size.width
            val thick = if (vertical) size.width else size.height
            val px = len / Tape.SPAN
            val probe = measurer.measure("000", labelStyle)
            val step = Tape.labelStep(px, if (vertical) probe.size.height.toFloat() else probe.size.width.toFloat())
            val base = min(thick * 0.42f, 40.dp.toPx())
            val major = 14.dp.toPx()
            // senkrecht: Inhalt (Pfeil, Striche, Zahlen) quer zentriert, Zeiger-Box nur so breit wie der Inhalt
            val ext = base + major + 10.dp.toPx() + probe.size.width
            val shift = if (vertical) maxOf(0f, (thick - ext) / 2) else 0f
            val boxEnd = if (vertical) min(ext + 4.dp.toPx(), thick - shift - 2.dp.toPx()) else thick - 2.dp.toPx()
            val minor = 7.dp.toPx()
            val band = 4.dp.toPx()
            val bandAt = base - 8.dp.toPx()
            val arrowAt = base - 20.dp.toPx()
            val labels = HashMap<Int, androidx.compose.ui.text.TextLayoutResult>()
            fun label(v: Int) = labels.getOrPut(v) { measurer.measure(v.toString(), labelStyle) }
            // entlang (0 = Mitte, positiv = schneller) und quer zur Achse
            fun at(along: Float, across: Float) =
                if (vertical) Offset(shift + across, len / 2 - along) else Offset(len / 2 + along, across)
            val fade = if (vertical) {
                Brush.verticalGradient(0f to palette.bg, 0.16f to palette.bg.copy(alpha = 0f), 0.84f to palette.bg.copy(alpha = 0f), 1f to palette.bg)
            } else {
                Brush.horizontalGradient(0f to palette.bg, 0.16f to palette.bg.copy(alpha = 0f), 0.84f to palette.bg.copy(alpha = 0f), 1f to palette.bg)
            }
            val hair = 1.5.dp.toPx()
            val boxHalf = 11.dp.toPx()
            onDrawBehind {
                update()
                val r = display.reading ?: return@onDrawBehind
                val live = ready(r)
                val a = if (live) 1f else LOST_ALPHA
                val center = display.tape
                val lo = maxOf(0f, center - Tape.SPAN / 2 - Tape.TICK)
                val hi = center + Tape.SPAN / 2 + Tape.TICK
                // Grundlinie
                drawLine(palette.faint, at(-len / 2, base), at(len / 2, base), hair)
                // Warnschwelle: Bernstein 90-100 %, Rot darueber
                if (limit != null) {
                    val nearFrom = limit * 0.9f
                    fun seg(from: Float, to: Float, col: Color) {
                        val f = maxOf(from, lo)
                        val t = min(to, hi)
                        if (t > f) drawLine(col.copy(alpha = a), at(Tape.offset(f, center, px), bandAt), at(Tape.offset(t, center, px), bandAt), band)
                    }
                    seg(nearFrom, limit.toFloat(), palette.amber)
                    seg(limit.toFloat(), hi, palette.red)
                }
                // Striche je 5, Zahlen je 10 oder 20
                var v = (ceil(lo / Tape.TICK) * Tape.TICK).toInt()
                while (v <= floor(hi)) {
                    val o = Tape.offset(v.toFloat(), center, px)
                    val big = v % 10 == 0
                    drawLine(if (big) palette.muted else palette.faint, at(o, base), at(o, base + if (big) major else minor), if (big) hair * 1.3f else hair)
                    if (v % step == 0) {
                        val l = label(v)
                        val p = at(o, base + major + 6.dp.toPx())
                        val tl = if (vertical) Offset(p.x, p.y - l.size.height / 2f) else Offset(p.x - l.size.width / 2f, p.y)
                        drawText(l, palette.muted, tl)
                    }
                    v += Tape.TICK
                }
                // Max als Dreieck ueber der Grundlinie
                if (!r.max.isNaN() && r.max in lo..hi) {
                    val o = Tape.offset(r.max, center, px)
                    val s = 6.dp.toPx()
                    val tip = at(o, base - 2.dp.toPx())
                    val p1 = at(o - s, base - 2.dp.toPx() - s * 1.4f)
                    val p2 = at(o + s, base - 2.dp.toPx() - s * 1.4f)
                    drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); close() }, palette.muted)
                }
                // Trend: Pfeil von der Mitte bis zur Vorhersage in 3 s
                if (live && !r.trend.isNaN() && abs(r.trend - r.speed) >= 1f) {
                    val o = Tape.offset(r.trend, center, px).coerceIn(-len / 2 + 12.dp.toPx(), len / 2 - 12.dp.toPx())
                    val col = palette.alert(r.alert)
                    val s = 6.dp.toPx() * if (o >= 0) 1f else -1f
                    drawLine(col, at(0f, arrowAt), at(o, arrowAt), 2.5.dp.toPx(), StrokeCap.Round)
                    val head = Path().apply {
                        val t = at(o, arrowAt)
                        val b1 = at(o - s, arrowAt - abs(s))
                        val b2 = at(o - s, arrowAt + abs(s))
                        moveTo(b1.x, b1.y); lineTo(t.x, t.y); lineTo(b2.x, b2.y)
                    }
                    drawPath(head, col, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                // Enden ausblenden
                drawRect(fade)
                // feste Zeiger-Box ueber die ganze Bandbreite
                val col = if (live) palette.alert(r.alert) else palette.muted
                val p0 = at(-boxHalf, 2.dp.toPx())
                val p1 = at(boxHalf, boxEnd)
                val tl = Offset(min(p0.x, p1.x), min(p0.y, p1.y))
                val sz = androidx.compose.ui.geometry.Size(abs(p1.x - p0.x), abs(p1.y - p0.y))
                drawRoundRect(col, tl, sz, androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = Stroke(hair))
                drawLine(col, at(0f, base - 10.dp.toPx()), at(0f, base + major + 2.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
            }
        },
    )
}

// HUD-Reticle statt Glaskarte: vier Eck-Klammern
fun Modifier.brackets(color: Color, arm: Dp = 16.dp, width: Dp = 1.5.dp): Modifier = drawWithCache {
    val a = arm.toPx()
    val w = width.toPx()
    val i = w / 2
    val path = Path().apply {
        moveTo(i, a); lineTo(i, i); lineTo(a, i)
        moveTo(size.width - a, i); lineTo(size.width - i, i); lineTo(size.width - i, a)
        moveTo(size.width - i, size.height - a); lineTo(size.width - i, size.height - i); lineTo(size.width - a, size.height - i)
        moveTo(a, size.height - i); lineTo(i, size.height - i); lineTo(i, size.height - a)
    }
    onDrawBehind { drawPath(path, color, style = Stroke(w, cap = StrokeCap.Round)) }
}

// schmalste Ziffernform (Breite zu Hoehe), damit hohe Flaechen gefuellt werden
private const val MIN_ASPECT = 0.36f
