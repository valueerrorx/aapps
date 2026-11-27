package solutions.xapient.roundr

import androidx.compose.runtime.Composable
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.roundr.domain.Presets
import solutions.xapient.roundr.domain.Run
import solutions.xapient.roundr.domain.Session
import solutions.xapient.roundr.domain.Workout
import solutions.xapient.roundr.ui.DoneScreen
import solutions.xapient.roundr.ui.EditorScreen
import solutions.xapient.roundr.ui.HomeScreen
import solutions.xapient.roundr.ui.LibraryScreen
import solutions.xapient.roundr.ui.RoundrAbout
import solutions.xapient.roundr.ui.RoundrAccent
import solutions.xapient.roundr.ui.RoundrSettings
import solutions.xapient.roundr.ui.RunScreen

// Feste Zustaende fuer App-Store-Screenshots, wie die Roborazzi-Tests auf Android
internal enum class Scene {
    WORK, REST, HOME, LIBRARY, EDITOR, SETREST, LAST3, DONE, PAUSED, PREP, LONG, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

private val tabata = Presets.all[0]
private val tabata3 = Presets.all[1]
private val boxing = Presets.all[4]
private val saved = listOf(Workout("Quiz", 5, 45, 15, 6), Workout("Sprint", 10, 60, 30, 5, 2, 120))

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(RoundrAccent) {
        when (scene) {
            // Runde 3, Aktiv, 12 von 20 s uebrig
            Scene.WORK -> Running(tabata, 78_000)
            Scene.REST -> Running(tabata, 68_000)
            Scene.HOME -> HomeScreen(tabata, {}, {}, {}, {})
            Scene.LIBRARY -> LibraryScreen(tabata, saved, {}, {}, {}, confirmName = "Sprint")
            Scene.EDITOR -> EditorScreen(tabata3, saved.size, null, {}, { true }, {})
            Scene.SETREST -> Running(tabata3, 255_000)
            Scene.LAST3 -> Running(tabata, 88_000)
            Scene.DONE -> DoneScreen(tabata, {}, {})
            Scene.PAUSED -> Running(tabata, 78_000, running = false, confirm = true)
            Scene.PREP -> Running(tabata, 3_000)
            Scene.LONG -> Running(boxing, 29_000)
            Scene.SETTINGS -> RoundrSettings(true, {}, true, {}, true, {}, {}, {})
            Scene.ABOUT -> RoundrAbout(versionName, iosNote = true) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

// Uhr steht bei 0, Lauf seit 0: verstrichen = e
@Composable
private fun Running(w: Workout, e: Long, running: Boolean = true, confirm: Boolean = false) = RunScreen(
    Run(w, Session(e, if (running) 0L else null), 1), clock = { 0L }, haptics = null,
    onToggle = {}, onSkip = {}, onBack = {}, onStop = {}, onAgain = {}, onClose = {},
    keepOn = false, animate = false, confirmStop = confirm,
)

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
