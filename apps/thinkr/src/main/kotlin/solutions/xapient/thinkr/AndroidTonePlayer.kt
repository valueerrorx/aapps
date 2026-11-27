package solutions.xapient.thinkr

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import solutions.xapient.thinkr.domain.Tones
import java.io.File

// Spieltoene ueber SoundPool (Medienlautstaerke); Klaenge werden einmal erzeugt und im Cache abgelegt
class AndroidTonePlayer(context: Context) : TonePlayer {
    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val loaded = HashSet<Int>()
    private val ids: Map<Tones.Kind, Int>

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(loaded) { loaded += id } }
        ids = Tones.Kind.entries.associateWith { kind ->
            val name = "tone-${kind.name.lowercase()}-$VERSION"
            val file = File(context.cacheDir, "$name.wav")
            // erst fertig schreiben, dann umbenennen: kein halber Klang nach Abbruch
            if (!file.exists()) {
                val tmp = File(context.cacheDir, "$name.tmp")
                tmp.writeBytes(Tones.wav(Tones.pcm(kind)))
                tmp.renameTo(file)
            }
            pool.load(file.path, 1)
        }
    }

    override fun play(kind: Tones.Kind) {
        val id = ids[kind] ?: return
        if (synchronized(loaded) { id in loaded }) pool.play(id, 0.5f, 0.5f, 1, 0, 1f)
    }

    override fun release() = pool.release()

    private companion object {
        // neue Klaenge = neue Datei, alte im Cache verfallen von selbst
        const val VERSION = 1
    }
}
