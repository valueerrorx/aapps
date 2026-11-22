package solutions.xapient.magnr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
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
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.magnr.R
import solutions.xapient.magnr.domain.Reading
import solutions.xapient.magnr.domain.Sensitivity
import androidx.compose.animation.core.animateFloatAsState

// Detektor: Radar als Hero, darunter Werte und Verlauf; Hauptaktion Nullen
@Composable
fun DetectorScreen(
    state: DetectorState,
    sensitivity: Sensitivity,
    available: Boolean,
    live: Boolean,
    sound: Boolean,
    keepOn: Boolean,
    haptics: Haptics?,
    onPause: () -> Unit,
    onZero: () -> Unit,
    onSound: () -> Unit,
    onSettings: () -> Unit,
    onIntro: () -> Unit,
    hintPhase: Float = -1f,
) {
    if (live && keepOn) KeepScreenOn()
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }

    // Vorlesen: hoechstens einmal pro Sekunde, nicht je Messwert
    val res = LocalResources.current
    val sep = decimalSeparator()
    var spoken by remember { mutableStateOf("") }
    LaunchedEffect(live, available) {
        while (true) {
            spoken = if (available) describe(res, state.shown, sep) else ""
            if (!live) break
            delay(1_000)
        }
    }

    val radar: @Composable (Modifier) -> Unit = { m ->
        val desc = stringResource(R.string.field)
        Radar(
            state, sensitivity, live, empty = !available,
            modifier = m.semantics {
                contentDescription = desc
                stateDescription = spoken
                liveRegion = LiveRegionMode.Polite
            },
        ) {
            if (!available) AappsIcon(MagnrIcons.Magnet, null, Aapps.colors.fgMuted, size = 56.dp)
        }
    }
    val top: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (available) Status(live)
            Spacer(Modifier.weight(1f))
            AappsIconButton(AappsIcons.Gear, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_settings), onSettings)
        }
    }
    val bottom: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AappsIconButton(
                if (live) MagnrIcons.Pause else MagnrIcons.Play,
                stringResource(if (live) R.string.pause else R.string.resume),
                {
                    haptics?.press()
                    onPause()
                },
            )
            Spacer(Modifier.weight(1f))
            // Text bleibt gleich: nichts springt beim Pausieren; pausiert nullt es neu und misst weiter
            AappsPillButton(
                stringResource(R.string.zero),
                {
                    haptics?.confirm()
                    onZero()
                },
                Modifier.widthIn(min = 160.dp),
                icon = AappsIcons.Target,
                active = live,
            )
            Spacer(Modifier.weight(1f))
            SoundButton(sound) {
                haptics?.press()
                onSound()
            }
        }
    }
    val chart: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            BasicText(
                stringResource(R.string.history).uppercase(),
                Modifier.padding(bottom = 6.dp),
                style = Aapps.type.label.copy(color = Aapps.colors.fgMuted, fontSize = cappedSp(12f, 16f)),
            )
            HistoryChart(state, live, Modifier.fillMaxWidth().weight(1f))
        }
    }
    val card: @Composable (Modifier) -> Unit = { m ->
        state.hint?.let { h -> HintCard(h, state::dismissHint, m, hintPhase) }
    }

    AappsScaffold {
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.46f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    top()
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (available) {
                                Stats(state, Modifier.fillMaxWidth())
                                Spacer(Modifier.height(16.dp))
                                // fill = false: sonst erzwingt weight die volle Hoehe und heightIn greift nicht
                                chart(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 130.dp))
                                Spacer(Modifier.height(10.dp))
                                Footnote(onIntro)
                            } else {
                                NoSensor()
                            }
                        }
                        card(Modifier)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (available) bottom()
                }
                radar(Modifier.weight(0.54f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                top()
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    val radarMax = maxHeight * 0.56f
                    // Radar und Werte als Gruppe mittig: kein Leerraum zwischen Hero und Ablesewert
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (available) {
                            radar(Modifier.padding(horizontal = 8.dp).heightIn(max = radarMax).aspectRatio(RADAR_ASPECT))
                            Stats(state, Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                            Spacer(Modifier.height(22.dp))
                            chart(Modifier.fillMaxWidth().padding(horizontal = 24.dp).weight(1f, fill = false).heightIn(max = 170.dp))
                            Spacer(Modifier.height(10.dp))
                            Footnote(onIntro)
                        } else {
                            radar(Modifier.fillMaxWidth(0.75f).heightIn(max = radarMax).aspectRatio(RADAR_ASPECT))
                            NoSensor()
                        }
                    }
                    card(Modifier.padding(bottom = 8.dp))
                }
                Spacer(Modifier.height(16.dp))
                if (available) bottom()
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

// Ton an/aus: Akzent-Kante wenn an
@Composable
private fun SoundButton(on: Boolean, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, Springs.bouncy(), label = "soundScale")
    val desc = stringResource(R.string.sound)
    val stateText = stringResource(if (on) R.string.on else R.string.off)
    Box(
        Modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(if (on) c.accent.copy(alpha = 0.14f) else c.bg, CircleShape)
            .border(1.dp, if (on) c.accent.copy(alpha = 0.6f) else c.bg, CircleShape)
            .clickable(source, indication = null, role = Role.Switch, onClick = onClick)
            .semantics {
                contentDescription = desc
                stateDescription = stateText
            },
        contentAlignment = Alignment.Center,
    ) {
        AappsIcon(if (on) MagnrIcons.Sound else MagnrIcons.Mute, null, if (on) c.accent else c.fgMuted, size = 24.dp)
    }
}

// Ehrlich: was gefunden wird; Tippen zeigt die Einfuehrung
@Composable
private fun Footnote(onIntro: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    BasicText(
        stringResource(R.string.footnote),
        Modifier
            .clickable(source, indication = null, role = Role.Button, onClick = onIntro)
            .heightIn(min = 48.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        // waechst mit der Schrift nur bis 16 sp: der Hinweis bleibt eine Nebenzeile
        style = Aapps.type.body.copy(
            color = Aapps.colors.fgMuted,
            fontSize = cappedSp(12.6f, 16f),
            lineHeight = 1.3.em,
            textAlign = TextAlign.Center,
        ),
    )
}

@Composable
private fun NoSensor() {
    val c = Aapps.colors
    Column(Modifier.padding(horizontal = 32.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(stringResource(R.string.no_sensor_title), style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center))
        Spacer(Modifier.height(8.dp))
        BasicText(stringResource(R.string.no_sensor_text), style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center))
    }
}

private fun describe(res: android.content.res.Resources, r: Reading, sep: Char): String {
    if (r.total.isNaN()) return res.getString(R.string.field_none)
    val total = formatUt(r.total, sep)
    if (r.saturated) return res.getString(R.string.field_limit, total)
    if (r.settling) return res.getString(R.string.field_settling, total)
    return res.getString(R.string.field_state, total, formatUt(r.dev, sep, signed = true), res.getString(levelRes(r.level)))
}
