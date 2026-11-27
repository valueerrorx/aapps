package solutions.xapient.thinkr

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
import solutions.xapient.thinkr.domain.Tones

// Spieltoene: je Klang ein vorbereiteter Player; Ambient = Stummschalter gilt, andere Audio-Apps laufen weiter
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosTonePlayer : TonePlayer {
    private val players: Map<Tones.Kind, AVAudioPlayer>

    init {
        AVAudioSession.sharedInstance().apply {
            setCategory(AVAudioSessionCategoryAmbient, null)
            setActive(true, null)
        }
        players = Tones.Kind.entries.associateWith { kind ->
            val bytes = Tones.wav(Tones.pcm(kind))
            val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
            AVAudioPlayer(data = data, error = null).apply {
                volume = 0.5f
                prepareToPlay()
            }
        }
    }

    override fun play(kind: Tones.Kind) {
        val p = players[kind] ?: return
        p.currentTime = 0.0
        p.play()
    }

    override fun release() = players.values.forEach { it.stop() }
}
