package solutions.xapient.thinkr

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.thinkr.domain.GameId
import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.echo.EchoGame
import solutions.xapient.thinkr.domain.echo.EchoState
import solutions.xapient.thinkr.ui.GameHost
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ThinkrIcons
import solutions.xapient.thinkr.ui.echo.EchoBoard

// Brett eines Spiels: zeichnet den Stand, live = Spielzeit laeuft, count = Einzaehlen (0 = aus)
typealias Board<S, I> = @Composable (state: S, live: Boolean, count: Int, onInput: (I) -> Unit) -> Unit

// Ein Spiel = Domain + Brett + Eintrag hier
class GameDef<S : Play, I>(
    val id: GameId,
    @param:StringRes val name: Int,
    val icon: ImageVector,
    val game: MiniGame<S, I>,
    val board: Board<S, I>,
) {
    fun startLevel(saved: Int): Int = game.startLevel(saved)

    // Typen bleiben hier gebunden, die Registry kennt nur GameDef<*, *>
    @Composable
    fun Host(
        seed: Long,
        level: Int,
        relaxed: Boolean,
        haptics: Haptics?,
        tones: TonePlayer?,
        onFinish: (Play) -> Unit,
        onQuit: () -> Unit,
    ) = GameHost(this, seed, level, relaxed, haptics, tones, onFinish, onQuit)

    // fuer Screenshots: fester Stand, keine Uhr
    @Composable
    fun Frozen(state: Play, phase: Phase, count: Int = 0) {
        @Suppress("UNCHECKED_CAST")
        GameHost(this, 0, 1, false, null, null, {}, {}, initial = state as S, initialPhase = phase, frozen = true, initialCount = count)
    }
}

object Games {
    val Echo = GameDef<EchoState, Boolean>(GameId.ECHO, R.string.game_echo, ThinkrIcons.Echo, EchoGame) { s, live, count, input ->
        EchoBoard(s, live, count, input)
    }

    // Reihenfolge = Anzeige; neue Spiele hier eintragen
    val all: List<GameDef<*, *>> = listOf(Echo)

    val ids: List<GameId> get() = all.map { it.id }

    fun of(id: GameId): GameDef<*, *>? = all.firstOrNull { it.id == id }
}
