package solutions.xapient.roundr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.roundr.R
import solutions.xapient.roundr.domain.TimeFormat
import solutions.xapient.roundr.domain.Timeline
import solutions.xapient.roundr.domain.Workout

// Kurzform: „8 × 0:20 / 0:10" bzw. mit Saetzen „3 × 8 × …"
@Composable
fun summary(w: Workout): String {
    val sets = if (w.sets > 1) "${w.sets} × " else ""
    val rest = if (w.restSec > 0) " / ${TimeFormat.clock(w.restSec)}" else ""
    return "$sets${w.rounds} × ${TimeFormat.clock(w.workSec)}$rest"
}

// Start: Ring des Trainings als Hero, Werte darunter, eine Hauptaktion
@Composable
fun HomeScreen(
    workout: Workout,
    onLibrary: () -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onStart: () -> Unit,
) {
    val timeline = remember(workout) { Timeline(workout) }
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val bigFont = LocalDensity.current.fontScale > 1.5f
    AappsScaffold {
        val hero: @Composable (Modifier) -> Unit = { m ->
            Box(m, contentAlignment = Alignment.Center) {
                Box(Modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
                    WorkoutRing(timeline, Modifier.fillMaxSize())
                    RingCenter(workout, timeline, Modifier.fillMaxSize(0.62f))
                }
            }
        }
        val start: @Composable () -> Unit = {
            AappsPillButton(
                stringResource(R.string.start), onStart, Modifier.widthIn(min = 176.dp),
                icon = if (landscape && bigFont) null else RoundrIcons.Play, active = true,
            )
        }
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(if (bigFont) 0.55f else 0.45f).fillMaxHeight().padding(start = 12.dp, bottom = 16.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { NameChip(workout, onLibrary) }
                        Gear(onSettings)
                    }
                    Spacer(Modifier.weight(1f))
                    Values(workout, onEdit, Modifier.fillMaxWidth().padding(end = 8.dp))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { start() }
                }
                hero(Modifier.weight(if (bigFont) 0.45f else 0.55f).fillMaxHeight().padding(20.dp))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) { NameChip(workout, onLibrary) }
                    Gear(onSettings)
                }
                hero(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp))
                Values(workout, onEdit, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                Box(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp), contentAlignment = Alignment.Center) { start() }
            }
        }
    }
}

@Composable
private fun Gear(onClick: () -> Unit) {
    AappsIconButton(AappsIcons.Gear, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_settings), onClick)
}

// Gesamtzeit in der Ringmitte
@Composable
private fun RingCenter(w: Workout, t: Timeline, modifier: Modifier) {
    val c = Aapps.colors
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            stringResource(R.string.total).uppercase(),
            style = Aapps.type.label.copy(color = c.fgMuted, textAlign = TextAlign.Center),
            maxLines = 1,
        )
        BasicText(
            TimeFormat.clock(TimeFormat.ceilSec(t.totalMs)),
            Modifier.fillMaxWidth(),
            style = Aapps.type.display.copy(color = c.fg, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 88.sp, stepSize = 2.sp),
        )
        BasicText(
            summary(w),
            style = Aapps.type.number.copy(color = c.fgMuted, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 20.sp, stepSize = 0.5.sp),
        )
    }
}

// Name des Trainings als Chip, oeffnet die Vorlagen
@Composable
private fun NameChip(w: Workout, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val desc = stringResource(R.string.library)
    Row(
        Modifier
            .pressScale(pressed, 0.96f)
            .heightIn(min = 48.dp)
            .clickable(source, indication = null, role = Role.Button, onClickLabel = desc, onClick = onClick)
            .padding(vertical = 4.dp)
            .background(if (pressed) c.surfaceHigh else Color.Transparent, RoundedCornerShape(24.dp))
            .border(1.dp, if (pressed) c.fgFaint else c.line, RoundedCornerShape(24.dp))
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            workoutName(w),
            Modifier.weight(1f, fill = false),
            style = Aapps.type.title.copy(color = c.fg),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(6.dp))
        AappsIcon(RoundrIcons.Down, null, c.accent, size = 20.dp)
    }
}

// Arbeit, Pause, Runden; Tippen oeffnet Bearbeiten
@Composable
private fun Values(w: Workout, onEdit: () -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val desc = stringResource(R.string.edit)
    Row(
        modifier
            .pressScale(pressed, 0.97f)
            .background(if (pressed) c.surfaceHigh else c.surface, RoundedCornerShape(22.dp))
            .border(1.dp, c.line, RoundedCornerShape(22.dp))
            .clickable(source, indication = null, role = Role.Button, onClickLabel = desc, onClick = onEdit)
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 14.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Value(stringResource(R.string.phase_work), TimeFormat.clock(w.workSec), Ember, Modifier.weight(1f))
        Divider()
        Value(stringResource(R.string.phase_rest), TimeFormat.clock(w.restSec), Ice, Modifier.weight(1f))
        Divider()
        val rounds = if (w.sets > 1) "${w.sets}×${w.rounds}" else w.rounds.toString()
        Value(stringResource(R.string.rounds), rounds, null, Modifier.weight(1f))
    }
}

@Composable
private fun Divider() {
    Box(Modifier.width(1.dp).height(40.dp).background(Aapps.colors.line))
}

@Composable
private fun Value(label: String, value: String, dot: Color?, modifier: Modifier) {
    val c = Aapps.colors
    Column(modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(Modifier.size(8.dp).background(dot, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            BasicText(
                label.uppercase(),
                style = Aapps.type.label.copy(color = c.fgMuted),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = Aapps.type.label.fontSize, stepSize = 0.5.sp),
            )
        }
        Spacer(Modifier.height(4.dp))
        BasicText(
            value,
            style = Aapps.type.number.copy(color = c.fg, fontSize = 26.sp, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 26.sp, stepSize = 1.sp),
        )
    }
}
