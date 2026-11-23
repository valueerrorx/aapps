package solutions.xapient.sleepr

import android.content.Context
import androidx.annotation.StringRes
import kotlin.math.pow

data class NoiseColor(@StringRes val name: Int, @StringRes val slope: Int, @StringRes val hint: Int, val color: Int)
// id ist stabil, name nur Anzeige
data class Preset(val id: String, @StringRes val name: Int, val levels: Map<Int, Int>, val tone: Int, val waves: Map<Int, Int> = emptyMap())

/** Gemeinsamer Zustand von UI und Audio-Thread, in SharedPreferences gespeichert */
object Mixer {
    val COLORS = listOf(
        NoiseColor(R.string.white, R.string.slope_white, R.string.hint_white, 0xFFE9EDF0.toInt()),
        NoiseColor(R.string.pink, R.string.slope_pink, R.string.hint_pink, 0xFFD99AA3.toInt()),
        NoiseColor(R.string.brown, R.string.slope_brown, R.string.hint_brown, 0xFFB08465.toInt()),
        NoiseColor(R.string.grey, R.string.slope_grey, R.string.hint_grey, 0xFF9BA3AA.toInt()),
        NoiseColor(R.string.green, R.string.slope_green, R.string.hint_green, 0xFF86B38D.toInt())
    )
    val PRESETS = listOf(
        Preset("ocean", R.string.mix_ocean, mapOf(2 to 70, 1 to 35), 70, waves = mapOf(2 to 9, 1 to 13)),
        Preset("focus", R.string.mix_focus, mapOf(1 to 55, 2 to 35), 100),
        Preset("sleep", R.string.mix_sleep, mapOf(2 to 70), 55),
        Preset("mask", R.string.mix_mask, mapOf(3 to 50, 1 to 40), 100),
        Preset("nature", R.string.mix_nature, mapOf(4 to 60, 2 to 25), 85),
        Preset("white", R.string.mix_white, mapOf(0 to 45), 80)
    )
    fun preset(id: String) = PRESETS.first { it.id == id }
    val TIMERS = intArrayOf(0, 15, 30, 45, 60, 90)

    val levels = intArrayOf(0, 20, 60, 0, 0)
    val waves = BooleanArray(5)              // Wellen-Modulation je Farbe
    val waveSec = intArrayOf(8, 8, 8, 8, 8)  // mittlere Wellenlänge in s
    const val WAVE_MIN = 3
    const val WAVE_MAX = 20
    @Volatile var master = 70
    @Volatile var tone = 100
    @Volatile var timer = 0

    fun gain(v: Int) = (v / 100.0).pow(2) // wahrnehmungsnahe Kurve
    fun gain(v: Double) = (v / 100.0).pow(2)
    fun toneHz(v: Int = tone) = 400.0 * 50.0.pow(v / 100.0) // 400 Hz … 20 kHz log

    // Dezimaltrennzeichen nach Oberflächensprache
    fun fmtHz(ctx: Context, f: Double): String {
        val loc = ctx.resources.configuration.locales[0]
        return when {
            f >= 19000 -> ctx.getString(R.string.treble_open)
            f >= 1000 -> String.format(loc, if (f < 10000) "%.1f kHz" else "%.0f kHz", f / 1000)
            else -> String.format(loc, "%d Hz", f.toInt())
        }
    }

    fun summary(ctx: Context): String = describe(ctx, levels, waves)

    // Zusammensetzung einer Mischung, lauteste Farbe zuerst
    fun describe(ctx: Context, p: Preset): String = describe(ctx, IntArray(5) { p.levels[it] ?: 0 }, BooleanArray(5) { p.waves.containsKey(it) })

    private fun describe(ctx: Context, lv: IntArray, wv: BooleanArray): String {
        val a = COLORS.indices.filter { lv[it] > 0 }.sortedByDescending { lv[it] }
        return if (a.isEmpty()) ctx.getString(R.string.all_zero)
        else a.joinToString(" · ") { "${ctx.getString(COLORS[it].name)} ${lv[it]}" + if (wv[it]) " ∿" else "" }
    }

    /** true, solange die aktuelle Einstellung genau dieser Mischung entspricht */
    fun matches(p: Preset): Boolean = tone == p.tone && levels.indices.all { i ->
        levels[i] == (p.levels[i] ?: 0) && waves[i] == p.waves.containsKey(i) && (p.waves[i] ?: waveSec[i]) == waveSec[i]
    }

    fun applyPreset(p: Preset) {
        for (i in levels.indices) {
            levels[i] = p.levels[i] ?: 0
            waves[i] = p.waves.containsKey(i)
            p.waves[i]?.let { waveSec[i] = it }
        }
        tone = p.tone
    }

    private const val PREFS = "mixer"

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        for (i in levels.indices) {
            levels[i] = p.getInt("l$i", levels[i])
            waves[i] = p.getBoolean("w$i", waves[i])
            waveSec[i] = p.getInt("ws$i", waveSec[i])
        }
        master = p.getInt("master", master); tone = p.getInt("tone", tone); timer = p.getInt("timer", timer)
    }

    fun save(ctx: Context) {
        val e = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        for (i in levels.indices) e.putInt("l$i", levels[i]).putBoolean("w$i", waves[i]).putInt("ws$i", waveSec[i])
        e.putInt("master", master).putInt("tone", tone).putInt("timer", timer).apply()
    }
}
