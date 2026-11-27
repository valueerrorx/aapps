package solutions.xapient.roundr

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import solutions.xapient.roundr.domain.Cue
import solutions.xapient.roundr.domain.Phase
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Toene wie Android (erzeugt, nicht gebuendelt): im Vordergrund per AVAudioPlayer, Kategorie Playback und mischend
 * (klingt auch bei Stummschalter, Musik laeuft weiter); fuer Mitteilungen als WAV in Library/Sounds.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSignals {
    private val scope = MainScope()
    private val players = HashMap<String, AVAudioPlayer>()
    private val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)

    init {
        AVAudioSession.sharedInstance().apply {
            setCategory(AVAudioSessionCategoryPlayback, AVAudioSessionCategoryOptionMixWithOthers, null)
            setActive(true, null)
        }
        val dir = soundsDir()
        KEYS.forEach { key ->
            val bytes = wav(pcm(notes(key)))
            val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
            AVAudioPlayer(data = data, error = null).also {
                it.prepareToPlay()
                players[key] = it
            }
            // Mitteilungs-Toene muessen als Datei vorliegen; nur Abschnitts-Signale, kein 3-2-1
            if (dir != null && key != "count") data.writeToFile("$dir/${fileName(key)}", true)
        }
    }

    fun play(cue: Cue, sound: Boolean, vibrate: Boolean) {
        if (sound) players[key(cue)]?.let {
            it.currentTime = 0.0
            it.play()
        }
        if (vibrate) buzz(cue)
    }

    // iOS kennt keine Muster: Stoesse in Folge, iPads ohne Vibrationsmotor bleiben still
    private fun buzz(cue: Cue) {
        when (cue) {
            is Cue.Count -> light.impactOccurred()
            is Cue.Start -> AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            Cue.Done -> scope.launch {
                repeat(2) {
                    AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
                    delay(600)
                }
            }
        }
    }

    private fun soundsDir(): String? {
        val lib = NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, true).firstOrNull() as? String ?: return null
        val dir = "$lib/Sounds"
        NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
        return dir
    }

    companion object {
        private val KEYS = listOf("count", "work", "rest", "done")

        fun key(cue: Cue): String = when (cue) {
            is Cue.Count -> "count"
            is Cue.Start -> if (cue.phase == Phase.WORK) "work" else "rest"
            Cue.Done -> "done"
        }

        // Dateiname fuer UNNotificationSound
        fun fileName(key: String) = "roundr-$key.wav"

        // Toene als (Frequenz Hz, Dauer ms); 0 Hz = Stille; wie Android
        private fun notes(key: String): List<Pair<Double, Int>> = when (key) {
            "count" -> listOf(880.0 to 110)
            "work" -> listOf(1318.5 to 420)
            "rest" -> listOf(659.3 to 160, 0.0 to 50, 523.3 to 300)
            else -> listOf(784.0 to 140, 0.0 to 30, 1046.5 to 140, 0.0 to 30, 1568.0 to 420)
        }

        private const val RATE = 44_100

        private fun pcm(notes: List<Pair<Double, Int>>): ShortArray {
            val out = ShortArray(notes.sumOf { it.second } * RATE / 1000)
            var at = 0
            for ((hz, ms) in notes) {
                val n = ms * RATE / 1000
                // weiche Flanken gegen Knacksen
                val fade = min(n / 4, RATE * 8 / 1000)
                if (hz > 0) for (i in 0 until n) {
                    val env = when {
                        i < fade -> i.toDouble() / fade
                        i > n - fade -> (n - i).toDouble() / fade
                        else -> 1.0
                    }
                    val x = 2 * PI * hz * i / RATE
                    out[at + i] = ((sin(x) * 0.8 + sin(2 * x) * 0.2) * env * 0.7 * Short.MAX_VALUE).toInt().toShort()
                }
                at += n
            }
            return out
        }

        private fun wav(samples: ShortArray): ByteArray {
            val data = samples.size * 2
            val out = ByteArray(44 + data)
            fun str(at: Int, s: String) = s.forEachIndexed { i, ch -> out[at + i] = ch.code.toByte() }
            fun int(at: Int, v: Int) = repeat(4) { out[at + it] = (v ushr (8 * it)).toByte() }
            fun short(at: Int, v: Int) = repeat(2) { out[at + it] = (v ushr (8 * it)).toByte() }
            str(0, "RIFF"); int(4, 36 + data); str(8, "WAVE")
            str(12, "fmt "); int(16, 16); short(20, 1); short(22, 1)
            int(24, RATE); int(28, RATE * 2); short(32, 2); short(34, 16)
            str(36, "data"); int(40, data)
            samples.forEachIndexed { i, s -> short(44 + 2 * i, s.toInt()) }
            return out
        }
    }
}
