package solutions.xapient.roundr.domain

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class TimelineTest {
    private fun phases(w: Workout) = Timeline(w).segments.map { it.phase }

    @Test fun tabataHasPrepThenAlternatingWithoutTrailingRest() {
        val t = Timeline(Workout(prepSec = 10, workSec = 20, restSec = 10, rounds = 8))
        assertEquals(1 + 8 + 7, t.segments.size)
        assertEquals(Phase.PREP, t.segments.first().phase)
        assertEquals(Phase.WORK, t.segments.last().phase)
        assertEquals((10 + 8 * 20 + 7 * 10) * 1000L, t.totalMs)
    }

    @Test fun segmentsAreContiguous() {
        val t = Timeline(Presets.all[1])
        t.segments.zipWithNext().forEach { (a, b) -> assertEquals(a.endMs, b.startMs) }
        assertEquals(t.segments.last().endMs, t.totalMs)
    }

    @Test fun zeroRestAndZeroPrepAreSkipped() {
        assertEquals(List(3) { Phase.WORK }, phases(Workout(prepSec = 0, workSec = 60, restSec = 0, rounds = 3)))
    }

    @Test fun setRestReplacesRestBetweenSets() {
        val p = phases(Workout(prepSec = 0, workSec = 20, restSec = 10, rounds = 2, sets = 2, setRestSec = 60))
        assertEquals(listOf(Phase.WORK, Phase.REST, Phase.WORK, Phase.SET_REST, Phase.WORK, Phase.REST, Phase.WORK), p)
    }

    @Test fun zeroSetRestFallsBackToRest() {
        val p = phases(Workout(prepSec = 0, workSec = 20, restSec = 10, rounds = 1, sets = 3, setRestSec = 0))
        assertEquals(listOf(Phase.WORK, Phase.REST, Phase.WORK, Phase.REST, Phase.WORK), p)
    }

    @Test fun roundAndSetNumbers() {
        val t = Timeline(Workout(prepSec = 5, workSec = 20, restSec = 10, rounds = 2, sets = 2, setRestSec = 30))
        val work = t.segments.filter { it.phase == Phase.WORK }.map { it.set to it.round }
        assertEquals(listOf(1 to 1, 1 to 2, 2 to 1, 2 to 2), work)
    }

    @Test fun positionAndRemaining() {
        val t = Timeline(Workout(prepSec = 10, workSec = 20, restSec = 10, rounds = 8))
        val p0 = t.at(0)!!
        assertEquals(Phase.PREP, p0.segment.phase)
        assertEquals(10_000, p0.remainingMs)
        assertEquals(1f, p0.fraction)
        val p = t.at(10_000 + 5_000)!!
        assertEquals(Phase.WORK, p.segment.phase)
        assertEquals(15_000, p.remainingMs)
        assertEquals(0.75f, p.fraction)
        // genau auf der Grenze beginnt der naechste Abschnitt
        assertEquals(Phase.REST, t.at(30_000)!!.segment.phase)
        assertEquals(3, t.at(70_500)!!.segment.round)
    }

    @Test fun finishedAndNegative() {
        val t = Timeline(Presets.default)
        assertNull(t.at(t.totalMs))
        assertNull(t.at(t.totalMs + 99_999))
        assertEquals(0, t.at(-50)!!.index)
    }

    @Test fun skipAndBackTargets() {
        val t = Timeline(Workout(prepSec = 10, workSec = 20, restSec = 10, rounds = 3))
        assertEquals(30_000, t.skipTarget(12_000))
        assertEquals(t.totalMs, t.skipTarget(t.totalMs))
        // mehr als 2 s im Abschnitt: an dessen Anfang
        assertEquals(10_000, t.backTarget(15_000))
        // in den ersten 2 s: voriger Abschnitt
        assertEquals(0, t.backTarget(11_000))
        assertEquals(0, t.backTarget(500))
        // fertig: letzter Abschnitt
        assertEquals(t.segments.last().startMs, t.backTarget(t.totalMs))
    }

    @Test fun outOfRangeValuesAreClamped() {
        val t = Timeline(Workout(prepSec = -5, workSec = 1, restSec = 99_999, rounds = 500))
        assertEquals(99, t.rounds)
        assertEquals(5_000, t.segments.first().durationMs)
    }
}
