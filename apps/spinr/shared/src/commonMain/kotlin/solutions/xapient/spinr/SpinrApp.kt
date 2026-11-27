package solutions.xapient.spinr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.spinr.domain.NumberRange
import solutions.xapient.spinr.domain.Palette
import solutions.xapient.spinr.domain.Round
import solutions.xapient.spinr.domain.Wheel
import solutions.xapient.spinr.res.*
import solutions.xapient.spinr.ui.EditorScreen
import solutions.xapient.spinr.ui.SpinScreen
import solutions.xapient.spinr.ui.SpinState
import solutions.xapient.spinr.ui.SpinrAbout
import solutions.xapient.spinr.ui.SpinrPalette
import solutions.xapient.spinr.ui.SpinrSettings
import solutions.xapient.spinr.ui.TemplatesScreen
import solutions.xapient.spinr.ui.WheelsScreen

const val APP_NAME = "spinr"

val SpinrAccent = SpinrPalette[0]

private enum class Screen { WHEEL, WHEELS, TEMPLATES, EDIT, SETTINGS, ABOUT }

// Runde als Groesse + entfernte Indizes
private val RoundSaver = Saver<Round, IntArray>(
    save = { intArrayOf(it.size) + it.removed.toIntArray() },
    restore = { Round(it[0], it.drop(1).toSet()) },
)

// Vorlagen in der Sprache des Geraets, gruppiert
class TemplateGroup(val title: String, val wheels: List<Wheel>, val withRange: Boolean = false)

@Composable
private fun t(name: StringResource, items: StringArrayResource) = Wheel(stringResource(name), stringArrayResource(items))

@Composable
fun templateGroups(): List<TemplateGroup> {
    return listOf(
        TemplateGroup(
            stringResource(Res.string.tpl_group_everyday),
            listOf(
                t(Res.string.tpl_food, Res.array.tpl_food),
                t(Res.string.tpl_yesno, Res.array.tpl_yesno),
                t(Res.string.tpl_pay, Res.array.tpl_pay),
                t(Res.string.tpl_weekdays, Res.array.tpl_weekdays),
                t(Res.string.tpl_colors, Res.array.tpl_colors),
            ),
        ),
        TemplateGroup(
            stringResource(Res.string.tpl_group_numbers),
            listOf(
                t(Res.string.tpl_dice, Res.array.tpl_dice),
                Wheel(stringResource(Res.string.tpl_numbers), NumberRange.entries(1, 10)!!),
            ),
            withRange = true,
        ),
        TemplateGroup(
            stringResource(Res.string.tpl_group_directions),
            listOf(t(Res.string.tpl_way, Res.array.tpl_way), t(Res.string.tpl_compass, Res.array.tpl_compass)),
        ),
        TemplateGroup(
            stringResource(Res.string.tpl_group_games),
            listOf(
                t(Res.string.tpl_coin, Res.array.tpl_coin),
                t(Res.string.tpl_rps, Res.array.tpl_rps),
                t(Res.string.tpl_truth, Res.array.tpl_truth),
                Wheel(stringResource(Res.string.tpl_letters), ('A'..'Z').map { "$it" }),
            ),
        ),
    )
}

// Raeder beim ersten Start
@Composable
fun starterWheels(groups: List<TemplateGroup> = templateGroups()): List<Wheel> {
    val all = groups.flatMap { it.wheels }.associateBy { it.name }
    return listOf(Res.string.tpl_food, Res.string.tpl_yesno, Res.string.tpl_dice, Res.string.tpl_pay)
        .map { all.getValue(stringResource(it)) }
}

/** Klick pro Stift; Android SoundPool, iOS AVAudioPlayer */
interface TickPlayer {
    fun play()
    fun release()
}

// Gemeinsam fuer Android und iOS; Speicher und Klick-Ton kommen von der Plattform
@Composable
fun SpinrApp(prefs: Prefs, newTickPlayer: () -> TickPlayer, versionName: String) {
    // Vorlagen und Name eines neuen Rads folgen der aktuellen Sprache
    val groups = templateGroups()
    val starters = starterWheels(groups)
    val newWheelName = stringResource(Res.string.new_wheel_name)
    var wheels by remember { mutableStateOf(prefs.wheels.ifEmpty { starters.also { prefs.wheels = it } }) }
    // Aenderungen im Editor erst beim Verlassen bzw. beim Wegschalten der App schreiben
    var dirty by remember { mutableStateOf(false) }
    var active by remember { mutableIntStateOf(prefs.active.coerceIn(0, wheels.lastIndex)) }
    var editing by rememberSaveable { mutableIntStateOf(0) }
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var soundOn by remember { mutableStateOf(prefs.sound) }
    var screen by rememberSaveable { mutableStateOf(Screen.WHEEL) }

    val wheel = wheels[active]
    val labels = wheel.labels
    // Runde gilt fuer dieses Rad mit genau diesen Eintraegen; ueberlebt Prozess-Neustart
    var round by rememberSaveable(active, labels, stateSaver = RoundSaver) { mutableStateOf(Round(labels.size)) }
    val shown = round.remaining.map { labels[it] }
    // jeder Eintrag behaelt seine Farbe aus dem vollen Rad
    val colors = Palette.colors(round.remaining, labels.size, SpinrPalette.size).map { SpinrPalette[it] }
    val state = rememberSaveable(saver = Saver<SpinState, Float>({ it.angle }, { a -> SpinState(shown, colors).apply { pose(a) } })) {
        SpinState(shown, colors)
    }
    LaunchedEffect(shown, colors) { state.showLabels(shown, colors) }

    fun flush() {
        if (!dirty) return
        prefs.wheels = wheels
        dirty = false
    }

    fun save(new: List<Wheel>, newActive: Int = active) {
        wheels = new
        active = newActive.coerceIn(0, new.lastIndex)
        prefs.wheels = new
        prefs.active = active
        dirty = false
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { flush() }

    // Ton nur, solange eingeschaltet
    val player = remember(soundOn) { if (soundOn) newTickPlayer() else null }
    DisposableEffect(player) { onDispose { player?.release() } }

    AappsTheme(SpinrAccent) {
        val haptics = rememberHaptics(hapticsOn)
        when (screen) {
            Screen.WHEEL -> SpinScreen(
                state = state,
                wheelName = wheel.name,
                canRemove = round.canRemove,
                removedCount = round.removed.size,
                haptics = haptics,
                onTick = player?.let { p -> { p.play() } },
                onOpenWheels = { screen = Screen.WHEELS },
                onOpenSettings = { screen = Screen.SETTINGS },
                onRemove = { segment -> round = round.removeShown(segment) },
                onRestore = { round = round.restore() },
            )
            Screen.WHEELS -> {
                BackEffect { screen = Screen.WHEEL }
                WheelsScreen(
                    wheels = wheels,
                    active = active,
                    onSelect = {
                        save(wheels, it)
                        haptics.press()
                        screen = Screen.WHEEL
                    },
                    onEdit = {
                        editing = it
                        screen = Screen.EDIT
                    },
                    onNew = {
                        save(wheels + Wheel(newWheelName, listOf("", "")), wheels.size)
                        editing = wheels.lastIndex
                        screen = Screen.EDIT
                    },
                    onTemplates = { screen = Screen.TEMPLATES },
                    onBack = { screen = Screen.WHEEL },
                )
            }
            Screen.TEMPLATES -> {
                BackEffect { screen = Screen.WHEELS }
                TemplatesScreen(
                    groups = groups,
                    onAdd = {
                        save(wheels + it, wheels.size)
                        haptics.press()
                        screen = Screen.WHEEL
                    },
                    onBack = { screen = Screen.WHEELS },
                )
            }
            Screen.EDIT -> {
                val index = editing.coerceIn(0, wheels.lastIndex)
                BackEffect { flush(); screen = Screen.WHEELS }
                EditorScreen(
                    wheel = wheels[index],
                    canDelete = wheels.size > 1,
                    onChange = { w ->
                        wheels = wheels.toMutableList().also { it[index] = w }
                        dirty = true
                    },
                    onDelete = {
                        val rest = wheels.toMutableList().also { it.removeAt(index) }
                        save(rest, if (active > index) active - 1 else if (active == index) minOf(index, rest.lastIndex) else active)
                        screen = Screen.WHEELS
                    },
                    onBack = { flush(); screen = Screen.WHEELS },
                )
            }
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.WHEEL }
                SpinrSettings(
                    haptics = hapticsOn,
                    onHaptics = { hapticsOn = it; prefs.haptics = it },
                    sound = soundOn,
                    onSound = { soundOn = it; prefs.sound = it },
                    activeName = wheel.name,
                    onWheels = { screen = Screen.WHEELS },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.WHEEL },
                )
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                SpinrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}
