package solutions.xapient.magnr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import solutions.xapient.magnr.domain.BeepSynth
import solutions.xapient.magnr.domain.Beeper
import kotlin.concurrent.Volatile

/**
 * Pieptoene ueber AVAudioEngine: drei Puffer zu 10 ms im Wechsel, jeder fertig gespielte wird neu berechnet und
 * wieder eingereiht (wie der AudioTrack-Stream auf Android). Ambient: der Stummschalter gilt, andere Audio-Apps laufen weiter.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBeeper : Beeper {
    @Volatile override var strength = 0f

    private val engine = AVAudioEngine()
    private val player = AVAudioPlayerNode()
    private val format = AVAudioFormat(standardFormatWithSampleRate = RATE.toDouble(), channels = 1u)
    private val synth = BeepSynth(RATE)
    private val block = ShortArray(BLOCK)
    private val buffers = List(BUFFERS) { AVAudioPCMBuffer(pCMFormat = format, frameCapacity = BLOCK.toUInt()) }

    // jede Wiedergabe hat eine eigene Nummer: Rueckrufe einer gestoppten reihen nichts mehr ein
    @Volatile private var generation = 0
    @Volatile private var running = false

    init {
        engine.attachNode(player)
        engine.connect(player, engine.mainMixerNode, format)
    }

    override fun start() {
        if (running) return
        AVAudioSession.sharedInstance().apply {
            setCategory(AVAudioSessionCategoryAmbient, null)
            setActive(true, null)
        }
        // kein Audio moeglich: still bleiben
        if (!engine.running && !engine.startAndReturnError(null)) return
        running = true
        val gen = ++generation
        player.play()
        buffers.forEach { schedule(it, gen) }
    }

    override fun stop() {
        if (!running) return
        running = false
        generation++
        player.stop()
        engine.pause()
    }

    private fun schedule(buffer: AVAudioPCMBuffer, gen: Int) {
        if (!running || gen != generation) return
        val out = buffer.floatChannelData?.get(0) ?: return
        synth.render(block, BLOCK, strength)
        for (i in 0 until BLOCK) out[i] = block[i] / 32768f
        buffer.frameLength = BLOCK.toUInt()
        player.scheduleBuffer(buffer) { schedule(buffer, gen) }
    }

    private companion object {
        const val RATE = 48_000
        // 10 ms je Puffer
        const val BLOCK = RATE / 100
        const val BUFFERS = 3
    }
}
