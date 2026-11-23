package solutions.xapient.thinkr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodecTest {
    private val sample = Journal()
        .record(RoundResult(20261122, GameId.ECHO, 3450, 7, 86, 60_000, false), listOf(GameId.ECHO))
        .record(RoundResult(20261123, GameId.ECHO, 1200, 3, 71, 60_000, true), listOf(GameId.ECHO))

    @Test fun roundTrip() {
        val text = Codec.encode(sample)
        assertTrue(text.startsWith(Codec.HEADER + "\n"))
        assertEquals(sample, Codec.decode(text))
    }

    @Test fun emptyRoundTrip() {
        assertEquals(Journal(), Codec.decode(Codec.encode(Journal())))
    }

    @Test fun missingOrForeignDataIsEmpty() {
        assertEquals(Journal(), Codec.decode(null))
        assertEquals(Journal(), Codec.decode(""))
        assertEquals(Journal(), Codec.decode("thinkr9\nP|echo|0|3|100|1"))
    }

    @Test fun brokenLinesAreSkippedOthersKept() {
        val text = listOf(
            Codec.HEADER,
            "P|echo|0|4|900|3",
            "P|echo|2|4|900|3",
            "P|nope|0|4|900|3",
            "P|echo|1|0|900|3",
            "P|echo|1|4|-5|3",
            "P|echo|1",
            "R|20261123|echo|500|4|80|60000|0",
            "R|20261340|echo|500|4|80|60000|0",
            "R|20261123|echo|500|4|180|60000|0",
            "R|20261123|echo|x|4|80|60000|0",
            "R|20261123|echo|500|4|80|60000",
            "D|20261123",
            "D|2026",
            "D|20261123|1",
            "garbage",
            "",
        ).joinToString("\n")
        val j = Codec.decode(text)
        assertEquals(mapOf(ProgressKey(GameId.ECHO, false) to GameProgress(4, 900, 3)), j.progress)
        assertEquals(listOf(RoundResult(20261123, GameId.ECHO, 500, 4, 80, 60_000, false)), j.history)
        assertEquals(setOf(20261123), j.dailyDone)
    }
}
