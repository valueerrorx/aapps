package solutions.xapient.nordr.domain

import kotlin.math.abs
import kotlin.math.roundToInt

// Gehaltener Kurs: Abweichung und Einrasten mit Hysterese
class CourseTracker(private val enter: Float = 2f, private val exit: Float = 4f) {
    var course: Float? = null
        private set
    var onCourse = false
        private set

    fun set(deg: Float) {
        course = Angles.normalize(deg)
        // beim Setzen ist man auf Kurs, ohne Einrast-Meldung
        onCourse = true
    }

    fun clear() {
        course = null
        onCourse = false
    }

    // Abweichung zum Kurs: positiv = Kurs liegt rechts
    fun deviation(heading: Float): Float = course?.let { Angles.delta(heading, it) } ?: 0f

    // true genau dann, wenn gerade auf Kurs eingerastet wurde
    fun update(heading: Float): Boolean {
        if (course == null) return false
        val d = abs(deviation(heading))
        val was = onCourse
        onCourse = if (onCourse) d <= exit else d < enter
        return onCourse && !was
    }
}

// Tick beim Erreichen oder Ueberstreichen von N/O/S/W; dieselbe Richtung erst wieder nach Verlassen der Zone
class CardinalDetector(private val enter: Float = 1.5f, private val exit: Float = 5f) {
    private var at: Int? = null
    private var previous = Float.NaN

    fun reset() {
        at = null
        previous = Float.NaN
    }

    // true, wenn gerade eine Hauptrichtung erreicht wurde (nie beim ersten Messwert)
    fun update(heading: Float): Boolean {
        val p = previous
        previous = heading
        at?.let { if (abs(Angles.delta(heading, it * 90f)) > exit) at = null }
        if (p.isNaN()) {
            val n = (heading / 90f).roundToInt().mod(4)
            if (abs(Angles.delta(heading, n * 90f)) < enter) at = n
            return false
        }
        val move = Angles.delta(p, heading)
        for (n in 0..3) {
            if (n == at) continue
            val c = n * 90f
            val toC = Angles.delta(p, c)
            val near = abs(Angles.delta(heading, c)) < enter
            val crossed = move != 0f && toC != 0f && (toC > 0f) == (move > 0f) && abs(toC) <= abs(move)
            if (near || crossed) {
                at = n
                return true
            }
        }
        return false
    }
}
