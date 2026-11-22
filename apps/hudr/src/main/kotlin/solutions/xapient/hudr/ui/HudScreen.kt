package solutions.xapient.hudr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.hudr.R
import solutions.xapient.hudr.Settings
import solutions.xapient.hudr.domain.GpsPhase
import solutions.xapient.hudr.domain.Reading

/**
 * HUD: dasselbe Layout wie der Tacho, reduziert auf Ziffern und Band, reines Schwarz,
 * auf Wunsch gespiegelt fuer die Windschutzscheibe. Tippen zeigt kurz "HUD beenden"; Zurueck beendet ebenfalls.
 */
@Composable
fun HudScreen(
    reading: () -> Reading,
    version: IntState,
    settings: Settings,
    haptics: Haptics?,
    onExit: () -> Unit,
    animate: Boolean = true,
    showExit: Boolean = false,
) {
    val (display, update) = rememberSpeedDisplay(reading, version, settings, animate)
    KeepScreenOn()
    var exit by rememberSaveable { mutableStateOf(showExit) }
    // Knopf blendet nach 3 s wieder aus
    LaunchedEffect(exit, animate) {
        if (exit && animate) {
            delay(3_000)
            exit = false
        }
    }
    val unit = stringResource(unitRes(settings.unit))
    val searching = stringResource(R.string.gps_searching)
    val lost = stringResource(R.string.gps_lost)
    val desc = stringResource(R.string.speed)
    val showLabel = stringResource(R.string.hud_show_exit)
    val spoken = rememberSpoken(reading, version, settings)
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val status: (GpsPhase) -> String = { if (it == GpsPhase.LOST) lost else searching }
    Box(
        Modifier
            .fillMaxSize()
            .background(HudBg)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = showLabel) {
                haptics?.tick()
                exit = !exit
            }
            .semantics {
                contentDescription = desc
                stateDescription = spoken
            },
    ) {
        // horizontal gespiegelt: in der Scheibe wieder richtig herum
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp)
                .graphicsLayer { scaleX = if (settings.mirror) -1f else 1f },
        ) {
            if (landscape) {
                Row(Modifier.fillMaxSize()) {
                    Digits(display, update, unit, HudPalette, Modifier.weight(1f).fillMaxHeight(), status)
                    Spacer(Modifier.width(16.dp))
                    SpeedTape(display, update, true, settings.activeLimit, HudPalette, Modifier.width(TAPE_WIDTH).fillMaxHeight())
                }
            } else {
                // hochkant: Ziffern nehmen die verfuegbare Hoehe, Band darunter
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Digits(display, update, unit, HudPalette, Modifier.weight(1f, fill = false).fillMaxWidth().aspectRatio(DIGITS_RATIO), status)
                    Spacer(Modifier.height(16.dp))
                    SpeedTape(display, update, false, settings.activeLimit, HudPalette, Modifier.fillMaxWidth().height(120.dp))
                }
            }
        }
        if (exit) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp), contentAlignment = Alignment.BottomCenter) {
                AappsPillButton(stringResource(R.string.hud_exit), {
                    haptics?.confirm()
                    onExit()
                }, icon = HudrIcons.Close)
            }
        }
    }
}
