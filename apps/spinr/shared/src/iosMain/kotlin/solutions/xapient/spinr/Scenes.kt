package solutions.xapient.spinr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.spinr.domain.Palette
import solutions.xapient.spinr.domain.WheelMath
import solutions.xapient.spinr.ui.EditorScreen
import solutions.xapient.spinr.ui.SpinScreen
import solutions.xapient.spinr.ui.SpinState
import solutions.xapient.spinr.ui.SpinrAbout
import solutions.xapient.spinr.ui.SpinrPalette
import solutions.xapient.spinr.ui.SpinrSettings
import solutions.xapient.spinr.ui.TemplatesScreen
import solutions.xapient.spinr.ui.WheelsScreen

// Feste Zustaende fuer App-Store-Screenshots, wie die Roborazzi-Tests auf Android
internal enum class Scene {
    WHEEL, SPINNING, RESULT, MANY, REMOVED, WHEELS, TEMPLATES, COMPASS, EDITOR, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(SpinrAccent) {
        val groups = templateGroups()
        val presets = starterWheels(groups)
        val food = presets[0]
        when (scene) {
            Scene.WHEEL -> Spin(remember { SpinState(food.labels).apply { pose(17f) } }, food.name)
            // schnell, Zunge gerade von einem Stift mitgenommen
            Scene.SPINNING -> Spin(remember { SpinState(food.labels).apply { pose(-123f, speed = 1250f, flap = 0.7f) } }, food.name)
            Scene.RESULT -> {
                val angle = WheelMath.angleFor(1, food.labels.size).toFloat() + 9f
                Spin(remember { SpinState(food.labels).apply { pose(angle, winner = 1) } }, food.name, confetti = 0.42f)
            }
            Scene.MANY -> {
                val labels = (1..30).map { "$it" }
                Spin(remember { SpinState(labels).apply { pose(WheelMath.angleFor(16, 30).toFloat(), winner = 16) } }, "1 – 30")
            }
            Scene.REMOVED -> {
                val ids = food.labels.indices.filter { it != 1 && it != 5 }
                val colors = Palette.colors(ids, food.labels.size, SpinrPalette.size).map { SpinrPalette[it] }
                Spin(remember { SpinState(ids.map { food.labels[it] }, colors).apply { pose(40f) } }, food.name, removed = 2)
            }
            Scene.WHEELS -> WheelsScreen(presets, 0, {}, {}, {}, {}, {})
            Scene.TEMPLATES -> TemplatesScreen(groups, {}, {})
            // Vorlage Himmelsrichtungen als Rad (Gruppe Richtungen, zweites Rad)
            Scene.COMPASS -> {
                val w = groups[2].wheels[1]
                Spin(remember { SpinState(w.labels).apply { pose(17f) } }, w.name)
            }
            Scene.EDITOR -> EditorScreen(
                food.copy(entries = food.entries.take(6) + ""), canDelete = true, onChange = {}, onDelete = {}, onBack = {},
            )
            Scene.SETTINGS -> SpinrSettings(
                haptics = true, onHaptics = {}, sound = false, onSound = {},
                activeName = food.name, onWheels = {}, onAbout = {}, onBack = {},
            )
            Scene.ABOUT -> SpinrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

@Composable
private fun Spin(state: SpinState, name: String, removed: Int = 0, confetti: Float = -1f) = SpinScreen(
    state, name, canRemove = state.labels.size > 2, removedCount = removed, haptics = null, onTick = null,
    onOpenWheels = {}, onOpenSettings = {}, onRemove = {}, onRestore = {},
    animate = false, confettiAt = confetti,
)

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
