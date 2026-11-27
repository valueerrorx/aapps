package solutions.xapient.roundr.domain

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class CuesTest {
    private val tabata = Timeline(Workout(prepSec = 10, workSec = 20, restSec = 10, rounds = 2))
    private val points = Cues.points(tabata)

    @Test fun countdownBeforeEachEndAndPhaseStarts() {
        val firstTen = points.takeWhile { it.atMs <= 10_000 }
        assertEquals(
            listOf(
                CuePoint(0, Cue.Start(Phase.PREP)),
                CuePoint(7_000, Cue.Count(3)), CuePoint(8_000, Cue.Count(2)), CuePoint(9_000, Cue.Count(1)),
                CuePoint(10_000, Cue.Start(Phase.WORK)),
            ),
            firstTen,
        )
        assertEquals(CuePoint(30_000, Cue.Start(Phase.REST)), points.first { it.atMs == 30_000L })
        assertEquals(CuePoint(tabata.totalMs, Cue.Done), points.last())
        assertEquals(points.sortedBy { it.atMs }, points)
    }

    @Test fun shortSegmentsGetOnlyFittingCounts() {
        // 3 s Abschnitt: 3 faellt auf den Start, nur 2 und 1
        val t = Timeline(Workout(prepSec = 3, workSec = 5, restSec = 0, rounds = 1))
        val counts = Cues.points(t).filter { it.atMs < 3_000 }.map { it.cue }
        assertEquals(listOf(Cue.Start(Phase.PREP), Cue.Count(2), Cue.Count(1)), counts)
    }

    @Test fun dueIsHalfOpenWindow() {
        assertEquals(Cue.Count(3), Cues.due(points, 6_999, 7_000))
        assertNull(Cues.due(points, 7_000, 7_999))
        assertEquals(Cue.Start(Phase.PREP), Cues.due(points, -1, 0))
        assertNull(Cues.due(points, 0, 0))
    }

    // Verspaeteter Tick: nur die juengste Ansage, keine Piep-Salve
    @Test fun lateTickPlaysOnlyLatest() {
        assertEquals(Cue.Start(Phase.WORK), Cues.due(points, 6_500, 10_200))
        assertEquals(Cue.Done, Cues.due(points, 0, tabata.totalMs + 5_000))
    }

    @Test fun nextAt() {
        assertEquals(7_000L, Cues.nextAt(points, 0))
        assertEquals(10_000L, Cues.nextAt(points, 9_000))
        assertEquals(tabata.totalMs, Cues.nextAt(points, tabata.totalMs - 1))
        assertNull(Cues.nextAt(points, tabata.totalMs))
    }
}
