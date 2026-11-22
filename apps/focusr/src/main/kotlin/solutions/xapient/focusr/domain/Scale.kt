package solutions.xapient.focusr.domain

// Zifferblatt: eine Umdrehung = minutes, immer 60 Rasten; labelEvery = Rasten zwischen zwei Zahlen
enum class Scale(val minutes: Int, val labelEvery: Int) {
    MIN5(5, 12),
    MIN20(20, 6),
    MIN60(60, 5),
    MIN120(120, 5);

    val ms: Long get() = minutes * Timer.MINUTE
    val stepMs: Long get() = ms / STEPS

    // Beschriftete Rasten mit Minutenwert, 0 oben
    val labels: List<Pair<Int, Int>> get() = (0 until STEPS step labelEvery).map { it to (it * stepMs / Timer.MINUTE).toInt() }

    companion object {
        const val STEPS = 60
        val DEFAULT = MIN60

        fun fromMinutes(m: Int): Scale = entries.firstOrNull { it.minutes == m } ?: DEFAULT
    }
}
