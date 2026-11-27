package solutions.xapient.thinkr

import solutions.xapient.thinkr.domain.Tones

// Spieltoene; Android: SoundPool, iOS: AVAudioPlayer
interface TonePlayer {
    fun play(kind: Tones.Kind)
    fun release()
}
