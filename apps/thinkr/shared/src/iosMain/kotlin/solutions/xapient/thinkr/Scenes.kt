package solutions.xapient.thinkr

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.thinkr.domain.Daily
import solutions.xapient.thinkr.domain.echo.EchoGame
import solutions.xapient.thinkr.domain.echo.EchoState
import solutions.xapient.thinkr.domain.slalom.SlalomGame
import solutions.xapient.thinkr.domain.slalom.SlalomRules
import solutions.xapient.thinkr.domain.slalom.SlalomState
import solutions.xapient.thinkr.domain.stars.StarsGame
import solutions.xapient.thinkr.domain.stars.StarsState
import solutions.xapient.thinkr.domain.switches.Pilot
import solutions.xapient.thinkr.domain.switches.SwitchesGame
import solutions.xapient.thinkr.ui.DailyItem
import solutions.xapient.thinkr.ui.GameItem
import solutions.xapient.thinkr.ui.GamesScreen
import solutions.xapient.thinkr.ui.HomeScreen
import solutions.xapient.thinkr.ui.LevelItem
import solutions.xapient.thinkr.ui.Phase
import solutions.xapient.thinkr.ui.ResultScreen
import solutions.xapient.thinkr.ui.ResultView
import solutions.xapient.thinkr.ui.ThinkrAbout
import solutions.xapient.thinkr.ui.ThinkrAccent
import solutions.xapient.thinkr.ui.ThinkrSettings

// Feste Zustaende fuer App-Store-Screenshots, wie die Roborazzi-Tests auf Android
internal enum class Scene {
    HOME, SWITCHES, STARS, SLALOM, ECHO, RESULT, PATH, GAMES, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

// Mittwoch; Serie ueber Montag und Dienstag
private const val TODAY = 20261125
private val WEEK = listOf(true, true, false, false, false, false, false)
private val LEVELS = listOf(6, 3, 4, 5, 2, 7, 4, 3, 5)
private val BESTS = listOf(4280, 1840, 2310, 3650, 0, 2120, 3380, 1460, 5210)

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AppsTheme {
        when (scene) {
            Scene.HOME -> Home()
            // Lernpfad: wie Android 1700 px bei 2,625 px/dp
            Scene.PATH -> Home(done = 1, scroll = with(LocalDensity.current) { 648.dp.roundToPx() })
            Scene.SWITCHES -> Games.Switches.Frozen(Pilot.play(SwitchesGame.start(3, 1, false), 23_350), Phase.PLAYING)
            Scene.STARS -> Games.Stars.Frozen(starsRecall(), Phase.PLAYING)
            Scene.SLALOM -> Games.Slalom.Frozen(slalomBanking(4, 3, 9_000), Phase.PLAYING)
            Scene.ECHO -> Games.Echo.Frozen(echoPlay(), Phase.PLAYING)
            Scene.RESULT -> ResultScreen(
                ResultView(Games.Echo.name, Games.Echo.icon, 4280, 6, 5, 88, 4280, newBest = true, dailyComplete = false, hasNext = false),
                null, {}, {}, {}, animate = false,
            )
            Scene.GAMES -> GamesScreen(
                Games.all.mapIndexed { i, def ->
                    val daily = def.id in Daily.pick(TODAY, Games.ids)
                    GameItem(def.id, stringResource(def.name), def.icon, LEVELS[i], BESTS[i], daily = daily, doneToday = daily && i == 0)
                },
                onPlay = {}, onBack = {},
            )
            Scene.SETTINGS -> ThinkrSettings(
                relaxed = false, onRelaxed = {}, safeColors = false, onSafeColors = {}, haptics = true, onHaptics = {}, sound = false, onSound = {},
                levels = Games.all.mapIndexed { i, def -> LevelItem(def.id, stringResource(def.name), LEVELS[i]) },
                onReset = {}, onAbout = {}, onBack = {},
            )
            Scene.ABOUT -> ThinkrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

@Composable
private fun AppsTheme(content: @Composable () -> Unit) = AappsTheme(ThinkrAccent) { content() }

// Tagesbild aus der echten Auswahl fuer heute; done = wie viele schon erledigt
@Composable
private fun Home(done: Int = 0, scroll: Int = 0) {
    val pick = Daily.pick(TODAY, Games.ids)
    HomeScreen(
        today = TODAY,
        items = pick.mapIndexed { i, id ->
            val def = Games.of(id)!!
            DailyItem(id, stringResource(def.name), def.icon, LEVELS[Games.ids.indexOf(id)], i < done)
        },
        games = Games.all.mapIndexed { i, def ->
            val daily = def.id in pick
            GameItem(def.id, stringResource(def.name), def.icon, LEVELS[i], 0, daily, daily && pick.indexOf(def.id) < done)
        },
        streak = 4,
        week = WEEK,
        haptics = null, onPlay = {}, onGames = {}, onSettings = {},
        scroll = remember { ScrollState(scroll) },
    )
}

// 1-back mitten im Spiel: 3 richtig, 1 falsch, 10 richtig = Multiplikator 3
private fun echoPlay(): EchoState {
    var s = EchoGame.start(11, 4, false)
    s = EchoGame.tick(s, s.dueAt)
    repeat(14) { i ->
        val truth = s.deck.matches[s.index]
        s = EchoGame.input(s, if (i != 3) truth else !truth, s.shownAt + 700)
    }
    return EchoGame.tick(s, s.shownAt + 450)
}

// Abfragen: zwei gefunden, ein Fehler
private fun starsRecall(): StarsState {
    var s = StarsGame.start(5, 4, false)
    repeat(2) {
        s = StarsGame.tick(s, s.recallAt)
        var t = s.round.now
        for (c in s.pass.stars) s = StarsGame.input(s, c, ++t)
        s = StarsGame.tick(s, s.nextAt)
    }
    s = StarsGame.tick(s, s.recallAt)
    val empty = (0 until s.pass.side * s.pass.side).first { it !in s.pass.stars }
    s = StarsGame.input(s, s.pass.stars[0], s.round.now + 400)
    s = StarsGame.input(s, empty, s.round.now + 500)
    s = StarsGame.input(s, s.pass.stars[2], s.round.now + 450)
    return StarsGame.tick(s, s.round.now + 300)
}

// fahren wie ein guter Spieler, dann kurz nach einem Spurwechsel: Gleiter neigt sich
private fun slalomBanking(seed: Long, level: Int, until: Long): SlalomState {
    var s = SlalomGame.start(seed, level, false)
    var t = 0L
    while (t < until) {
        t += 16
        s = SlalomGame.tick(s, t)
        val row = s.rows.firstOrNull { it.z > s.z } ?: continue
        if (s.lane != row.safe && t - s.laneAt > SlalomRules.LANE_MS * 2) s = SlalomGame.input(s, if (row.safe > s.lane) 1 else -1, t)
    }
    val dir = if (s.lane == 0) 1 else -1
    return SlalomGame.tick(SlalomGame.input(s, dir, s.round.now), s.round.now + 90)
}

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
