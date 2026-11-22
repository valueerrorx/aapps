package com.valueerrorx.spinr

import android.content.res.Resources
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.valueerrorx.aapps.core.ui.AappsTheme
import com.valueerrorx.aapps.core.ui.rememberHaptics
import com.valueerrorx.spinr.domain.NumberRange
import com.valueerrorx.spinr.domain.Palette
import com.valueerrorx.spinr.domain.Round
import com.valueerrorx.spinr.domain.Wheel
import com.valueerrorx.spinr.ui.EditorScreen
import com.valueerrorx.spinr.ui.SpinScreen
import com.valueerrorx.spinr.ui.SpinState
import com.valueerrorx.spinr.ui.SpinrAbout
import com.valueerrorx.spinr.ui.SpinrPalette
import com.valueerrorx.spinr.ui.SpinrSettings
import com.valueerrorx.spinr.ui.TemplatesScreen
import com.valueerrorx.spinr.ui.WheelsScreen

val SpinrAccent = SpinrPalette[0]

private enum class Screen { WHEEL, WHEELS, TEMPLATES, EDIT, SETTINGS, ABOUT }

// Runde als Groesse + entfernte Indizes
private val RoundSaver = Saver<Round, IntArray>(
    save = { intArrayOf(it.size) + it.removed.toIntArray() },
    restore = { Round(it[0], it.drop(1).toSet()) },
)

// Vorlagen in der Sprache des Geraets, gruppiert
class TemplateGroup(val title: String, val wheels: List<Wheel>, val withRange: Boolean = false)

fun templateGroups(res: Resources): List<TemplateGroup> {
    fun t(name: Int, items: Int) = Wheel(res.getString(name), res.getStringArray(items).toList())
    return listOf(
        TemplateGroup(
            res.getString(R.string.tpl_group_everyday),
            listOf(
                t(R.string.tpl_food, R.array.tpl_food),
                t(R.string.tpl_yesno, R.array.tpl_yesno),
                t(R.string.tpl_pay, R.array.tpl_pay),
                t(R.string.tpl_weekdays, R.array.tpl_weekdays),
                t(R.string.tpl_colors, R.array.tpl_colors),
            ),
        ),
        TemplateGroup(
            res.getString(R.string.tpl_group_numbers),
            listOf(
                t(R.string.tpl_dice, R.array.tpl_dice),
                Wheel(res.getString(R.string.tpl_numbers), NumberRange.entries(1, 10)!!),
            ),
            withRange = true,
        ),
        TemplateGroup(
            res.getString(R.string.tpl_group_directions),
            listOf(t(R.string.tpl_way, R.array.tpl_way), t(R.string.tpl_compass, R.array.tpl_compass)),
        ),
        TemplateGroup(
            res.getString(R.string.tpl_group_games),
            listOf(
                t(R.string.tpl_coin, R.array.tpl_coin),
                t(R.string.tpl_rps, R.array.tpl_rps),
                t(R.string.tpl_truth, R.array.tpl_truth),
                Wheel(res.getString(R.string.tpl_letters), ('A'..'Z').map { "$it" }),
            ),
        ),
    )
}

// Raeder beim ersten Start
fun starterWheels(res: Resources): List<Wheel> {
    val all = templateGroups(res).flatMap { it.wheels }.associateBy { it.name }
    return listOf(R.string.tpl_food, R.string.tpl_yesno, R.string.tpl_dice, R.string.tpl_pay).map { all.getValue(res.getString(it)) }
}

@Composable
fun SpinrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    // Vorlagen und Name eines neuen Rads folgen der aktuellen Sprache
    val resources = LocalResources.current
    val groups = remember(LocalConfiguration.current) { templateGroups(resources) }
    val newWheelName = stringResource(R.string.new_wheel_name)
    var wheels by remember { mutableStateOf(prefs.wheels.ifEmpty { starterWheels(resources).also { prefs.wheels = it } }) }
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
    val player = remember(soundOn) { if (soundOn) TickPlayer(context) else null }
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
                BackHandler { screen = Screen.WHEEL }
                WheelsScreen(
                    wheels = wheels,
                    active = active,
                    onSelect = {
                        save(wheels, it)
                        haptics.tick()
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
                BackHandler { screen = Screen.WHEELS }
                TemplatesScreen(
                    groups = groups,
                    onAdd = {
                        save(wheels + it, wheels.size)
                        haptics.tick()
                        screen = Screen.WHEEL
                    },
                    onBack = { screen = Screen.WHEELS },
                )
            }
            Screen.EDIT -> {
                val index = editing.coerceIn(0, wheels.lastIndex)
                BackHandler { flush(); screen = Screen.WHEELS }
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
                BackHandler { screen = Screen.WHEEL }
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
                BackHandler { screen = Screen.SETTINGS }
                SpinrAbout(BuildConfig.VERSION_NAME) { screen = Screen.SETTINGS }
            }
        }
    }
}
