package solutions.xapient.loudr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.loudr.domain.Calibration
import solutions.xapient.loudr.domain.Reading
import solutions.xapient.loudr.domain.Scale
import solutions.xapient.loudr.domain.SoundMeter
import solutions.xapient.loudr.domain.TimeWeighting
import solutions.xapient.loudr.domain.Zone
import solutions.xapient.loudr.domain.formatDb
import solutions.xapient.loudr.res.Res
import solutions.xapient.loudr.res.stat_avg
import solutions.xapient.loudr.res.stat_max
import solutions.xapient.loudr.res.stat_min

// Min · Mittel · Max; Ziffern nur in der Zeichenphase
@Composable
fun Stats(
    display: MeterDisplay,
    update: () -> Unit,
    weighting: TimeWeighting,
    offset: Float,
    modifier: Modifier = Modifier,
) {
    val items = listOf<Pair<StringResource, (Reading) -> Float>>(
        Res.string.stat_min to { it.min(weighting) },
        Res.string.stat_avg to { it.leq },
        Res.string.stat_max to { it.max(weighting) },
    )
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { (label, pick) ->
            Stat(stringResource(label), { update(); formatDb(Calibration.apply(pick(display.shown), offset)) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: () -> String, modifier: Modifier) {
    val c = Aapps.colors
    val measurer = rememberTextMeasurer()
    // Zahlen wachsen mit der Schrift nur maessig, damit die drei Spalten nebeneinander passen
    val style = Aapps.type.number.copy(fontSize = cappedSp(30f, 40f), color = c.fg)
    val labelMax = cappedSp(12f, 16f)
    // Hoehe einmal aus einer Probe, damit nichts springt
    val h = with(LocalDensity.current) { measurer.measure("88", style).size.height.toDp() }
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = label }, horizontalAlignment = Alignment.CenterHorizontally) {
        // alle drei Beschriftungen gleich gross; zu lange werden einzeln kleiner
        BasicText(
            label.uppercase(),
            style = Aapps.type.label.copy(color = c.fgMuted, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = labelMax, stepSize = 0.5.sp),
        )
        Spacer(Modifier.height(2.dp))
        Spacer(
            Modifier.fillMaxWidth().height(h).drawWithCache {
                onDrawBehind {
                    val l = measurer.measure(value(), style)
                    drawText(l, topLeft = Offset((size.width - l.size.width) / 2f, 0f))
                }
            },
        )
    }
}

// Verlauf der letzten 60 s als Flaeche in Zonenfarben, 30–110 dB; Pausen als Luecke
@Composable
fun HistoryChart(
    display: MeterDisplay,
    update: () -> Unit,
    offset: Float,
    measuring: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = Aapps.colors
    val measurer = rememberTextMeasurer()
    val gridStyle = TextStyle(fontFamily = Aapps.type.number.fontFamily, fontSize = cappedSp(10f, 14f), color = c.fgMuted)
    Spacer(
        modifier.drawWithCache {
            fun y(db: Float) = size.height * (1 - Scale.fraction(db, Scale.HISTORY_MIN, Scale.HISTORY_MAX))
            val all = listOf(40, 60, 80, 100).map { db -> db to measurer.measure(db.toString(), gridStyle) }
            // bei wenig Hoehe oder grosser Schrift nur jede zweite Zahl
            val gap = y(40f) - y(60f)
            val grid = if (all.first().second.size.height * 1.15f > gap) all.filter { it.first % 40 == 0 } else all
            val labelW = all.maxOf { it.second.size.width } + 8.dp.toPx()
            val x0 = labelW
            val w = size.width - x0
            // Zonen von oben nach unten, harte Grenzen mit kurzem Uebergang
            val stops = mutableListOf(0f to zoneColor(Zone.VERY_LOUD))
            listOf(Zone.VERY_LOUD, Zone.LOUD, Zone.MODERATE).forEachIndexed { i, z ->
                val f = y(z.from) / size.height
                stops += (f - 0.005f) to zoneColor(z)
                stops += (f + 0.005f) to zoneColor(Zone.entries[Zone.entries.size - 2 - i])
            }
            stops += 1f to zoneColor(Zone.QUIET)
            val brush = Brush.verticalGradient(*stops.toTypedArray(), startY = 0f, endY = size.height)
            val line = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val gridW = 1.dp.toPx()
            val fade = Brush.verticalGradient(0f to c.bg.copy(alpha = 0f), 1f to c.bg.copy(alpha = 0.9f), startY = y(Scale.HISTORY_MAX), endY = size.height)
            val path = Path()
            val area = Path()
            // Pfad nur bei neuem Verlauf neu bauen; das Array wechselt alle 100 ms
            var built: FloatArray? = null
            onDrawBehind {
                update()
                all.forEach { (db, _) ->
                    val gy = y(db.toFloat())
                    drawLine(c.line, Offset(x0, gy), Offset(size.width, gy), gridW)
                }
                grid.forEach { (db, l) -> drawText(l, topLeft = Offset(0f, y(db.toFloat()) - l.size.height / 2f)) }
                val h = display.raw.history
                if (h.size < 2) return@onDrawBehind
                if (h !== built) {
                    built = h
                    val step = w / (SLICES - 1)
                    path.reset()
                    area.reset()
                    val first = size.width - (h.size - 1) * step
                    var lastX = Float.NaN
                    fun closeArea() {
                        if (!lastX.isNaN()) {
                            area.lineTo(lastX, size.height)
                            area.close()
                        }
                        lastX = Float.NaN
                    }
                    h.forEachIndexed { i, v ->
                        if (v.isNaN()) {
                            closeArea()
                            return@forEachIndexed
                        }
                        val px = first + i * step
                        val py = y(Calibration.apply(v, offset))
                        if (lastX.isNaN()) {
                            path.moveTo(px, py)
                            area.moveTo(px, size.height)
                            area.lineTo(px, py)
                        } else {
                            path.lineTo(px, py)
                            area.lineTo(px, py)
                        }
                        lastX = px
                    }
                    closeArea()
                }
                val a = if (measuring) 1f else PAUSED_ALPHA
                // Flaeche in Zonenfarben, nach unten ausblendend statt in Streifen
                drawPath(area, brush, alpha = 0.28f * a)
                clipPath(area) { drawRect(fade) }
                drawPath(path, brush, alpha = a, style = line)
            }
        },
    )
}

private const val SLICES = SoundMeter.HISTORY_SLICES
