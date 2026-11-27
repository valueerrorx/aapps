package solutions.xapient.sleepr

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.sleepr.res.Res
import solutions.xapient.sleepr.res.all_zero
import solutions.xapient.sleepr.res.brown
import solutions.xapient.sleepr.res.explain_brown
import solutions.xapient.sleepr.res.explain_green
import solutions.xapient.sleepr.res.explain_grey
import solutions.xapient.sleepr.res.explain_pink
import solutions.xapient.sleepr.res.explain_white
import solutions.xapient.sleepr.res.green
import solutions.xapient.sleepr.res.grey
import solutions.xapient.sleepr.res.hint_brown
import solutions.xapient.sleepr.res.hint_green
import solutions.xapient.sleepr.res.hint_grey
import solutions.xapient.sleepr.res.hint_pink
import solutions.xapient.sleepr.res.hint_white
import solutions.xapient.sleepr.res.mix_focus
import solutions.xapient.sleepr.res.mix_mask
import solutions.xapient.sleepr.res.mix_nature
import solutions.xapient.sleepr.res.mix_ocean
import solutions.xapient.sleepr.res.mix_sleep
import solutions.xapient.sleepr.res.mix_white
import solutions.xapient.sleepr.res.pink
import solutions.xapient.sleepr.res.slope_brown
import solutions.xapient.sleepr.res.slope_green
import solutions.xapient.sleepr.res.slope_grey
import solutions.xapient.sleepr.res.slope_pink
import solutions.xapient.sleepr.res.slope_white
import solutions.xapient.sleepr.res.treble_open
import solutions.xapient.sleepr.res.white
import kotlin.math.roundToInt

const val APP_NAME = "sleepr"

// Farben der Oberflaeche wie MainActivity auf Android
val SleeprAccent = Color(0xFF86B7CC)
internal val Track = Color(0xFF29343D)

class NoiseColor(val name: StringResource, val slope: StringResource, val hint: StringResource, val explain: StringResource, val color: Color) {
    // Weiss als Regler etwas gedaempft, sonst zu grell
    val tint: Color get() = if (color == WHITE) Color(0xFFC9D2D8) else color
}

private val WHITE = Color(0xFFE9EDF0)

val COLORS = listOf(
    NoiseColor(Res.string.white, Res.string.slope_white, Res.string.hint_white, Res.string.explain_white, WHITE),
    NoiseColor(Res.string.pink, Res.string.slope_pink, Res.string.hint_pink, Res.string.explain_pink, Color(0xFFD99AA3)),
    NoiseColor(Res.string.brown, Res.string.slope_brown, Res.string.hint_brown, Res.string.explain_brown, Color(0xFFB08465)),
    NoiseColor(Res.string.grey, Res.string.slope_grey, Res.string.hint_grey, Res.string.explain_grey, Color(0xFF9BA3AA)),
    NoiseColor(Res.string.green, Res.string.slope_green, Res.string.hint_green, Res.string.explain_green, Color(0xFF86B38D)),
)

fun presetName(p: Preset): StringResource = when (p.id) {
    "ocean" -> Res.string.mix_ocean
    "focus" -> Res.string.mix_focus
    "sleep" -> Res.string.mix_sleep
    "mask" -> Res.string.mix_mask
    "nature" -> Res.string.mix_nature
    else -> Res.string.mix_white
}

// Zusammensetzung einer Mischung, lauteste Farbe zuerst
@Composable
fun describe(mix: Mix): String = describe(IntArray(5) { mix.levels[it] }, BooleanArray(5) { mix.waves[it] })

@Composable
fun describe(lv: IntArray, wv: BooleanArray): String {
    val names = COLORS.map { stringResource(it.name) }
    val a = COLORS.indices.filter { lv[it] > 0 }.sortedByDescending { lv[it] }
    return if (a.isEmpty()) stringResource(Res.string.all_zero)
    else a.joinToString(" · ") { "${names[it]} ${lv[it]}" + if (wv[it]) " ∿" else "" }
}

@Composable
fun describe(p: Preset): String = describe(IntArray(5) { p.levels[it] ?: 0 }, BooleanArray(5) { p.waves.containsKey(it) })

// Dezimaltrennzeichen nach Oberflaechensprache
@Composable
fun fmtHz(f: Double): String {
    val sep = decimalSeparator()
    return when {
        f >= 19000 -> stringResource(Res.string.treble_open)
        f >= 1000 -> if (f < 10000) "${oneDecimal(f / 1000, sep)} kHz" else "${(f / 1000).roundToInt()} kHz"
        else -> "${f.toInt()} Hz"
    }
}

internal fun oneDecimal(v: Double, sep: Char): String {
    val t = (v * 10).roundToInt()
    return "${t / 10}$sep${t % 10}"
}

// Restzeit m:ss bzw. h:mm:ss
fun fmtTime(ms: Long): String {
    val s = (ms + 999) / 1000; val h = s / 3600; val m = s % 3600 / 60; val x = s % 60
    fun two(v: Long) = v.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(x)}" else "$m:${two(x)}"
}
