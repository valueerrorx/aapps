package solutions.xapient.loudr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import solutions.xapient.loudr.domain.Reading
import solutions.xapient.loudr.domain.SoundMeter

// Pegelquelle fuer die Oberflaeche; im Test ersetzt
interface LevelMeter {
    fun start(): Boolean
    fun stop()
    fun reset()
    fun reading(): Reading
    fun save(): DoubleArray
    fun restore(state: DoubleArray)
    // Aufnahme abgebrochen, kommt vom Audio-Thread
    var onFailure: (() -> Unit)?
}

// AudioRecord auf eigenem Thread; der Thread fuettert das SoundMeter, die Oberflaeche liest die letzte Momentaufnahme
class MicMeter(context: Context, private val core: SoundMeter = SoundMeter()) : LevelMeter {
    private val app = context.applicationContext

    private class Session(val record: AudioRecord, val rate: Int, val refDb: Double) {
        @Volatile var running = true
        var thread: Thread? = null
    }

    @Volatile private var session: Session? = null
    @Volatile private var latest: Reading? = null
    override var onFailure: (() -> Unit)? = null

    override fun start(): Boolean {
        if (session != null) return true
        val run = open() ?: return false
        publish { core.restart(run.rate, run.refDb) }
        session = run
        val t = Thread({ loop(run) }, "loudr-mic")
        run.thread = t
        t.start()
        return true
    }

    override fun stop() {
        val run = session ?: return
        session = null
        run.running = false
        // read() kehrt nach hoechstens einem Block zurueck; danach ist das Mikrofon frei
        try {
            run.thread?.join(JOIN_MS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        publish { core.pause() }
    }

    override fun reset() = publish { core.reset() }

    override fun reading(): Reading = latest ?: synchronized(core) { core.reading() }.also { latest = it }

    override fun save(): DoubleArray = synchronized(core) { core.save() }

    override fun restore(state: DoubleArray) = publish { core.restore(state) }

    private inline fun publish(f: () -> Unit) {
        synchronized(core) {
            f()
            latest = core.reading()
        }
    }

    // UNPROCESSED wenn gemeldet, sonst VOICE_RECOGNITION: beide ohne AGC/Rauschunterdrueckung
    @SuppressLint("MissingPermission")
    private fun open(): Session? {
        if (app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return null
        val am = app.getSystemService(AudioManager::class.java)
        val unprocessed = am?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val sources = if (unprocessed) {
            listOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.VOICE_RECOGNITION)
        } else {
            listOf(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        }
        for (source in sources) for (rate in RATES) {
            val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (min <= 0) continue
            val rec = try {
                AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, rate / 5 * 2))
            } catch (_: Exception) {
                continue
            }
            if (rec.state == AudioRecord.STATE_INITIALIZED) return Session(rec, rec.sampleRate, refDb(source))
            rec.release()
        }
        return null
    }

    private fun loop(run: Session) {
        val rec = run.record
        var failed = false
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            rec.startRecording()
            check(rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Mikrofon belegt" }
            // 20-ms-Bloecke: Anzeige frisch, Sperre kurz
            val buf = ShortArray(run.rate / 50)
            while (run.running) {
                val n = rec.read(buf, 0, buf.size)
                check(n >= 0) { "read $n" }
                if (n == 0 || !run.running) continue
                publish { core.feed(buf, n) }
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
            run.running = false
            if (session === run) {
                session = null
                publish { core.pause() }
            }
            onFailure?.invoke()
        }
    }

    private companion object {
        // Bezugspegel laut CDD je Quelle
        fun refDb(source: Int) =
            if (source == MediaRecorder.AudioSource.UNPROCESSED) SoundMeter.REF_UNPROCESSED else SoundMeter.REF_VOICE

        val RATES = intArrayOf(48_000, 44_100)
        const val JOIN_MS = 500L
    }
}
