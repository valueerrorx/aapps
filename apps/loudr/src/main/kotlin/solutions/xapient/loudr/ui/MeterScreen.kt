package solutions.xapient.loudr.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.loudr.R
import solutions.xapient.loudr.Settings
import solutions.xapient.loudr.domain.Calibration
import solutions.xapient.loudr.domain.Example
import solutions.xapient.loudr.domain.Reading
import solutions.xapient.loudr.domain.formatDb

enum class MicState { READY, ASK, BLOCKED, FAILED, SILENCED }

// Messer: Bogen als Hero, darunter Werte und Verlauf, eine Hauptaktion
@Composable
fun MeterScreen(
    reading: () -> Reading,
    mic: MicState,
    measuring: Boolean,
    settings: Settings,
    haptics: Haptics?,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onMic: () -> Unit,
    onSettings: () -> Unit,
    animate: Boolean = true,
) {
    val ready = mic == MicState.READY
    val live = ready && measuring
    // Bildzeit nur waehrend der Messung; pausiert kein einziges Bild
    val frame = remember { mutableLongStateOf(0L) }
    val version = remember { mutableIntStateOf(0) }
    LaunchedEffect(live, animate) {
        if (live && animate) while (true) withFrameNanos { frame.longValue = it }
    }
    if (live && settings.keepOn) KeepScreenOn()
    val source by rememberUpdatedState(reading)
    val display = remember { MeterDisplay { source() } }
    val update = { display.at(frame.longValue, version.intValue, settings.weighting, live, animate) }
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }

    // Vorlesen: Zusammenfassung hoechstens einmal pro Sekunde, nicht je Bild
    val res = LocalResources.current
    var spoken by remember { mutableStateOf("") }
    LaunchedEffect(live, ready, settings) {
        while (true) {
            spoken = if (ready) describe(res, reading(), settings) else ""
            if (!live) break
            delay(1_000)
        }
    }

    val gauge: @Composable (Modifier) -> Unit = { m ->
        val desc = stringResource(R.string.level)
        Gauge(
            display, update, settings.weighting, settings.offset, live, empty = !ready,
            modifier = m.semantics {
                contentDescription = desc
                stateDescription = spoken
                liveRegion = LiveRegionMode.Polite
            },
        ) {
            if (!ready) AappsIcon(LoudrIcons.Mic, null, Aapps.colors.fgMuted, size = 56.dp)
        }
    }
    val top: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ready) Status(live)
            Spacer(Modifier.weight(1f))
            AappsIconButton(AappsIcons.Gear, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_settings), onSettings)
        }
    }
    val bottom: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp)) {
                if (ready) {
                    AappsIconButton(LoudrIcons.Reset, stringResource(R.string.reset), {
                        haptics?.confirm()
                        onReset()
                        version.intValue++
                    })
                }
            }
            Spacer(Modifier.weight(1f))
            MainAction(mic, live, haptics, onToggle, onMic)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }
    }
    val chart: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            BasicText(
                stringResource(R.string.history).uppercase(),
                Modifier.padding(bottom = 6.dp),
                style = Aapps.type.label.copy(color = Aapps.colors.fgMuted, fontSize = cappedSp(12f, 16f)),
            )
            HistoryChart(display, update, settings.offset, live, Modifier.fillMaxWidth().weight(1f))
        }
    }
    val stats: @Composable (Modifier) -> Unit = { m -> Stats(display, update, settings.weighting, settings.offset, m) }

    AappsScaffold {
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.46f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    top()
                    Column(
                        Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (ready) {
                            stats(Modifier.fillMaxWidth())
                            Spacer(Modifier.height(16.dp))
                            chart(Modifier.fillMaxWidth().weight(1f).heightIn(max = 130.dp))
                            Spacer(Modifier.height(10.dp))
                            Footnote(settings.offset)
                        } else {
                            Prompt(mic)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    bottom()
                }
                gauge(Modifier.weight(0.54f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                top()
                // Bogen und Werte als Gruppe mittig: kein Leerraum zwischen Hero und Ablesewert
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    gauge(Modifier.weight(1f, fill = false).aspectRatio(GAUGE_ASPECT))
                    if (ready) {
                        stats(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                        Spacer(Modifier.height(24.dp))
                        chart(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(150.dp))
                        Spacer(Modifier.height(10.dp))
                        Footnote(settings.offset)
                    } else {
                        Prompt(mic)
                    }
                }
                Spacer(Modifier.height(16.dp))
                bottom()
            }
        }
    }
}

@Composable
private fun Status(live: Boolean) {
    val c = Aapps.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(if (live) c.accent else c.fgMuted, CircleShape))
        Spacer(Modifier.width(10.dp))
        BasicText(
            stringResource(if (live) R.string.status_live else R.string.status_paused).uppercase(),
            style = Aapps.type.label.copy(color = c.fgMuted),
            maxLines = 1,
        )
    }
}

@Composable
private fun MainAction(mic: MicState, live: Boolean, haptics: Haptics?, onToggle: () -> Unit, onMic: () -> Unit) {
    val (text, icon) = when (mic) {
        // nichts zu tun: das System schaltet frei
        MicState.SILENCED -> return
        MicState.READY -> if (live) R.string.pause to LoudrIcons.Pause else R.string.resume to LoudrIcons.Play
        MicState.ASK -> R.string.mic_allow to LoudrIcons.Mic
        MicState.BLOCKED -> R.string.mic_settings to LoudrIcons.Mic
        MicState.FAILED -> R.string.mic_retry to LoudrIcons.Reset
    }
    AappsPillButton(
        stringResource(text),
        {
            haptics?.confirm()
            if (mic == MicState.READY) onToggle() else onMic()
        },
        Modifier.widthIn(min = 176.dp),
        icon = icon,
        active = !(mic == MicState.READY && live),
    )
}

@Composable
private fun Prompt(mic: MicState) {
    val c = Aapps.colors
    val (title, text) = when (mic) {
        MicState.BLOCKED -> R.string.mic_blocked_title to R.string.mic_blocked_text
        MicState.FAILED -> R.string.mic_failed_title to R.string.mic_failed_text
        MicState.SILENCED -> R.string.mic_silenced_title to R.string.mic_silenced_text
        else -> R.string.mic_ask_title to R.string.mic_ask_text
    }
    Column(Modifier.padding(horizontal = 32.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(stringResource(title), style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center))
        Spacer(Modifier.height(8.dp))
        BasicText(stringResource(text), style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center))
    }
}

@Composable
private fun Footnote(offset: Float) {
    val text = if (offset == 0f) {
        stringResource(R.string.footnote)
    } else {
        stringResource(R.string.footnote_cal, Calibration.label(offset, decimalSeparator()))
    }
    BasicText(
        text,
        Modifier.padding(horizontal = 24.dp),
        // waechst mit der Schrift nur bis 16 sp: der Hinweis bleibt eine Nebenzeile
        style = Aapps.type.body.copy(
            color = Aapps.colors.fgMuted,
            fontSize = cappedSp(12.6f, 16f),
            lineHeight = 1.3.em,
            textAlign = TextAlign.Center,
        ),
    )
}

private fun describe(res: android.content.res.Resources, r: Reading, s: Settings): String {
    val level = Calibration.apply(r.level(s.weighting), s.offset)
    val ex = Example.of(level) ?: return res.getString(R.string.level_none)
    return res.getString(R.string.level_state, formatDb(level), res.getString(exampleRes(ex)))
}
