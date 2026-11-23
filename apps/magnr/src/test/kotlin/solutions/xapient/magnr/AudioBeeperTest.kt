package solutions.xapient.magnr

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import solutions.xapient.magnr.sensor.AudioBeeper

// Hoechstens ein Ton-Thread, stop blockiert nicht
@RunWith(RobolectricTestRunner::class)
class AudioBeeperTest {
    private fun beepThreads() = Thread.getAllStackTraces().keys.count { it.name == "magnr-beep" && it.isAlive }

    @Test fun rapidStartStopKeepsOneThread() {
        val b = AudioBeeper()
        b.strength = 0.6f
        repeat(50) {
            b.start()
            val t0 = System.nanoTime()
            b.stop()
            assertTrue("stop blockiert", System.nanoTime() - t0 < 20_000_000L)
            b.start()
            assertTrue("mehrere Threads", beepThreads() <= 1)
        }
        b.stop()
        // laeuft aus und beendet sich
        val until = System.currentTimeMillis() + 3_000
        while (beepThreads() > 0 && System.currentTimeMillis() < until) Thread.sleep(10)
        assertTrue(beepThreads() == 0)
    }
}
