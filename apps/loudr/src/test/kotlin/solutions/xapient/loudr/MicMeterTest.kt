package solutions.xapient.loudr

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAudioRecord
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// Recorder-Ablauf mit ShadowAudioRecord: Rueckfall, Fehlerpfad, Stummschaltung, Uebersteuerung
@RunWith(RobolectricTestRunner::class)
class MicMeterTest {
    private val app = RuntimeEnvironment.getApplication()
    private val tried = mutableListOf<Int>()
    private var released = 0
    private var meter: MicMeter? = null

    @Before fun grant() {
        shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
    }

    @After fun stop() {
        meter?.stop()
        ShadowAudioRecord.clearSource()
    }

    // liefert je Aufruf gen(i) fuer Sample i, oder einen Fehlercode
    private fun source(error: Int = 0, gen: (Long) -> Short = { 0.toShort() }) = object : ShadowAudioRecord.AudioRecordSource {
        var i = 0L
        override fun readInShortArray(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int, isBlocking: Boolean): Int {
            if (error != 0) return error
            for (k in 0 until sizeInShorts) audioData[offsetInShorts + k] = gen(i++)
            Thread.sleep(1)
            return sizeInShorts
        }
    }

    // Recorder, dessen Start fuer die genannten Quellen scheitert
    private fun factory(failing: Set<Int>): RecordFactory = { src, rate, bytes ->
        tried += src
        if (src in failing) {
            object : AudioRecord(src, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bytes) {
                override fun startRecording() = throw IllegalStateException("belegt")
                override fun release() {
                    released++
                    super.release()
                }
            }
        } else {
            AudioRecord(src, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bytes)
        }
    }

    private fun meter(failing: Set<Int> = emptySet()) = MicMeter(app, create = factory(failing)).also { meter = it }

    private fun waitFor(what: String, f: () -> Boolean) {
        val end = System.nanoTime() + 5_000_000_000L
        while (!f()) {
            check(System.nanoTime() < end) { "Zeitueberschreitung: $what" }
            Thread.sleep(5)
        }
    }

    // nicht fertige Quellen werden freigegeben, die naechste kommt dran
    @Test fun fallsBackToNextSource() {
        ShadowAudioRecord.setSource(source { 1000.toShort() })
        val m = meter(failing = setOf(MediaRecorder.AudioSource.VOICE_RECOGNITION))
        assertTrue(m.start())
        val vr = tried.indexOf(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        assertTrue(vr >= 0)
        assertEquals(MediaRecorder.AudioSource.MIC, tried.last())
        // je Rate ein gescheiterter Versuch, alle freigegeben
        assertEquals(tried.count { it == MediaRecorder.AudioSource.VOICE_RECOGNITION }, released)
        waitFor("Pegel") { m.reading().live }
    }

    @Test fun allSourcesFailing() {
        val m = meter(failing = MicMeter.sources(true).toSet())
        assertFalse(m.start())
        assertEquals(tried.size, released)
    }

    // Lesefehler: Recorder frei, Fehler gemeldet, Messung aus
    @Test fun readErrorReportsFailure() {
        ShadowAudioRecord.setSource(source(error = AudioRecord.ERROR_INVALID_OPERATION))
        val m = meter()
        val failed = CountDownLatch(1)
        m.onFailure = { failed.countDown() }
        assertTrue(m.start())
        assertTrue(failed.await(5, TimeUnit.SECONDS))
        assertFalse(m.reading().live)
        // danach wieder startbar
        ShadowAudioRecord.setSource(source { 1000.toShort() })
        assertTrue(m.start())
    }

    // lauter Nullen: nach etwa 0,5 s stummgeschaltet, Statistik bleibt leer
    @Test fun zerosMeanSilenced() {
        ShadowAudioRecord.setSource(source())
        val m = meter()
        val silenced = CountDownLatch(1)
        m.onSilenced = { if (it) silenced.countDown() }
        assertTrue(m.start())
        assertTrue(silenced.await(5, TimeUnit.SECONDS))
        val r = m.reading()
        assertTrue(r.leq.isNaN())
        assertEquals(0, r.history.size)
    }

    // Vollausschlag: Reading meldet Uebersteuerung
    @Test fun fullScaleIsOverload() {
        ShadowAudioRecord.setSource(source { i -> (if ((i / 24) % 2 == 0L) 32767 else -32767).toShort() })
        val m = meter()
        assertTrue(m.start())
        waitFor("Uebersteuerung") { m.reading().let { it.live && it.overload } }
    }
}
