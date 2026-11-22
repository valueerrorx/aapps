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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.valueerrorx.aapps.core.ui.AappsTheme
import com.valueerrorx.aapps.core.ui.rememberHaptics
import com.valueerrorx.spinr.domain.Round
import com.valueerrorx.spinr.domain.Wheel
import com.valueerrorx.spinr.ui.EditorScreen
import com.valueerrorx.spinr.ui.SpinScreen
import com.valueerrorx.spinr.ui.SpinState
import com.valueerrorx.spinr.ui.SpinrAbout
import com.valueerrorx.spinr.ui.SpinrPalette
import com.valueerrorx.spinr.ui.SpinrSettings
import com.valueerrorx.spinr.ui.WheelsScreen

val SpinrAccent = SpinrPalette[0]

private enum class Screen { WHEEL, WHEELS, EDIT, SETTINGS, ABOUT }

// Vorlagen in der Sprache des Geraets
fun templates(res: Resources): List<Wheel> = listOf(
    R.string.tpl_food to R.array.tpl_food,
    R.string.tpl_yesno to R.array.tpl_yesno,
    R.string.tpl_dice to R.array.tpl_dice,
    R.string.tpl_pay to R.array.tpl_pay,
).map { (name, items) -> Wheel(res.getString(name), res.getStringArray(items).toList()) }

@Composable
fun SpinrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    // Vorlagen und Name eines neuen Rads folgen der aktuellen Sprache
    val resources = LocalResources.current
    val presets = remember(LocalConfiguration.current) { templates(resources) }
    val newWheelName = stringResource(R.string.new_wheel_name)
    var wheels by remember { mutableStateOf(prefs.wheels.ifEmpty { presets.also { prefs.wheels = it } }) }
    var active by remember { mutableIntStateOf(prefs.active.coerceIn(0, wheels.lastIndex)) }
    var editing by rememberSaveable { mutableIntStateOf(0) }
    var hapticsOn by remember { mutableStateOf(prefs.haptics) }
    var soundOn by remember { mutableStateOf(prefs.sound) }
    var screen by rememberSaveable { mutableStateOf(Screen.WHEEL) }

    val wheel = wheels[active]
    val labels = wheel.labels
    // Runde gilt fuer dieses Rad mit genau diesen Eintraegen
    var round by remember(active, labels) { mutableStateOf(Round(labels.size)) }
    val shown = round.remaining.map { labels[it] }
    val state = remember { SpinState(shown) }
    LaunchedEffect(shown) { state.showLabels(shown) }

    fun save(new: List<Wheel>, newActive: Int = active) {
        wheels = new
        active = newActive.coerceIn(0, new.lastIndex)
        prefs.wheels = new
        prefs.active = active
    }

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
                onRemove = { segment -> round.remaining.getOrNull(segment)?.let { round = round.remove(it) } },
                onRestore = { round = round.restore() },
            )
            Screen.WHEELS -> {
                BackHandler { screen = Screen.WHEEL }
                WheelsScreen(
                    wheels = wheels,
                    active = active,
                    templates = presets,
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
                    onAddTemplate = {
                        save(wheels + it, wheels.size)
                        haptics.tick()
                        screen = Screen.WHEEL
                    },
                    onBack = { screen = Screen.WHEEL },
                )
            }
            Screen.EDIT -> {
                val index = editing.coerceIn(0, wheels.lastIndex)
                BackHandler { screen = Screen.WHEELS }
                EditorScreen(
                    wheel = wheels[index],
                    canDelete = wheels.size > 1,
                    onChange = { w -> save(wheels.toMutableList().also { it[index] = w }) },
                    onDelete = {
                        val rest = wheels.toMutableList().also { it.removeAt(index) }
                        save(rest, if (active > index) active - 1 else if (active == index) minOf(index, rest.lastIndex) else active)
                        screen = Screen.WHEELS
                    },
                    onBack = { screen = Screen.WHEELS },
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
