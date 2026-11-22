package com.valueerrorx.scorr

import android.content.Context
import androidx.core.content.edit
import com.valueerrorx.scorr.domain.Game
import com.valueerrorx.scorr.domain.GameCodec

// Spielstand und Einstellungen; Backup ist aus
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("scorr", Context.MODE_PRIVATE)

    var game: Game
        get() = GameCodec.decode(sp.getString("game", null))
        set(v) = sp.edit { putString("game", GameCodec.encode(v)) }

    var rounds: Boolean
        get() = sp.getBoolean("rounds", true)
        set(v) = sp.edit { putBoolean("rounds", v) }

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit { putBoolean("haptics", v) }

    var keepOn: Boolean
        get() = sp.getBoolean("keepOn", true)
        set(v) = sp.edit { putBoolean("keepOn", v) }
}
