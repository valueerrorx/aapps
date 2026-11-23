package solutions.xapient.loudr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
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
    // System liefert nur Stille (Datenschutzschalter, andere App hat Vorrang), kommt vom Audio-Thread
    var onSilenced: ((Boolean) -> Unit)?
}

// Legt einen Recorder an: Quelle, Rate, Puffer in Bytes; im Test ersetzt
typealias RecordFactory = (source: Int, rate: Int, bytes: Int) -> AudioRecord

// AudioRecord auf eigenem Thread; der Thread fuettert das SoundMeter, die Oberflaeche liest die letzte Momentaufnahme
class MicMeter(
    context: Context,
    private val core: SoundMeter = SoundMeter(),
    private val create: RecordFactory = ::record,
) : LevelMeter {
    private val app = context.applicationContext

    private class Session(val record: AudioRecord, val rate: Int, val refDb: Double) {
        @Volatile var running = true
        @Volatile var clientSilenced = false
        val sessionId = record.audioSessionId
        var thread: Thread? = null
        @Volatile var callback: AudioManager.AudioRecordingCallback? = null
    }

    @Volatile private var session: Session? = null
    @Volatile private var latest: Reading? = null
    @Volatile override var onFailure: (() -> Unit)? = null
    @Volatile override var onSilenced: ((Boolean) -> Unit)? = null

    override fun start(): Boolean {
        if (session != null) return true
        val run = open() ?: return false
        update { core.restart(run.rate, run.refDb) }
        session = run
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) watch(run)
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
        update { core.pause() }
    }

    override fun reset() = update { core.reset() }

    override fun reading(): Reading = latest ?: synchronized(core) { core.reading() }.also { latest = it }

    override fun save(): DoubleArray = synchronized(core) { core.save() }

    override fun restore(state: DoubleArray) = update { core.restore(state) }

    private inline fun update(f: () -> Unit) {
        synchronized(core) {
            f()
            latest = core.reading()
        }
    }

    // nur der aktuelle Lauf schreibt; ein alter Thread nach stop()/start() verwirft seine Bloecke
    private inline fun feed(run: Session, f: () -> Unit) {
        synchronized(core) {
            if (run.running && session === run) {
                f()
                latest = core.reading()
            }
        }
    }

    // Quellen der Reihe nach, je Quelle 48 und 44,1 kHz; offen ist eine Quelle erst, wenn sie aufnimmt
    private fun open(): Session? {
        if (app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return null
        val am = app.getSystemService(AudioManager::class.java)
        val unprocessed = am?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        for (source in sources(unprocessed)) for (rate in RATES) {
            val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (min <= 0) continue
            val rec = try {
                create(source, rate, maxOf(min, rate / 5 * 2))
            } catch (_: Exception) {
                continue
            }
            if (rec.state == AudioRecord.STATE_INITIALIZED && begin(rec)) return Session(rec, rec.sampleRate, refDb(source))
            rec.release()
        }
        return null
    }

    private fun begin(rec: AudioRecord): Boolean = try {
        rec.startRecording()
        rec.recordingState == AudioRecord.RECORDSTATE_RECORDING
    } catch (_: Exception) {
        false
    }

    // ab Android 10 meldet das System direkt, wenn es uns stummschaltet
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun watch(run: Session) {
        val am = app.getSystemService(AudioManager::class.java) ?: return
        val cb = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
                run.clientSilenced = configs.any { it.clientAudioSessionId == run.sessionId && it.isClientSilenced }
            }
        }
        run.callback = cb
        am.registerAudioRecordingCallback(cb, null)
    }

    private fun unwatch(run: Session) {
        val cb = run.callback ?: return
        run.callback = null
        app.getSystemService(AudioManager::class.java)?.unregisterAudioRecordingCallback(cb)
    }

    private fun loop(run: Session) {
        val rec = run.record
        var failed = false
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            // 20-ms-Bloecke: Anzeige frisch, Sperre kurz
            val buf = ShortArray(run.rate / 50)
            var zeros = 0
            var silenced = false
            while (run.running) {
                val n = rec.read(buf, 0, buf.size)
                check(n >= 0) { "read $n" }
                if (n == 0) continue
                // ein echtes Mikrofon rauscht immer; lauter Nullen = vom System stummgeschaltet
                val quiet = run.clientSilenced || allZero(buf, n)
                zeros = if (quiet) zeros + n else 0
                val now = run.clientSilenced || zeros >= run.rate / 2
                if (now != silenced) {
                    silenced = now
                    if (run.running && session === run) onSilenced?.invoke(now)
                }
                // Stille vom System zaehlt nicht: Min, Leq und Verlauf bleiben stehen
                if (!quiet) feed(run) { core.feed(buf, n) }
            }
        } catch (_: Exception) {
            failed = run.running
        } finally {
            unwatch(run)
            try {
                rec.stop()
            } catch (_: IllegalStateException) {
                // nie gestartet
            }
            rec.release()
        }
        if (!failed) return
        run.running = false
        val current = synchronized(core) {
            (session === run).also {
                if (it) {
                    session = null
                    core.pause()
                    latest = core.reading()
                }
            }
        }
        if (current) onFailure?.invoke()
    }

    companion object {
        private val RATES = intArrayOf(48_000, 44_100)
        private const val JOIN_MS = 500L

        // UNPROCESSED wenn gemeldet, dann VOICE_RECOGNITION (beide ohne AGC), zuletzt MIC
        fun sources(unprocessed: Boolean): List<Int> = listOfNotNull(
            MediaRecorder.AudioSource.UNPROCESSED.takeIf { unprocessed },
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
        )

        // Bezugspegel laut CDD je Quelle; MIC ohne eigene Vorgabe wie VOICE_RECOGNITION
        fun refDb(source: Int) =
            if (source == MediaRecorder.AudioSource.UNPROCESSED) SoundMeter.REF_UNPROCESSED else SoundMeter.REF_VOICE

        private fun allZero(buf: ShortArray, n: Int): Boolean {
            for (i in 0 until n) if (buf[i].toInt() != 0) return false
            return true
        }

        @SuppressLint("MissingPermission")
        private fun record(source: Int, rate: Int, bytes: Int) =
            AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bytes)
    }
}
