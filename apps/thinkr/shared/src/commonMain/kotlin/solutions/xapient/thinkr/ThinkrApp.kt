package solutions.xapient.thinkr

import solutions.xapient.aapps.core.ui.BackEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.thinkr.domain.Daily
import solutions.xapient.thinkr.domain.GameId
import solutions.xapient.thinkr.domain.Play
import solutions.xapient.thinkr.domain.RoundResult
import solutions.xapient.thinkr.domain.Streak
import solutions.xapient.thinkr.ui.GameItem
import solutions.xapient.thinkr.ui.GamesScreen
import solutions.xapient.thinkr.ui.HomeScreen
import solutions.xapient.thinkr.ui.LevelItem
import solutions.xapient.thinkr.ui.ResultScreen
import solutions.xapient.thinkr.ui.ResultView
import solutions.xapient.thinkr.ui.ThinkrAbout
import solutions.xapient.thinkr.ui.ThinkrAccent
import solutions.xapient.thinkr.ui.ThinkrSettings
import kotlin.random.Random

private enum class Screen { HOME, GAMES, PLAY, RESULT, SETTINGS, ABOUT }

// Laufende Runde: gilt fuer den Tag, an dem sie begann; daily = aus dem Tagestraining gestartet
private data class Session(
    val id: GameId,
    val daily: Boolean,
    val seed: Long,
    val level: Int,
    val relaxed: Boolean,
    val safeColors: Boolean,
    val day: Int,
    val back: Screen,
)

@Composable
// clock = heutiger Tag als yyyymmdd; newTonePlayer je Plattform
fun ThinkrApp(prefs: Prefs, newTonePlayer: () -> TonePlayer, versionName: String, clock: () -> Int = ::todayDay) {
    var journal by remember { mutableStateOf(prefs.journal) }
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var soundOn by remember { mutableStateOf(prefs.sound) }
    var relaxed by remember { mutableStateOf(prefs.relaxed) }
    var safeColors by remember { mutableStateOf(prefs.safeColors) }
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    // Tageswechsel erst beim naechsten Oeffnen, nie mitten im Spiel
    var today by remember { mutableIntStateOf(clock()) }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { today = clock() }

    var session by remember { mutableStateOf<Session?>(null) }
    var result by remember { mutableStateOf<ResultView?>(null) }

    val daily = remember(today) { Daily.pick(today, Games.ids) }
    val done = journal.doneToday(today, daily)
    // Ein laufendes Spiel wird nicht gespeichert: nach Prozess-Tod zurueck nach Heute
    val shown = if ((screen == Screen.PLAY && session == null) || (screen == Screen.RESULT && result == null)) Screen.HOME else screen

    val tones = remember(soundOn) { if (soundOn) newTonePlayer() else null }
    DisposableEffect(tones) { onDispose { tones?.release() } }

    fun start(id: GameId, fromDaily: Boolean, back: Screen) {
        val def = Games.of(id) ?: return
        val saved = journal.progress(id, relaxed).level
        session = Session(id, fromDaily, Random.nextLong(), def.startLevel(saved), relaxed, safeColors, today, back)
        screen = Screen.PLAY
    }

    fun finish(s: Session, play: Play) {
        val def = Games.of(s.id) ?: return
        val r = play.round
        val res = RoundResult(s.day, s.id, r.score, play.level, r.accuracy, r.now, s.relaxed)
        val before = journal.progress(s.id, s.relaxed)
        val dailyIds = Daily.pick(s.day, Games.ids)
        val wasDone = s.day in journal.dailyDone
        val next = journal.record(res, dailyIds)
        journal = next
        prefs.journal = next
        val open = dailyIds.filterNot { it in next.doneToday(s.day, dailyIds) }
        result = ResultView(
            name = def.name,
            icon = def.icon,
            score = res.score,
            level = res.level,
            prevLevel = before.level,
            accuracy = res.accuracy,
            best = next.progress(s.id, s.relaxed).best,
            newBest = before.plays > 0 && res.score > before.best,
            dailyComplete = !wasDone && s.day in next.dailyDone,
            hasNext = s.daily && s.day == today && open.isNotEmpty(),
            tally = def.tally,
            hits = r.correct,
            total = r.answered,
        )
        screen = Screen.RESULT
    }

    fun back() {
        screen = session?.back ?: Screen.HOME
    }

    AappsTheme(ThinkrAccent) {
        val haptics = rememberHaptics(hapticsOn)
        // Start (Pfad) und Spiele-Menue zeigen dieselben Eintraege
        val gameItems = Games.all.map {
            val p = journal.progress(it.id, relaxed)
            GameItem(it.id, stringResource(it.name), it.icon, p.level, p.best, it.id in daily, it.id in done)
        }
        when (shown) {
            Screen.HOME -> HomeScreen(
                today = today,
                games = gameItems,
                streak = Streak.count(journal.dailyDone, today),
                week = Streak.week(journal.dailyDone, today),
                haptics = haptics,
                onPlay = { start(it, it in daily, Screen.HOME) },
                onGames = { screen = Screen.GAMES },
                onSettings = { screen = Screen.SETTINGS },
            )
            Screen.GAMES -> {
                BackEffect { screen = Screen.HOME }
                GamesScreen(
                    items = gameItems,
                    onPlay = { haptics.press(); start(it, it in daily, Screen.GAMES) },
                    onBack = { screen = Screen.HOME },
                )
            }
            Screen.PLAY -> {
                val s = session ?: return@AappsTheme
                val def = Games.of(s.id) ?: return@AappsTheme
                KeepScreenOn()
                // neue Runde = neuer Zustand im Host
                key(s) {
                    def.Host(
                        seed = s.seed, level = s.level, relaxed = s.relaxed,
                        haptics = haptics, tones = tones,
                        onFinish = { finish(s, it) },
                        onQuit = { back() },
                        safeColors = s.safeColors,
                    )
                }
            }
            Screen.RESULT -> {
                val r = result ?: return@AappsTheme
                val s = session ?: return@AappsTheme
                BackEffect { back() }
                ResultScreen(
                    r = r,
                    haptics = haptics,
                    onNext = {
                        val open = daily.firstOrNull { it !in journal.doneToday(today, daily) }
                        if (open != null) start(open, true, s.back) else back()
                    },
                    onAgain = { start(s.id, s.daily, s.back) },
                    onDone = { back() },
                )
            }
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.HOME }
                ThinkrSettings(
                    relaxed = relaxed,
                    onRelaxed = { relaxed = it; prefs.relaxed = it },
                    safeColors = safeColors,
                    onSafeColors = { safeColors = it; prefs.safeColors = it },
                    haptics = hapticsOn,
                    onHaptics = { hapticsOn = it; prefs.haptics = it },
                    sound = soundOn,
                    onSound = { soundOn = it; prefs.sound = it },
                    levels = Games.all.map { LevelItem(it.id, stringResource(it.name), journal.progress(it.id, relaxed).level) },
                    onReset = { id ->
                        val j = journal.resetLevel(id)
                        journal = j
                        prefs.journal = j
                        haptics.press()
                    },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.HOME },
                )
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                ThinkrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
