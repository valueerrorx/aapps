package solutions.xapient.roundr.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTopBar
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.roundr.R
import solutions.xapient.roundr.domain.Presets
import solutions.xapient.roundr.domain.TimeFormat
import solutions.xapient.roundr.domain.Timeline
import solutions.xapient.roundr.domain.Workout

// Vorlagen und eigene Trainings; Tippen waehlt, eigene loeschen mit zweitem Tipp
@Composable
fun LibraryScreen(
    current: Workout,
    saved: List<Workout>,
    onPick: (Workout) -> Unit,
    onDelete: (Workout) -> Unit,
    onBack: () -> Unit,
    confirmName: String? = null,
) {
    val c = Aapps.colors
    var confirm by rememberSaveable { mutableStateOf(confirmName) }
    if (confirm != null) LaunchedEffect(confirm) { delay(3000); confirm = null }
    AappsScaffold {
        Column(Modifier.fillMaxWidth()) {
            AappsTopBar(stringResource(R.string.library), onBack, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_back))
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SettingsGroup(stringResource(R.string.presets)) {
                    Presets.all.forEachIndexed { i, w ->
                        if (i > 0) SettingsDivider()
                        Item(w, w == current, null, onPick, null)
                    }
                }
                SettingsGroup(stringResource(R.string.saved)) {
                    if (saved.isEmpty()) {
                        BasicText(
                            stringResource(R.string.saved_empty),
                            Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                            style = Aapps.type.body.copy(color = c.fgMuted),
                        )
                    }
                    saved.forEachIndexed { i, w ->
                        if (i > 0) SettingsDivider()
                        Item(w, w == current, confirm == w.name, onPick) {
                            if (confirm == w.name) {
                                confirm = null
                                onDelete(w)
                            } else {
                                confirm = w.name
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Item(w: Workout, selected: Boolean, confirming: Boolean?, onPick: (Workout) -> Unit, onDelete: (() -> Unit)?) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) c.surfaceHigh else Color.Transparent, Springs.gentle(), label = "rowBg")
    val t = remember(w) { Timeline(w) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(source, indication = null, role = Role.Button) { onPick(w) }
            .semantics { this.selected = selected }
            .heightIn(min = 72.dp)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkoutRing(t, Modifier.size(40.dp), glow = false)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(workoutName(w), style = Aapps.type.body.copy(color = c.fg), maxLines = 2)
            Spacer(Modifier.height(2.dp))
            if (confirming == true) {
                BasicText(stringResource(R.string.delete_confirm), style = Aapps.type.body.copy(color = Amber, fontSize = Aapps.type.label.fontSize * 1.1f))
            } else {
                BasicText(
                    "${summary(w)} · ${TimeFormat.clock(TimeFormat.ceilSec(t.totalMs))}",
                    style = Aapps.type.number.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f),
                )
            }
        }
        if (selected) {
            AappsIcon(AappsIcons.Check, stringResource(R.string.selected), c.accent, Modifier.padding(horizontal = 12.dp), size = 22.dp)
        }
        if (onDelete != null) {
            AappsIconButton(RoundrIcons.Close, stringResource(R.string.delete_named, workoutName(w)), onDelete)
        }
    }
}
