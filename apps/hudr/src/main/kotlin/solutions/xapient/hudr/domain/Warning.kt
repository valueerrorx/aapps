package solutions.xapient.hudr.domain

enum class Alert { NONE, NEAR, OVER }

/**
 * Warnschwelle in der Anzeige-Einheit mit Hysterese.
 * OVER, sobald die angezeigte Zahl die Schwelle uebersteigt; zurueck erst 2 Einheiten darunter.
 * NEAR ab 90 % der Schwelle; zurueck 1 Einheit darunter.
 */
class Warning {
    var state = Alert.NONE
        private set

    // true genau beim Eintritt in OVER (Ton); ohne Wert bleibt der Zustand stehen
    fun update(value: Float, limit: Int?): Boolean {
        if (limit == null) {
            state = Alert.NONE
            return false
        }
        if (value.isNaN()) return false
        val prev = state
        val nearOn = limit * NEAR
        state = when {
            value >= limit + 0.5f -> Alert.OVER
            prev == Alert.OVER && value >= limit - OVER_HYST + 0.5f -> Alert.OVER
            value >= nearOn -> Alert.NEAR
            prev != Alert.NONE && value >= nearOn - NEAR_HYST -> Alert.NEAR
            else -> Alert.NONE
        }
        return state == Alert.OVER && prev != Alert.OVER
    }

    fun reset() {
        state = Alert.NONE
    }

    companion object {
        const val NEAR = 0.9f
        const val OVER_HYST = 2f
        const val NEAR_HYST = 1f
    }
}
