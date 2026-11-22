package solutions.xapient.hudr.ui

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
import solutions.xapient.hudr.R
import solutions.xapient.hudr.domain.GpsPhase
import solutions.xapient.hudr.domain.Reading
import solutions.xapient.hudr.domain.SpeedUnit
import solutions.xapient.hudr.domain.formatDistance
import solutions.xapient.hudr.domain.formatSpeed
import kotlin.math.roundToInt

// GPS-Stand oben links: Punkt und Text, nur in der Zeichenphase
@Composable
fun GpsStatus(display: SpeedDisplay, update: () -> Unit, modifier: Modifier = Modifier) {
    val c = Aapps.colors
    val measurer = rememberTextMeasurer()
    val style = Aapps.type.label.copy(color = c.fgMuted, fontSize = cappedSp(12f, 16f))
    val searching = stringResource(R.string.gps_searching).uppercase()
    val lost = stringResource(R.string.gps_lost).uppercase()
    val fixFmt = stringResource(R.string.gps_fix)
    val satFmt = stringResource(R.string.gps_sats)
    val h = with(LocalDensity.current) { measurer.measure("±8", style).size.height.toDp() }
    Spacer(
        modifier.height(h).drawWithCache {
            val dot = 4.dp.toPx()
            val gap = 10.dp.toPx()
            onDrawBehind {
                update()
                val r = display.reading ?: return@onDrawBehind
                val (text, col) = when (r.phase) {
                    GpsPhase.SEARCHING -> searching to c.fgMuted
                    GpsPhase.LOST -> lost to Amber
                    GpsPhase.FIX -> {
                        val acc = if (r.accuracy.isNaN()) "" else fixFmt.format(r.accuracy.roundToInt())
                        val sats = if (r.visible > 0) satFmt.format(r.used, r.visible) else ""
                        listOf(acc, sats).filter { it.isNotEmpty() }.joinToString(" · ").uppercase() to c.accent
                    }
                }
                drawCircle(col, dot, Offset(dot, size.height / 2))
                val l = measurer.measure(text, style, maxLines = 1, softWrap = false)
                drawText(l, topLeft = Offset(dot * 2 + gap, (size.height - l.size.height) / 2))
            }
        },
    )
}

// Max · Ø · Strecke der Fahrt
@Composable
fun Stats(display: SpeedDisplay, update: () -> Unit, unit: SpeedUnit, modifier: Modifier = Modifier) {
    val speedUnit = stringResource(unitRes(unit))
    val distUnit = stringResource(distRes(unit))
    val sep = decimalSeparator()
    val items = listOf<Triple<Int, String, (Reading) -> String>>(
        Triple(R.string.stat_max, speedUnit) { formatSpeed(it.max) },
        Triple(R.string.stat_avg, speedUnit) { formatSpeed(it.avg) },
        Triple(R.string.stat_dist, distUnit) { formatDistance(it.distance, sep) },
    )
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEach { (label, u, pick) ->
            Stat(stringResource(label), u, { update(); display.reading?.let(pick) ?: "–" }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, unit: String, value: () -> String, modifier: Modifier) {
    val c = Aapps.colors
    val measurer = rememberTextMeasurer()
    // Zahlen wachsen mit der Schrift nur maessig, damit die drei Spalten nebeneinander passen
    val style = Aapps.type.number.copy(fontSize = cappedSp(28f, 36f), color = c.fg)
    val unitStyle = Aapps.type.label.copy(fontSize = cappedSp(11f, 14f), color = c.fgMuted)
    val labelMax = cappedSp(12f, 16f)
    // Hoehe einmal aus einer Probe, damit nichts springt
    val h = with(LocalDensity.current) { measurer.measure("88", style).size.height.toDp() }
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
                val u = measurer.measure(unit, unitStyle)
                val gap = 4.dp.toPx()
                onDrawBehind {
                    val l = measurer.measure(value(), style)
                    // zu breit (grosse Schrift, lange Strecke): Einheit weglassen
                    val withUnit = l.size.width + gap + u.size.width <= size.width
                    val total = l.size.width + if (withUnit) gap + u.size.width else 0f
                    val x = (size.width - total) / 2f
                    drawText(l, topLeft = Offset(x, 0f))
                    if (withUnit) {
                        // Einheit auf der Grundlinie der Zahl
                        val base = l.firstBaseline - u.firstBaseline
                        drawText(u, topLeft = Offset(x + l.size.width + gap, base))
                    }
                }
            },
        )
    }
}
