package solutions.xapient.tunr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTopBar
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsChoice
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.tunr.R
import solutions.xapient.tunr.domain.Instrument
import solutions.xapient.tunr.domain.Notation
import solutions.xapient.tunr.domain.Settings

@Composable
fun TunrSettings(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(R.string.group_tuning)) {
            A4Row(settings.a4) { onChange(settings.copy(a4 = it)) }
            SettingsDivider()
            SettingsChoice(
                stringResource(R.string.notation),
                listOf(
                    // Woerter statt Vorzeichen: die Schriften haben keine ♯/♭-Glyphen
                    Notation.SHARP to stringResource(R.string.notation_sharp),
                    Notation.FLAT to stringResource(R.string.notation_flat),
                    Notation.GERMAN to stringResource(R.string.notation_german),
                ),
                settings.notation,
            ) { onChange(settings.copy(notation = it)) }
        }
        SettingsGroup(stringResource(R.string.group_feedback)) {
            SettingsToggle(stringResource(R.string.haptics), settings.haptics, { onChange(settings.copy(haptics = it)) }, stringResource(R.string.haptics_sub))
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_about), onClick = onAbout)
        }
    }
}

// Kammerton mit - und +; Halten wiederholt
@Composable
private fun A4Row(a4: Int, onA4: (Int) -> Unit) {
    val c = Aapps.colors
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val value by rememberUpdatedState(a4)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(stringResource(R.string.a4_title), style = Aapps.type.body.copy(color = c.fg))
            Spacer(Modifier.height(2.dp))
            BasicText(stringResource(R.string.a4_sub), style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f))
        }
        Spacer(Modifier.width(8.dp))
        StepButton(TunrIcons.Minus, stringResource(R.string.a4_lower), a4 > Settings.A4_MIN) { onA4((value - 1).coerceAtLeast(Settings.A4_MIN)) }
        BasicText(
            stringResource(R.string.a4_value, a4),
            Modifier.widthIn(min = 72.dp * fs),
            style = Aapps.type.number.copy(color = c.fg, textAlign = TextAlign.Center),
            maxLines = 1,
        )
        StepButton(TunrIcons.Plus, stringResource(R.string.a4_higher), a4 < Settings.A4_MAX) { onA4((value + 1).coerceAtMost(Settings.A4_MAX)) }
    }
}

// Tippen = ein Schritt, Halten wiederholt immer schneller
@Composable
private fun StepButton(icon: ImageVector, desc: String, enabled: Boolean, onStep: () -> Unit) {
    val c = Aapps.colors
    val step by rememberUpdatedState(onStep)
    val on by rememberUpdatedState(enabled)
    var pressed by remember { mutableStateOf(false) }
    Box48(
        Modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .pressScale(pressed, 0.86f)
            .background(if (pressed) c.surfaceHigh else c.bg, CircleShape)
            .border(1.dp, c.line, CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = desc
                if (enabled) onClick { step(); true }
            }
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown()
                        if (!on) return@awaitEachGesture
                        pressed = true
                        step()
                        val repeat = launch {
                            delay(420)
                            var gap = 140L
                            while (on) {
                                step()
                                delay(gap)
                                gap = (gap * 0.88).toLong().coerceAtLeast(50)
                            }
                        }
                        waitForUpOrCancellation()
                        repeat.cancel()
                        pressed = false
                    }
                }
            },
    ) { AappsIcon(icon, null, c.fg, size = 20.dp) }
}

@Composable
private fun Box48(modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier.size(48.dp), contentAlignment = Alignment.Center) { content() }
}

// Instrument waehlen: Liste mit Saiten, Auswahl springt zurueck
@Composable
fun InstrumentScreen(selected: Instrument, notation: Notation, onSelect: (Instrument) -> Unit, onBack: () -> Unit) {
    val c = Aapps.colors
    AappsScaffold {
        Column(Modifier.fillMaxWidth()) {
            AappsTopBar(stringResource(R.string.instrument), onBack, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_back))
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SettingsGroup(null) {
                    Instrument.entries.forEachIndexed { i, inst ->
                        if (i > 0) SettingsDivider()
                        val sel = inst == selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(sel, indication = null, interactionSource = null, role = Role.RadioButton) { onSelect(inst) }
                                .heightIn(min = 64.dp)
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                BasicText(instrumentLabel(inst), style = Aapps.type.body.copy(color = c.fg))
                                Spacer(Modifier.height(2.dp))
                                BasicText(
                                    if (inst.chromatic) stringResource(R.string.inst_chromatic_sub) else stringsText(inst, notation),
                                    style = if (inst.chromatic) {
                                        Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f)
                                    } else {
                                        Aapps.type.number.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.15f)
                                    },
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            AappsIcon(AappsIcons.Check, null, if (sel) c.accent else Color.Transparent, size = 22.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TunrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        tagline = stringResource(R.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(R.string.note_mic)),
        icon = { Image(painterResource(R.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}
