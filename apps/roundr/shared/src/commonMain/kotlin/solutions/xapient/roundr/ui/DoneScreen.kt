package solutions.xapient.roundr.ui

import solutions.xapient.aapps.core.ui.BackEffect
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTextButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.roundr.res.*
import solutions.xapient.roundr.domain.TimeFormat
import solutions.xapient.roundr.domain.Timeline
import solutions.xapient.roundr.domain.Workout

// Fertig: voller Ring, Gesamtzeit, „Nochmal"; statisch
@Composable
fun DoneScreen(workout: Workout, onAgain: () -> Unit, onClose: () -> Unit) {
    BackEffect(onClose)
    val c = Aapps.colors
    val t = remember(workout) { Timeline(workout) }
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val bigFont = LocalDensity.current.fontScale > 1.5f
    val rounds = t.rounds * t.sets
    AappsScaffold {
        val hero: @Composable (Modifier) -> Unit = { m ->
            Box(m, contentAlignment = Alignment.Center) {
                Box(Modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
                    WorkoutRing(t, Modifier.fillMaxSize())
                    Column(
                        Modifier.fillMaxSize(0.6f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BasicText(
                            stringResource(Res.string.done).uppercase(),
                            Modifier.fillMaxWidth(),
                            style = Aapps.type.headline.copy(color = c.accent, textAlign = TextAlign.Center),
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 44.sp, stepSize = 1.sp),
                        )
                        Spacer(Modifier.height(6.dp))
                        BasicText(
                            TimeFormat.clock(TimeFormat.ceilSec(t.totalMs)),
                            Modifier.fillMaxWidth(),
                            style = Aapps.type.display.copy(color = c.fg, textAlign = TextAlign.Center),
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 72.sp, stepSize = 2.sp),
                        )
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            pluralStringResource(Res.plurals.rounds_done, rounds, rounds).uppercase(),
                            Modifier.fillMaxWidth(),
                            style = Aapps.type.label.copy(color = c.fgMuted, textAlign = TextAlign.Center),
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = Aapps.type.label.fontSize, stepSize = 0.5.sp),
                        )
                    }
                }
            }
        }
        val actions: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AappsPillButton(
                    stringResource(Res.string.again), onAgain, Modifier.widthIn(min = 176.dp),
                    icon = if (landscape && bigFont) null else AappsIcons.Rotate, active = true,
                )
                AappsTextButton(stringResource(Res.string.close), onClose)
            }
        }
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.45f).fillMaxHeight().padding(start = 12.dp, bottom = 12.dp)) {
                    Row(Modifier.heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                        AappsIconButton(RoundrIcons.Close, stringResource(Res.string.close), onClose)
                    }
                    BasicText(
                        workoutName(workout),
                        Modifier.padding(horizontal = 12.dp),
                        style = Aapps.type.title.copy(color = c.fgMuted),
                        maxLines = 2,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { actions() }
                }
                hero(Modifier.weight(0.55f).fillMaxHeight().padding(20.dp))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AappsIconButton(RoundrIcons.Close, stringResource(Res.string.close), onClose)
                    BasicText(
                        workoutName(workout),
                        Modifier.weight(1f).padding(horizontal = 8.dp),
                        style = Aapps.type.title.copy(color = c.fgMuted),
                        maxLines = 1,
                    )
                }
                // Ring und Aktionen als Gruppe leicht ueber der Mitte
                Spacer(Modifier.weight(0.4f))
                Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    hero(Modifier.weight(1f, fill = false).fillMaxWidth().padding(horizontal = 12.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { actions() }
                }
                Spacer(Modifier.weight(0.6f))
            }
        }
    }
}
