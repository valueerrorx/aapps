package solutions.xapient.spinr

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create
import solutions.xapient.spinr.domain.TickSound
import kotlin.random.Random

// Klick pro Stift: kleiner Vorrat an Playern, damit schnelle Klicks sich ueberlappen; Ambient = Stummschalter gilt
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosTickPlayer : TickPlayer {
    private val players: List<AVAudioPlayer>
    private var next = 0

    init {
        AVAudioSession.sharedInstance().apply {
            setCategory(AVAudioSessionCategoryAmbient, null)
            setActive(true, null)
        }
        val bytes = TickSound.wav(TickSound.pcm())
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        players = List(3) { AVAudioPlayer(data = data, error = null) }.onEach {
            it.enableRate = true
            it.volume = 0.55f
            it.prepareToPlay()
        }
    }

    // leicht wechselnde Tonhoehe klingt weniger mechanisch (wie Android)
    override fun play() {
        val p = players[next]
        next = (next + 1) % players.size
        p.currentTime = 0.0
        p.rate = 0.94f + Random.nextFloat() * 0.12f
        p.play()
    }

    override fun release() = players.forEach { it.stop() }
}
