package com.valueerrorx.spinr

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.valueerrorx.spinr.domain.TickSound
import java.io.File
import kotlin.random.Random

// Klick pro Stift ueber SoundPool (Medienlautstaerke); der Klang wird einmal erzeugt und im Cache abgelegt
class TickPlayer(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private var loaded = false
    private val id: Int

    init {
        val file = File(context.cacheDir, "tick-1.wav")
        // erst fertig schreiben, dann umbenennen: kein halber Klang nach Abbruch
        if (!file.exists()) {
            val tmp = File(context.cacheDir, "tick-1.tmp")
            tmp.writeBytes(TickSound.wav(TickSound.pcm()))
            tmp.renameTo(file)
        }
        pool.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
        id = pool.load(file.path, 1)
    }

    // leicht wechselnde Tonhoehe klingt weniger mechanisch
    fun play() {
        if (loaded) pool.play(id, 0.55f, 0.55f, 1, 0, 0.94f + Random.nextFloat() * 0.12f)
    }

    fun release() = pool.release()
}
