package solutions.xapient.hudr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.hudr.R
import solutions.xapient.hudr.Settings
import solutions.xapient.hudr.domain.GpsPhase
import solutions.xapient.hudr.domain.Reading
import solutions.xapient.hudr.domain.formatSpeed
import kotlin.math.min

/**
 * HUD: reines Schwarz, riesige Zahl, auf Wunsch gespiegelt fuer die Windschutzscheibe.
 * Tippen zeigt kurz "HUD beenden"; Zurueck beendet ebenfalls (in der App).
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
    val measurer = rememberTextMeasurer()
    val type = Aapps.type
    val unitText = stringResource(unitRes(settings.unit)).uppercase()
    val searching = stringResource(R.string.gps_searching).uppercase()
    val lost = stringResource(R.string.gps_lost).uppercase()
    val desc = stringResource(R.string.speed)
    val spoken = rememberSpoken(reading, version, settings)
    Box(
        Modifier
            .fillMaxSize()
            .background(HudBg)
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                haptics?.tick()
                exit = !exit
            }
            .semantics {
                contentDescription = desc
                stateDescription = spoken
            }
            .drawWithCache {
                // Zahl fest aus "188": bei Wechsel 9 -> 10 -> 100 springt nichts
                val probeStyle = type.display.copy(fontSize = 100.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
                val probe = measurer.measure("188", probeStyle)
                val unitProbe = measurer.measure(unitText, type.label.copy(fontSize = 100.sp))
                // Zahl hoechstens 92 % breit und 68 % hoch, darunter Platz fuer die Einheit
                val kNum = min(size.width * 0.92f / probe.size.width, size.height * 0.68f / probe.size.height)
                val numStyle = probeStyle.copy(fontSize = 100.sp * kNum)
                val numH = probe.size.height * kNum
                // Einheit etwa ein Achtel der Zahlhoehe, mindestens 16 dp hoch
                val unitPx = maxOf(numH * 0.12f, 16.dp.toPx())
                val unitStyle = type.label.copy(fontSize = 100.sp * (unitPx / unitProbe.size.height))
                val unitL = measurer.measure(unitText, unitStyle)
                val statusSearch = measurer.measure(searching, unitStyle)
                val statusLost = measurer.measure(lost, unitStyle)
                val gap = numH * 0.02f
                val blockH = numH + gap + unitL.size.height
                val top = (size.height - blockH) / 2
                onDrawBehind {
                    update()
                    val r = display.reading ?: return@onDrawBehind
                    val col = hudColor(r.alert)
                    val muted = HudGreen.copy(alpha = HUD_MUTED_ALPHA)
                    val mirror = settings.mirror
                    // horizontal gespiegelt: in der Scheibe wieder richtig herum
                    scale(if (mirror) -1f else 1f, 1f) {
                        val live = r.phase == GpsPhase.FIX
                        val num = measurer.measure(formatSpeed(r.speed), numStyle)
                        drawText(num, if (live) col else muted, Offset((size.width - num.size.width) / 2, top))
                        val under = when (r.phase) {
                            GpsPhase.FIX -> unitL
                            GpsPhase.SEARCHING -> statusSearch
                            GpsPhase.LOST -> statusLost
                        }
                        drawText(under, muted, Offset((size.width - under.size.width) / 2, top + numH + gap))
                    }
                }
            },
    ) {
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
