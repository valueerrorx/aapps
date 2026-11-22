package solutions.xapient.hudr.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.hudr.R
import solutions.xapient.hudr.Settings
import solutions.xapient.hudr.domain.Alert
import solutions.xapient.hudr.domain.GpsPhase
import solutions.xapient.hudr.domain.Reading
import solutions.xapient.hudr.domain.formatSpeed

enum class LocState { READY, ASK, COARSE, BLOCKED, GPS_OFF }

// Stand je Bild fuer Tacho und HUD; Bilder nur nach neuem Fix, bis die Feder ruht
@Composable
fun rememberSpeedDisplay(reading: () -> Reading, version: IntState, settings: Settings, animate: Boolean): Pair<SpeedDisplay, () -> Unit> {
    val source by rememberUpdatedState(reading)
    val display = remember { SpeedDisplay { source() } }
    val frame = remember { mutableLongStateOf(0L) }
    val key by rememberUpdatedState(settings.hashCode())
    // Feder je Bild in der Schleife, die Zeichenphase liest nur; Schluss, sobald sie ruht
    LaunchedEffect(display, animate) {
        if (!animate) return@LaunchedEffect
        snapshotFlow { version.intValue * 31 + key }.collect {
            do {
                withFrameNanos {
                    display.at(it, version.intValue * 31 + key, true)
                    frame.longValue = it
                }
            } while (!display.settled)
        }
    }
    // Version und Bildzeit nur hier gelesen, also nur in der Zeichenphase
    return display to { display.at(frame.longValue, version.intValue * 31 + key, animate) }
}

// Tacho: Bogen als Hero, darunter Fahrtwerte, eine Hauptaktion (HUD)
@Composable
fun SpeedScreen(
    reading: () -> Reading,
    version: IntState,
    loc: LocState,
    settings: Settings,
    haptics: Haptics?,
    onHud: () -> Unit,
    onReset: () -> Unit,
    onLoc: () -> Unit,
    onSettings: () -> Unit,
    animate: Boolean = true,
) {
    val ready = loc == LocState.READY
    val (display, update) = rememberSpeedDisplay(reading, version, settings, animate && ready)
    if (ready && settings.keepOn) KeepScreenOn()
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }

    val spoken = if (ready) rememberSpoken(reading, version, settings) else ""

    val dial: @Composable (Modifier) -> Unit = { m ->
        val desc = stringResource(R.string.speed)
        Dial(
            display, update, settings.unit, settings.activeLimit, empty = !ready,
            modifier = m.semantics {
                contentDescription = desc
                stateDescription = spoken
                liveRegion = LiveRegionMode.Polite
            },
        ) {
            if (!ready) AappsIcon(HudrIcons.Pin, null, Aapps.colors.fgMuted, size = 56.dp)
        }
    }
    val top: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (ready) GpsStatus(display, update, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
            AappsIconButton(AappsIcons.Gear, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_settings), onSettings)
        }
    }
    val bottom: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AappsIconButton(HudrIcons.Reset, stringResource(R.string.reset), {
                haptics?.confirm()
                onReset()
            })
            Spacer(Modifier.weight(1f))
            AappsPillButton(
                stringResource(R.string.hud),
                {
                    haptics?.confirm()
                    onHud()
                },
                Modifier.widthIn(min = 176.dp),
                icon = HudrIcons.Hud,
                active = true,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }
    }
    val stats: @Composable (Modifier) -> Unit = { m -> Stats(display, update, settings.unit, m) }

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
                            stats(Modifier.glass().padding(vertical = 18.dp, horizontal = 8.dp).fillMaxWidth())
                            Spacer(Modifier.height(16.dp))
                            Footnote()
                        } else {
                            // Erklaerung und Aktion als Gruppe, Knopf ueber die ganze Spalte
                            Prompt(loc)
                            Spacer(Modifier.height(16.dp))
                            LocAction(loc, haptics, onLoc, fill = true)
                        }
                    }
                    Spacer(Modifier.height(if (ready) 12.dp else 16.dp))
                    if (ready) bottom()
                }
                dial(Modifier.weight(0.54f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                top()
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val dialMax = maxHeight * 0.68f
                    Column(
                        Modifier.fillMaxSize(),
                        // hohe Schirme: Luft gleichmaessig um Bogen und Werte statt eines Lochs
                        verticalArrangement = if (ready) Arrangement.SpaceEvenly else Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (ready) {
                            dial(Modifier.padding(horizontal = 4.dp).heightIn(max = dialMax).aspectRatio(DIAL_ASPECT))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                stats(Modifier.padding(horizontal = 16.dp).glass().padding(vertical = 18.dp, horizontal = 8.dp).fillMaxWidth())
                                Spacer(Modifier.height(16.dp))
                                Footnote()
                            }
                        } else {
                            // ohne Standort: kleinerer Bogen, Erklaerung und Aktion direkt darunter
                            dial(Modifier.fillMaxWidth(0.75f).heightIn(max = dialMax * 0.8f).aspectRatio(DIAL_ASPECT))
                            Prompt(loc)
                            Spacer(Modifier.height(16.dp))
                            Box(Modifier.padding(horizontal = 24.dp)) { LocAction(loc, haptics, onLoc) }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (ready) bottom()
            }
        }
    }
}

// Glas laut DESIGN.md: dunkle Fuellung, feine Kante, heller Reflexstreifen oben
@Composable
private fun Modifier.glass(): Modifier {
    val c = Aapps.colors
    return drawWithCache {
        val radius = CornerRadius(22.dp.toPx())
        val edge = 1.dp.toPx()
        val shine = Brush.horizontalGradient(
            0f to c.fg.copy(alpha = 0f),
            0.5f to c.fg.copy(alpha = 0.14f),
            1f to c.fg.copy(alpha = 0f),
        )
        onDrawBehind {
            drawRoundRect(c.surface, cornerRadius = radius)
            drawRoundRect(c.line, cornerRadius = radius, style = Stroke(edge))
            drawLine(shine, Offset(radius.x, edge), Offset(size.width - radius.x, edge), edge)
        }
    }
}

@Composable
private fun LocAction(loc: LocState, haptics: Haptics?, onLoc: () -> Unit, fill: Boolean = false) {
    // grosse Schrift: Kurztext, damit die Pille einzeilig bleibt
    val short = LocalDensity.current.fontScale > 1.3f
    val text = when (loc) {
        LocState.READY -> return
        LocState.ASK -> if (short) R.string.loc_allow_short else R.string.loc_allow
        LocState.COARSE -> if (short) R.string.loc_precise_short else R.string.loc_precise
        LocState.BLOCKED -> if (short) R.string.loc_settings_short else R.string.loc_settings
        LocState.GPS_OFF -> if (short) R.string.gps_turn_on_short else R.string.gps_turn_on
    }
    AappsPillButton(
        stringResource(text),
        {
            haptics?.confirm()
            onLoc()
        },
        if (fill) Modifier.fillMaxWidth() else Modifier.widthIn(min = 176.dp),
        icon = HudrIcons.Pin,
        active = true,
    )
}

@Composable
private fun Prompt(loc: LocState) {
    val c = Aapps.colors
    val (title, text) = when (loc) {
        LocState.COARSE -> R.string.loc_coarse_title to R.string.loc_coarse_text
        LocState.BLOCKED -> R.string.loc_blocked_title to R.string.loc_blocked_text
        LocState.GPS_OFF -> R.string.gps_off_title to R.string.gps_off_text
        else -> R.string.loc_ask_title to R.string.loc_ask_text
    }
    Column(Modifier.padding(horizontal = 32.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(stringResource(title), style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center))
        Spacer(Modifier.height(8.dp))
        BasicText(stringResource(text), style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center))
    }
}

@Composable
private fun Footnote() {
    BasicText(
        stringResource(R.string.footnote),
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

// Vorlesen: nur nach neuem Stand und hoechstens einmal pro Sekunde; im Leerlauf nichts
@Composable
fun rememberSpoken(reading: () -> Reading, version: IntState, settings: Settings): String {
    val res = LocalResources.current
    val source by rememberUpdatedState(reading)
    var spoken by remember { mutableStateOf("") }
    LaunchedEffect(settings) {
        snapshotFlow { version.intValue }.conflate().collect {
            spoken = describe(res, source(), settings)
            delay(1_000)
        }
    }
    return spoken
}

fun describe(res: android.content.res.Resources, r: Reading, s: Settings): String {
    if (r.phase != GpsPhase.FIX || r.speed.isNaN()) return res.getString(R.string.speed_none)
    val unit = res.getString(unitRes(s.unit))
    val v = formatSpeed(r.speed)
    return res.getString(if (r.alert == Alert.OVER) R.string.speed_over else R.string.speed_state, v, unit)
}
