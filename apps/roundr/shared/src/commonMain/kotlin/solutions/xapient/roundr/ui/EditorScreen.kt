package solutions.xapient.roundr.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTextButton
import solutions.xapient.aapps.core.ui.AappsTopBar
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.roundr.res.*
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_back
import solutions.xapient.roundr.domain.Field
import solutions.xapient.roundr.domain.TimeFormat
import solutions.xapient.roundr.domain.Timeline
import solutions.xapient.roundr.domain.Workout
import solutions.xapient.roundr.domain.Codec

// Bearbeiten: −/+ je Wert, Speichern unter Namen
@Composable
fun EditorScreen(
    workout: Workout,
    savedCount: Int,
    haptics: Haptics?,
    onChange: (Workout) -> Unit,
    onSave: (String) -> Boolean,
    onBack: () -> Unit,
) {
    val c = Aapps.colors
    val total = remember(workout) { Timeline(workout).totalMs }
    AappsScaffold {
        Column(Modifier.fillMaxWidth()) {
            AappsTopBar(workoutName(workout), onBack, stringResource(CoreRes.string.aapps_back))
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SettingsGroup(null) {
                    val fields = buildList {
                        add(Field.WORK); add(Field.REST); add(Field.ROUNDS); add(Field.SETS)
                        if (workout.sets > 1) add(Field.SET_REST)
                        add(Field.PREP)
                    }
                    fields.forEachIndexed { i, f ->
                        if (i > 0) SettingsDivider()
                        StepperRow(f, workout.get(f), haptics) { dir -> onChange(workout.step(f, dir)) }
                    }
                }
                BasicText(
                    stringResource(Res.string.total_is, TimeFormat.clock(TimeFormat.ceilSec(total))),
                    Modifier.padding(horizontal = 12.dp),
                    style = Aapps.type.number.copy(color = c.fgMuted),
                )
                SaveGroup(workout, savedCount, haptics, onSave)
            }
        }
    }
}

@Composable
private fun fieldTitle(f: Field): String = stringResource(
    when (f) {
        Field.PREP -> Res.string.field_prep
        Field.WORK -> Res.string.phase_work
        Field.REST -> Res.string.phase_rest
        Field.ROUNDS -> Res.string.rounds
        Field.SETS -> Res.string.sets
        Field.SET_REST -> Res.string.phase_set_rest
    },
)

private fun fieldColor(f: Field): Color? = when (f) {
    Field.PREP -> Amber
    Field.WORK -> Ember
    Field.REST, Field.SET_REST -> Ice
    else -> null
}

private fun fieldValue(f: Field, v: Int): String = if (f.isTime) TimeFormat.clock(v) else v.toString()

@Composable
private fun StepperRow(f: Field, value: Int, haptics: Haptics?, onStep: (Int) -> Unit) {
    val c = Aapps.colors
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val title = fieldTitle(f)
    val dot = fieldColor(f)
    val text = fieldValue(f, value)
    val texts: @Composable (Modifier) -> Unit = { m ->
        Row(m, verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(Modifier.size(10.dp).background(dot, CircleShape))
                Spacer(Modifier.width(10.dp))
            }
            BasicText(title, style = Aapps.type.body.copy(color = c.fg))
        }
    }
    val stepper: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(RoundrIcons.Minus, stringResource(Res.string.less_named, title), value > f.min, haptics) { onStep(-1) }
            BasicText(
                text,
                Modifier.widthIn(min = 76.dp * fs).semantics { contentDescription = title; stateDescription = text },
                style = Aapps.type.number.copy(color = c.fg, fontSize = Aapps.type.number.fontSize * 1.15f, textAlign = TextAlign.Center),
                maxLines = 1,
            )
            StepButton(RoundrIcons.Plus, stringResource(Res.string.more_named, title), value < f.max, haptics) { onStep(+1) }
        }
    }
    val pad = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    // grosse Schrift: Stepper unter den Titel
    if (fs > 1.4f) {
        Column(pad) {
            texts(Modifier.padding(top = 4.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { stepper() }
        }
    } else {
        Row(pad, verticalAlignment = Alignment.CenterVertically) {
            texts(Modifier.weight(1f))
            stepper()
        }
    }
}

// Tippen = ein Schritt, Halten wiederholt immer schneller; an der Grenze unsichtbar, Platz bleibt
@Composable
private fun StepButton(icon: ImageVector, desc: String, enabled: Boolean, haptics: Haptics?, onStep: () -> Unit) {
    val c = Aapps.colors
    val step by rememberUpdatedState(onStep)
    val on by rememberUpdatedState(enabled)
    var pressed by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (enabled) 1f else 0f, Springs.gentle(), label = "step")
    Box(
        Modifier
            .size(48.dp)
            .graphicsLayer { this.alpha = alpha }
            .pressScale(pressed, 0.86f)
            .background(if (pressed) c.surfaceHigh else c.bg, CircleShape)
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
                        haptics?.press()
                        val repeat = launch {
                            delay(420)
                            var gap = 140L
                            while (on) {
                                step()
                                haptics?.step()
                                delay(gap)
                                gap = (gap * 0.88).toLong().coerceAtLeast(45)
                            }
                        }
                        waitForUpOrCancellation()
                        repeat.cancel()
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AappsIcon(icon, null, c.fg, size = 22.dp)
    }
}

// Unter Namen speichern; gleicher Name ueberschreibt
@Composable
private fun SaveGroup(workout: Workout, savedCount: Int, haptics: Haptics?, onSave: (String) -> Boolean) {
    val c = Aapps.colors
    var name by rememberSaveable { mutableStateOf(if (workout.isPreset) "" else workout.name) }
    var full by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val save = {
        if (name.isNotBlank()) {
            if (onSave(name)) {
                full = false
                haptics?.press()
                focus.clearFocus()
            } else {
                full = true
            }
        }
    }
    val hint = stringResource(Res.string.name_hint)
    SettingsGroup(stringResource(Res.string.save_as)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = name,
                onValueChange = { name = it.replace("\n", "").take(Workout.NAME_MAX); full = false },
                modifier = Modifier.weight(1f).semantics { contentDescription = hint },
                textStyle = Aapps.type.body.copy(color = c.fg),
                singleLine = true,
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                decorationBox = { inner ->
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).background(c.bg, CircleShape).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (name.isEmpty()) BasicText(hint, style = Aapps.type.body.copy(color = c.fgMuted))
                        inner()
                    }
                },
            )
            AappsTextButton(stringResource(Res.string.save), { save() })
        }
        if (full) {
            BasicText(
                stringResource(Res.string.saved_full, Codec.MAX_SAVED),
                Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                style = Aapps.type.body.copy(color = Amber),
            )
        } else if (savedCount > 0) {
            BasicText(
                stringResource(Res.string.saved_count, savedCount, Codec.MAX_SAVED),
                Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f),
            )
        }
    }
}
