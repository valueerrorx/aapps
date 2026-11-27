package solutions.xapient.loudr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Foundation.NSLock
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import solutions.xapient.loudr.domain.Reading
import solutions.xapient.loudr.domain.SoundMeter
import kotlin.concurrent.Volatile

/**
 * Mikrofon ueber AVAudioEngine (Eingangs-Tap, Modus Measurement = ohne automatische Verstaerkung).
 * Der Tap fuettert das SoundMeter auf dem Audio-Thread, die Oberflaeche liest die letzte Momentaufnahme.
 * Unterbrechung (Anruf, andere App nimmt auf) beendet die Messung wie ein Aufnahmefehler auf Android.
 */
@OptIn(ExperimentalForeignApi::class)
class IosMicMeter(private val core: SoundMeter = SoundMeter()) : LevelMeter {
    private val lock = NSLock()
    private var engine: AVAudioEngine? = null
    @Volatile private var latest: Reading? = null
    @Volatile private var running = false
    @Volatile override var onFailure: (() -> Unit)? = null
    @Volatile override var onSilenced: ((Boolean) -> Unit)? = null
    private var pcm = ShortArray(0)

    init {
        NSNotificationCenter.defaultCenter.addObserverForName(AVAudioSessionInterruptionNotification, null, NSOperationQueue.mainQueue) { _ ->
            if (running) {
                stop()
                onFailure?.invoke()
            }
        }
    }

    override fun start(): Boolean {
        if (running) return true
        if (!granted()) return false
        val audio = AVAudioSession.sharedInstance()
        if (!audio.setCategory(AVAudioSessionCategoryRecord, null)) return false
        audio.setMode(AVAudioSessionModeMeasurement, null)
        if (!audio.setActive(true, null)) return false
        val e = AVAudioEngine()
        val input = e.inputNode
        val format = input.outputFormatForBus(0u)
        val rate = format.sampleRate.toInt()
        if (rate <= 0 || format.channelCount == 0u) {
            audio.setActive(false, null)
            return false
        }
        update { core.restart(rate, SoundMeter.REF_UNPROCESSED) }
        // 20-ms-Bloecke wie auf Android; iOS liefert oft groessere
        input.installTapOnBus(0u, (rate / 50).toUInt(), format) { buf, _ ->
            val b = buf ?: return@installTapOnBus
            val ch = b.floatChannelData?.get(0) ?: return@installTapOnBus
            val n = b.frameLength.toInt()
            if (!running || n <= 0) return@installTapOnBus
            if (pcm.size < n) pcm = ShortArray(n)
            for (i in 0 until n) pcm[i] = (ch[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            update { if (running) core.feed(pcm, n) }
        }
        e.prepare()
        running = true
        if (!e.startAndReturnError(null)) {
            running = false
            input.removeTapOnBus(0u)
            audio.setActive(false, null)
            return false
        }
        engine = e
        return true
    }

    override fun stop() {
        if (!running) return
        running = false
        engine?.let {
            it.inputNode.removeTapOnBus(0u)
            it.stop()
        }
        engine = null
        AVAudioSession.sharedInstance().setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
        update { core.pause() }
    }

    override fun reset() = update { core.reset() }

    override fun reading(): Reading = latest ?: locked { core.reading() }.also { latest = it }

    override fun save(): DoubleArray = locked { core.save() }

    override fun restore(state: DoubleArray) = update { core.restore(state) }

    private inline fun <T> locked(f: () -> T): T {
        lock.lock()
        try {
            return f()
        } finally {
            lock.unlock()
        }
    }

    private inline fun update(f: () -> Unit) = locked {
        f()
        latest = core.reading()
    }

    companion object {
        fun granted(): Boolean = AVAudioApplication.sharedInstance().recordPermission == AVAudioApplicationRecordPermissionGranted
    }
}
