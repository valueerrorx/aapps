package solutions.xapient.spinr.domain

import kotlin.math.floor
import kotlin.math.min

// Rad-Geometrie: Winkel in Grad, im Uhrzeigersinn; Zeiger fest oben.
// Segment i liegt im Rad bei [i*seg, (i+1)*seg) ab der Rad-Null.
object WheelMath {
    fun segment(n: Int): Double = 360.0 / n

    // Segment unter dem Zeiger bei Rad-Drehung angle
    fun segmentAt(angle: Double, n: Int): Int {
        require(n > 0)
        val local = (-angle).mod(360.0)
        return min((local / segment(n)).toInt(), n - 1)
    }

    // Zaehler, der sich an jeder Segmentgrenze um 1 aendert
    fun tickIndex(angle: Double, n: Int): Long = floor(-angle / segment(n)).toLong()

    // Versatz der naechsten Grenze (Stift) zum Zeiger, positiv = rechts davon, in [-seg/2, seg/2)
    fun pegOffset(angle: Double, n: Int): Double {
        val seg = segment(n)
        return (angle + seg / 2).mod(seg) - seg / 2
    }

    // Rad-Drehung, bei der die Mitte von Segment i unter dem Zeiger steht
    fun angleFor(i: Int, n: Int): Double = -(i + 0.5) * segment(n)

    // Winkel auf [0, 360)
    fun normalize(angle: Double): Double = angle.mod(360.0)
}
