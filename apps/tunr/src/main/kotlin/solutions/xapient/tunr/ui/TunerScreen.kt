package solutions.xapient.tunr.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.glow
import solutions.xapient.tunr.R
import solutions.xapient.tunr.domain.Instrument
import solutions.xapient.tunr.domain.Notation
import solutions.xapient.tunr.domain.Notes
import solutions.xapient.tunr.domain.Settings

// Mikrofon-Lage aus Sicht der Oberflaeche
enum class MicUi { ON, ASK, BLOCKED, ERROR }

@Composable
fun TunerScreen(
    state: TunerState,
    settings: Settings,
    mic: MicUi,
    locked: Int,
    haptics: Haptics?,
    onLock: (Int) -> Unit,
    onInstrument: () -> Unit,
    onSettings: () -> Unit,
    onMicAction: () -> Unit,
    animate: Boolean = true,
) {
    if (animate) {
        // Bildschleife nur, solange sich etwas bewegt; sonst wartet sie auf neue Werte
        LaunchedEffect(state) {
            var last = 0L
            while (true) {
                if (state.motion.isSettled) {
                    last = 0L
                    val v = state.version
                    snapshotFlow { state.version }.first { it != v }
                }
                withFrameNanos { now ->
                    val dt = if (last == 0L) 1f / 60f else (now - last) / 1e9f
                    last = now
                    state.motion.step(dt)
                    state.frame++
                }
            }
        }
    }
    // Haptik nur bei neuem Einrasten, nicht beim Zurueckkehren
    val locks = state.coarse.locks
    var locksHandled by remember { mutableIntStateOf(locks) }
    LaunchedEffect(locks) {
        if (locks > locksHandled) haptics?.confirm()
        locksHandled = locks
    }

    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val hint = when {
        mic != MicUi.ON -> stringResource(R.string.mic_off)
        state.micSilent -> stringResource(R.string.mic_silent)
        else -> null
    }
    val dial: @Composable (Modifier) -> Unit = { m -> TunerDial(state, settings.notation, hint, m) }
    val strings: @Composable () -> Unit = {
        if (!settings.instrument.chromatic) StringRow(settings.instrument, settings.notation, state.coarse, locked, haptics, onLock)
    }
    val top: @Composable (Modifier) -> Unit = { m ->
        Row(m.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                stringResource(R.string.a4_label, settings.a4).uppercase(),
                Modifier.padding(start = 12.dp).weight(1f),
                style = Aapps.type.label.copy(color = Aapps.colors.fgMuted),
                maxLines = 1,
            )
            AappsIconButton(AappsIcons.Gear, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_settings), onSettings)
        }
    }
    val action: @Composable () -> Unit = {
        if (mic == MicUi.ON) {
            AappsPillButton(instrumentLabel(settings.instrument), onInstrument, Modifier.widthIn(min = 176.dp), icon = TunrIcons.Fork)
        } else {
            MicPrompt(mic, onMicAction)
        }
    }
    AappsScaffold {
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(0.44f).fillMaxHeight().padding(start = 12.dp, end = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    top(Modifier.fillMaxWidth())
                    Spacer(Modifier.weight(1f))
                    strings()
                    Spacer(Modifier.height(24.dp))
                    action()
                    Spacer(Modifier.weight(1f))
                }
                dial(Modifier.weight(0.56f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                top(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp))
                // Hero und Saiten als Gruppe mittig: kein Leerraum nur unter dem Kreis
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    dial(Modifier.weight(1f, fill = false).fillMaxWidth().aspectRatio(1f, matchHeightConstraintsFirst = true))
                    if (!settings.instrument.chromatic) Spacer(Modifier.height(8.dp))
                    strings()
                }
                Spacer(Modifier.height(20.dp))
                Box(Modifier.padding(bottom = 16.dp)) { action() }
            }
        }
    }
}

// Saiten des Instruments: naechste hervorgehoben, gestimmt gruen gefuellt, Tippen haelt fest
@Composable
private fun StringRow(
    instrument: Instrument,
    notation: Notation,
    coarse: Coarse,
    locked: Int,
    haptics: Haptics?,
    onLock: (Int) -> Unit,
) {
    val c = Aapps.colors
    val held = stringResource(R.string.string_locked)
    val auto = stringResource(R.string.string_auto)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
        instrument.strings.forEachIndexed { i, midi ->
            val name = Notes.name(midi, notation)
            val current = coarse.heard && coarse.string == i
            val tuned = current && coarse.inTune
            val isLocked = locked == i
            val fill by animateColorAsState(
                when {
                    tuned -> c.accent
                    current -> c.surfaceHigh
                    else -> c.surface
                },
                Springs.gentle(), label = "stringFill",
            )
            val ink by animateColorAsState(
                when {
                    tuned -> c.onAccent
                    current || isLocked -> c.fg
                    else -> c.fgMuted
                },
                Springs.gentle(), label = "stringInk",
            )
            val edge = when {
                isLocked -> c.accent
                current -> c.fgMuted
                else -> c.line
            }
            val desc = stringResource(R.string.string_desc, i + 1, name.toString())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(52.dp)
                        .glow(c.accent, 44.dp, if (tuned) 0.4f else 0f)
                        .background(fill, CircleShape)
                        .border(if (isLocked) 2.dp else 1.dp, edge, CircleShape)
                        .clickable(interactionSource = null, indication = null, role = Role.Button) {
                            haptics?.tick()
                            onLock(if (isLocked) -1 else i)
                        }
                        .semantics {
                            contentDescription = desc
                            stateDescription = if (isLocked) held else auto
                        }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        buildAnnotatedString {
                            append(name.letter)
                            withStyle(SpanStyle(fontSize = 0.62.em)) { append(name.octave.toString()) }
                        },
                        style = Aapps.type.number.copy(color = ink, fontSize = 20.sp, textAlign = TextAlign.Center),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 20.sp, stepSize = 0.5.sp),
                    )
                }
                // Punkt unter der festgehaltenen Saite
                Spacer(Modifier.height(5.dp))
                Box(Modifier.size(5.dp).background(if (isLocked) c.accent else Color.Transparent, CircleShape))
            }
        }
    }
}

// Ohne Mikrofon: Erklaerung und genau eine Aktion
@Composable
private fun MicPrompt(mic: MicUi, onAction: () -> Unit) {
    val c = Aapps.colors
    val (text, button) = when (mic) {
        MicUi.BLOCKED -> R.string.mic_blocked_text to R.string.mic_settings
        MicUi.ERROR -> R.string.mic_error_text to R.string.mic_retry
        else -> R.string.mic_ask_text to R.string.mic_allow
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 28.dp)) {
        BasicText(
            stringResource(text),
            style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(14.dp))
        AappsPillButton(stringResource(button), onAction, Modifier.widthIn(min = 176.dp), icon = TunrIcons.Mic, active = true)
    }
}
