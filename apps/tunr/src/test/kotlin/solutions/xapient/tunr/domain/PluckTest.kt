package solutions.xapient.tunr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.sqrt

class PluckTest {
    private fun rms(x: FloatArray, from: Int, n: Int): Double {
        var s = 0.0
        for (i in from until from + n) s += x[i].toDouble() * x[i]
        return sqrt(s / n)
    }

    // Grundton per YIN aus der Domain: innerhalb +-2 Cent, alle Saiten, beide Raten, auch mit anderem Kammerton
    @Test fun pitchWithinTwoCents() {
        for (rate in listOf(48_000, 44_100)) {
            val det = PitchDetector(rate)
            val all = Instrument.entries.flatMap { it.strings }.distinct()
            for (a4 in listOf(440f, 415f, 466f)) {
                for (midi in all) {
                    val hz = Notes.hz(midi, a4).toDouble()
                    val x = Pluck.render(hz, rate)
                    // kurz nach dem Anschlag und im Ausklingen
                    for (at in listOf(0.25, 1.2)) {
                        val from = (at * rate).toInt()
                        val p = det.detect(x.copyOfRange(from, from + det.size))
                        assertNotNull("MIDI $midi @$rate A4=$a4 t=$at: kein Ton", p)
                        val c = 1200 * log2(p!!.hz / hz)
                        assertTrue("MIDI $midi @$rate A4=$a4 t=$at: %.2f Cent".format(c), abs(c) <= 2.0)
                    }
                }
            }
        }
    }

    // Huellkurve faellt, kein Clipping, Laenge 2-3 s
    @Test fun envelopeDecaysWithoutClipping() {
        val rate = 48_000
        for (midi in listOf(28, 40, 45, 55, 64, 76)) {
            val x = Pluck.render(Notes.hz(midi, 440f).toDouble(), rate)
            assertEquals(Pluck.SECONDS * rate, x.size.toFloat(), 1f)
            val peak = x.maxOf { abs(it) }
            assertTrue("MIDI $midi Spitze $peak", peak <= Pluck.PEAK + 1e-4f && peak > 0.3f)
            val block = rate / 4
            val levels = (0 until x.size / block).map { rms(x, it * block, block) }
            for (i in 1 until levels.size) assertTrue("MIDI $midi Block $i steigt: $levels", levels[i] < levels[i - 1])
            assertTrue("MIDI $midi klingt nicht aus: $levels", levels.last() < levels.first() * 0.02)
        }
    }

    // Anfang und Ende ohne Sprung (kein Knacksen)
    @Test fun noClickAtStartOrEnd() {
        val rate = 44_100
        for (midi in listOf(28, 40, 64, 76)) {
            val x = Pluck.render(Notes.hz(midi, 440f).toDouble(), rate)
            assertEquals(0f, x.first(), 1e-6f)
            assertEquals(0f, x.last(), 1e-6f)
            // Einsatz: Betrag bleibt unter der weichen Rampe, erste ms leise
            val attack = (Pluck.ATTACK_S * rate).toInt()
            for (i in 0 until attack) {
                val ramp = 0.5 * (1 - kotlin.math.cos(Math.PI * i / attack))
                assertTrue("MIDI $midi Start $i", abs(x[i]) <= Pluck.PEAK * ramp + 1e-5)
            }
            for (i in 0 until rate / 1000) assertTrue("MIDI $midi erste ms", abs(x[i]) < 0.15f)
            val edge = rate / 200
            for (i in x.size - edge until x.size) assertTrue("MIDI $midi Ende $i", abs(x[i]) < 1e-3f)
        }
    }

    // erneutes Anschlagen: alter Ton blendet stetig aus, neuer startet, danach still
    @Test fun mixerFadesOldToneSmoothly() {
        val rate = 48_000
        val mix = ToneMixer(rate, fadeMs = 30f)
        mix.trigger(FloatArray(rate) { 0.5f })
        val a = FloatArray(480)
        mix.render(a)
        assertEquals(0.5f, a.last(), 1e-6f)
        mix.trigger(FloatArray(rate) { 0f })
        val b = FloatArray(rate / 10)
        mix.render(b)
        var prev = a.last()
        for (v in b) {
            assertTrue("Sprung ${abs(v - prev)}", abs(v - prev) < 0.01f)
            prev = v
        }
        assertEquals(0f, b.last(), 1e-6f)
        mix.release()
        mix.render(FloatArray(rate / 10))
        assertTrue(mix.idle)
    }

    // Hoerprobe E2 nach build/pluck (fuer die Abnahme per Ohr)
    @Test fun writeSampleWav() {
        val rate = 48_000
        val x = Pluck.render(Notes.hz(40, 440f).toDouble(), rate)
        val f = File("build/pluck/tunr-pluck-E2.wav")
        f.parentFile?.mkdirs()
        writeWav(f, x, rate)
        assertTrue(f.length() > 44)
    }

    private fun writeWav(f: File, x: FloatArray, rate: Int) {
        val data = x.size * 2
        RandomAccessFile(f, "rw").use { w ->
            w.setLength(0)
            fun le32(v: Int) { w.write(v and 0xff); w.write(v shr 8 and 0xff); w.write(v shr 16 and 0xff); w.write(v shr 24 and 0xff) }
            fun le16(v: Int) { w.write(v and 0xff); w.write(v shr 8 and 0xff) }
            w.writeBytes("RIFF"); le32(36 + data); w.writeBytes("WAVE")
            w.writeBytes("fmt "); le32(16); le16(1); le16(1); le32(rate); le32(rate * 2); le16(2); le16(16)
            w.writeBytes("data"); le32(data)
            val bytes = ByteArray(data)
            x.forEachIndexed { i, v ->
                val s = (v.coerceIn(-1f, 1f) * 32767f).toInt()
                bytes[2 * i] = (s and 0xff).toByte()
                bytes[2 * i + 1] = (s shr 8 and 0xff).toByte()
            }
            w.write(bytes)
        }
    }
}
