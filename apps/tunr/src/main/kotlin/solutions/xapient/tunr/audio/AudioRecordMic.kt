package solutions.xapient.tunr.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import solutions.xapient.tunr.domain.Frame
import solutions.xapient.tunr.domain.PitchDetector
import solutions.xapient.tunr.domain.SlidingWindow

// Mikrofon; im Test ersetzt. start/stop nur vom Main-Thread, Rueckrufe kommen auf dem Main-Thread
interface Mic {
    fun start(sink: (Frame) -> Unit, onError: () -> Unit): Boolean
    fun stop()
}

// AudioRecord auf eigenem Thread; Analyse dort, nur fertige Frames gehen an den Main-Thread
class AudioRecordMic(context: Context) : Mic {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private class Session(val record: AudioRecord, val rate: Int, val boostDb: Float) {
        @Volatile var running = true
        var thread: Thread? = null
    }

    // nur Main-Thread; Rueckrufe pruefen, ob ihre Sitzung noch gilt
    private var session: Session? = null

    override fun start(sink: (Frame) -> Unit, onError: () -> Unit): Boolean {
        stop()
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return false
        val sources = buildList {
            if (unprocessed()) add(MediaRecorder.AudioSource.UNPROCESSED)
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            add(MediaRecorder.AudioSource.MIC)
        }
        // erste Quelle/Rate, die sich oeffnen und starten laesst
        for (source in sources) {
            for (rate in RATES) {
                val record = open(source, rate) ?: continue
                val boost = if (source == MediaRecorder.AudioSource.UNPROCESSED) UNPROCESSED_BOOST_DB else 0f
                val run = Session(record, rate, boost)
                session = run
                val t = Thread({ loop(run, sink, onError) }, "tunr-mic")
                run.thread = t
                t.start()
                return true
            }
        }
        return false
    }

    // geoeffnet und aufnehmend, sonst null (Geraet gibt es frei); Berechtigung prueft start()
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun open(source: Int, rate: Int): AudioRecord? {
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return null
        val record = try {
            AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, hop(rate) * 2 * 4))
        } catch (_: Exception) {
            return null
        }
        try {
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                record.startRecording()
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) return record
            }
        } catch (_: Exception) {
            // naechster Versuch
        }
        try {
            record.stop()
        } catch (_: IllegalStateException) {
            // nie gestartet
        }
        record.release()
        return null
    }

    override fun stop() {
        val run = session ?: return
        session = null
        run.running = false
        // read() blockiert hoechstens einen Hop (~25 ms); danach gibt der Thread das Geraet frei
        try {
            run.thread?.join(JOIN_MS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun loop(run: Session, sink: (Frame) -> Unit, onError: () -> Unit) {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        } catch (_: Exception) {
            // Standard-Prioritaet reicht fuer 40 Analysen je Sekunde
        }
        val rec = run.record
        var failed = false
        try {
            val det = PitchDetector(run.rate)
            val hop = hop(run.rate)
            val win = SlidingWindow(det.size, hop)
            val pcm = ShortArray(hop)
            val deadAfter = DEAD_S * run.rate
            while (run.running) {
                var off = 0
                while (off < hop && run.running) {
                    val n = rec.read(pcm, off, hop - off)
                    if (n < 0) throw IllegalStateException("read $n")
                    off += n
                }
                if (!run.running) break
                win.push(pcm)
                // Pegel ueber dasselbe juengste Fenster, das der Detektor auswertet
                val db = win.levelDb(det.window) + run.boostDb
                val p = if (db >= LEVEL_FLOOR) det.detect(win.data) else null
                val f = Frame(p?.hz ?: 0f, db, dead = win.zeroRun >= deadAfter)
                main.post { if (session === run) sink(f) }
            }
        } catch (_: Exception) {
            failed = run.running
        } finally {
            try {
                rec.stop()
            } catch (_: IllegalStateException) {
                // nie gestartet
            }
            rec.release()
        }
        if (failed) {
            main.post {
                if (session === run) {
                    session = null
                    onError()
                }
            }
        }
    }

    private fun unprocessed(): Boolean = try {
        app.getSystemService(AudioManager::class.java)
            ?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
    } catch (_: Exception) {
        false
    }

    private companion object {
        val RATES = intArrayOf(48_000, 44_100)
        const val JOIN_MS = 300L
        // darunter lohnt keine Analyse (Pegel-Tor liegt hoeher)
        const val LEVEL_FLOOR = -70f
        // UNPROCESSED ohne Vorverstaerkung: Tor 10 dB tiefer (-60 dBFS); Annahme, am Geraet pruefen
        const val UNPROCESSED_BOOST_DB = 10f
        // so lange nur Nullen: Mikrofon gesperrt oder belegt
        const val DEAD_S = 2L
        // 25 ms je Analyse
        fun hop(rate: Int) = rate / 40
    }
}
