package solutions.xapient.thinkr

import solutions.xapient.thinkr.res.*
import org.jetbrains.compose.resources.StringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.thinkr.domain.GameId
import solutions.xapient.thinkr.domain.MiniGame
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.bounce.BounceGame
import solutions.xapient.thinkr.domain.bounce.BounceState
import solutions.xapient.thinkr.domain.bounce.Port
import solutions.xapient.thinkr.domain.drops.DropsGame
import solutions.xapient.thinkr.domain.drops.DropsState
import solutions.xapient.thinkr.domain.glance.GlanceGame
import solutions.xapient.thinkr.domain.glance.GlanceInput
import solutions.xapient.thinkr.domain.glance.GlanceState
import solutions.xapient.thinkr.domain.slalom.SlalomGame
import solutions.xapient.thinkr.domain.slalom.SlalomState
import solutions.xapient.thinkr.domain.echo.EchoGame
import solutions.xapient.thinkr.domain.echo.EchoState
import solutions.xapient.thinkr.domain.ink.InkGame
import solutions.xapient.thinkr.domain.ink.InkState
import solutions.xapient.thinkr.domain.stars.StarsGame
import solutions.xapient.thinkr.domain.stars.StarsState
import solutions.xapient.thinkr.domain.swarm.Dir
import solutions.xapient.thinkr.domain.swarm.SwarmGame
import solutions.xapient.thinkr.domain.swarm.SwarmState
import solutions.xapient.thinkr.domain.switches.SwitchesGame
import solutions.xapient.thinkr.domain.switches.SwitchesState
import solutions.xapient.thinkr.ui.GameHost
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ThinkrIcons
import solutions.xapient.thinkr.ui.bounce.BounceBoard
import solutions.xapient.thinkr.ui.drops.DropsBoard
import solutions.xapient.thinkr.ui.glance.GlanceBoard
import solutions.xapient.thinkr.ui.slalom.SlalomBoard
import solutions.xapient.thinkr.ui.echo.EchoBoard
import solutions.xapient.thinkr.ui.ink.InkBoard
import solutions.xapient.thinkr.ui.stars.StarsBoard
import solutions.xapient.thinkr.ui.swarm.SwarmBoard
import solutions.xapient.thinkr.ui.switches.SwitchesBoard

// Brett eines Spiels: zeichnet den Stand, live = Spielzeit laeuft, count = Einzaehlen (0 = aus)
typealias Board<S, I> = @Composable (state: S, live: Boolean, count: Int, onInput: (I) -> Unit) -> Unit

// Ein Spiel = Domain + Brett + Eintrag hier
class GameDef<S : Play, I>(
    val id: GameId,
    val name: StringResource,
    val icon: ImageVector,
    val game: MiniGame<S, I>,
    // jede richtige Antwort ist ein Ereignis (tick), nicht nur der Kettenaufstieg
    val tickOnHit: Boolean = false,
    // Ergebnis als „27/31" statt in Prozent
    val tally: Boolean = false,
    // false: Runde endet ueber das Spiel selbst, keine Zeitleiste
    val timed: Boolean = true,
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
        safeColors: Boolean = false,
    ) = GameHost(this, seed, level, relaxed, haptics, tones, onFinish, onQuit, safeColors = safeColors)

    // fuer Screenshots: fester Stand, keine Uhr
    @Composable
    fun Frozen(state: Play, phase: Phase, count: Int = 0) {
        @Suppress("UNCHECKED_CAST")
        GameHost(this, 0, 1, false, null, null, {}, {}, initial = state as S, initialPhase = phase, frozen = true, initialCount = count)
    }
}

object Games {
    val Echo = GameDef<EchoState, Boolean>(GameId.ECHO, Res.string.game_echo, ThinkrIcons.Echo, EchoGame) { s, live, count, input ->
        EchoBoard(s, live, count, input)
    }

    val Switches = GameDef<SwitchesState, Int>(
        GameId.SWITCHES, Res.string.game_switches, ThinkrIcons.Switches, SwitchesGame, tickOnHit = true, tally = true,
    ) { s, live, count, input ->
        SwitchesBoard(s, live, count, input)
    }

    val Stars = GameDef<StarsState, Int>(GameId.STARS, Res.string.game_stars, ThinkrIcons.Stars, StarsGame) { s, live, count, input ->
        StarsBoard(s, live, count, input)
    }

    val Ink = GameDef<InkState, Boolean>(GameId.INK, Res.string.game_ink, ThinkrIcons.Ink, InkGame) { s, live, count, input ->
        InkBoard(s, live, count, input)
    }

    val Swarm = GameDef<SwarmState, Dir>(GameId.SWARM, Res.string.game_swarm, ThinkrIcons.Swarm, SwarmGame) { s, live, count, input ->
        SwarmBoard(s, live, count, input)
    }

    val Bounce = GameDef<BounceState, Port>(GameId.BOUNCE, Res.string.game_bounce, ThinkrIcons.Bounce, BounceGame) { s, live, count, input ->
        BounceBoard(s, live, count, input)
    }

    // Runde endet erst, wenn das Becken voll ist
    val Drops = GameDef<DropsState, Int>(
        GameId.DROPS, Res.string.game_drops, ThinkrIcons.Drops, DropsGame, tickOnHit = true, timed = false,
    ) { s, live, count, input ->
        DropsBoard(s, live, count, input)
    }

    val Glance = GameDef<GlanceState, GlanceInput>(
        GameId.GLANCE, Res.string.game_glance, ThinkrIcons.Glance, GlanceGame, tickOnHit = true,
    ) { s, live, count, input ->
        GlanceBoard(s, live, count, input)
    }

    val Slalom = GameDef<SlalomState, Int>(GameId.SLALOM, Res.string.game_slalom, ThinkrIcons.Slalom, SlalomGame) { s, live, count, input ->
        SlalomBoard(s, live, count, input)
    }

    // Reihenfolge = Anzeige; neue Spiele hier eintragen
    val all: List<GameDef<*, *>> = listOf(Echo, Switches, Stars, Ink, Swarm, Bounce, Drops, Glance, Slalom)

    val ids: List<GameId> get() = all.map { it.id }

    fun of(id: GameId): GameDef<*, *>? = all.firstOrNull { it.id == id }
}
