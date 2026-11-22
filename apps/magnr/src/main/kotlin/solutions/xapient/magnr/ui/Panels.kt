package solutions.xapient.magnr.ui

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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.magnr.R
import solutions.xapient.magnr.domain.History
import solutions.xapient.magnr.domain.Level
import solutions.xapient.magnr.domain.Reading

// Grundwert · Abweichung · Spitze; Ziffern nur in der Zeichenphase
@Composable
fun Stats(state: DetectorState, modifier: Modifier = Modifier) {
    val sep = decimalSeparator()
    val items = listOf<Pair<Int, (Reading) -> String>>(
        R.string.stat_baseline to { formatUt(it.baseline, sep) },
        R.string.stat_dev to { if (it.settling) "–" else formatUt(it.dev, sep, signed = true) },
        R.string.stat_peak to { formatUt(it.peak, sep, signed = true) },
    )
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { (label, pick) ->
            Stat(stringResource(label), { state.tick; pick(state.shown) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: () -> String, modifier: Modifier) {
    val c = Aapps.colors
    val measurer = rememberTextMeasurer()
    // Zahlen wachsen mit der Schrift nur maessig, damit die drei Spalten nebeneinander passen
    val style = Aapps.type.number.copy(fontSize = cappedSp(26f, 34f), color = c.fg)
    val labelMax = cappedSp(12f, 16f)
    // Hoehe einmal aus einer Probe, damit nichts springt
    val h = with(LocalDensity.current) { measurer.measure("+88.8", style).size.height.toDp() }
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = label }, horizontalAlignment = Alignment.CenterHorizontally) {
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
                    var l = measurer.measure(value(), style)
                    // sehr grosse Werte (starker Magnet) schmaler statt abgeschnitten
                    if (l.size.width > size.width) l = measurer.measure(value(), style.copy(fontSize = style.fontSize * (size.width / l.size.width)))
                    drawText(l, topLeft = Offset((size.width - l.size.width) / 2f, (size.height - l.size.height) / 2f))
                }
            },
        )
    }
}

// Verlauf der letzten 20 s als leuchtende Flaeche; Stufen-Linien gestrichelt, Pausen als Luecke
@Composable
fun HistoryChart(state: DetectorState, live: Boolean, modifier: Modifier = Modifier) {
    val c = Aapps.colors
    Spacer(
        modifier.drawWithCache {
            val top = 4.dp.toPx()
            fun y(s: Float) = top + (size.height - top) * (1f - s)
            val brush = Brush.verticalGradient(0f to AccentLight, 0.5f to MagnrAccent, 1f to MagnrAccent.copy(alpha = 0.8f), startY = y(1f), endY = size.height)
            val fill = Brush.verticalGradient(0f to MagnrAccent.copy(alpha = 0.34f), 1f to MagnrAccent.copy(alpha = 0.02f), startY = y(1f), endY = size.height)
            val line = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val gridW = 1.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
            val step = size.width / (History.SLICES - 1)
            val path = Path()
            val area = Path()
            // Pfad nur bei neuem Verlauf neu bauen; das Array wechselt alle 100 ms
            var built: FloatArray? = null
            onDrawBehind {
                state.tick
                listOf(Level.WEAK, Level.NEAR, Level.STRONG).forEach { l ->
                    drawLine(c.line, Offset(0f, y(l.from)), Offset(size.width, y(l.from)), gridW, pathEffect = dash)
                }
                drawLine(c.line, Offset(0f, size.height - gridW / 2), Offset(size.width, size.height - gridW / 2), gridW)
                val h = state.reading.history
                if (h.size < 2) return@onDrawBehind
                if (h !== built) {
                    built = h
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
                        val py = y(v)
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
                val a = if (live) 1f else PAUSED_ALPHA
                clipPath(area) { drawRect(fill, alpha = a) }
                drawPath(path, brush, alpha = a, style = line)
            }
        },
    )
}
