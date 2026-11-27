package solutions.xapient.tunr.domain

// Saiten als MIDI-Nummern in Spielreihenfolge (tiefste/erste links)
enum class Instrument(val strings: List<Int>) {
    CHROMATIC(emptyList()),
    GUITAR(listOf(40, 45, 50, 55, 59, 64)),
    GUITAR_DROP_D(listOf(38, 45, 50, 55, 59, 64)),
    BASS(listOf(28, 33, 38, 43)),
    UKULELE(listOf(67, 60, 64, 69)),
    VIOLIN(listOf(55, 62, 69, 76)),
    ;

    val chromatic: Boolean get() = strings.isEmpty()

    // Index der Saite mit dem kleinsten Abstand in Cent (centsA4 = Cent ueber A4)
    fun nearest(centsA4: Float): Int {
        var best = -1
        var bestDist = Float.MAX_VALUE
        strings.forEachIndexed { i, m ->
            val dist = kotlin.math.abs(centsA4 - (m - Notes.A4_MIDI) * 100f)
            if (dist < bestDist) {
                best = i; bestDist = dist
            }
        }
        return best
    }
}
