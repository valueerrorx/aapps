package solutions.xapient.sleepr.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.sleepr.COLORS
import solutions.xapient.sleepr.Display
import solutions.xapient.sleepr.Mix
import solutions.xapient.sleepr.Preset
import solutions.xapient.sleepr.describe
import solutions.xapient.sleepr.presetName
import solutions.xapient.sleepr.res.Res
import solutions.xapient.sleepr.res.explain_colors
import solutions.xapient.sleepr.res.explain_mixes
import solutions.xapient.sleepr.res.explain_timer
import solutions.xapient.sleepr.res.explain_volume
import solutions.xapient.sleepr.res.explain_waves
import solutions.xapient.sleepr.res.group_colors
import solutions.xapient.sleepr.res.group_display
import solutions.xapient.sleepr.res.group_mixes
import solutions.xapient.sleepr.res.group_timer
import solutions.xapient.sleepr.res.group_volume
import solutions.xapient.sleepr.res.group_waves
import solutions.xapient.sleepr.res.show_mixes
import solutions.xapient.sleepr.res.show_mixes_sub
import solutions.xapient.sleepr.res.show_waves
import solutions.xapient.sleepr.res.show_waves_sub
import solutions.xapient.sleepr.res.state_active

/** Einstellungen wie auf Android: Anzeige, Mischungen laden, Erklaerungen, Info */
@Composable
fun SleeprSettings(mix: Mix, display: Display, onPreset: (Preset) -> Unit, onAbout: () -> Unit, onBack: () -> Unit) {
    val c = Aapps.colors
    val small = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f)
    val muted = Aapps.type.body.copy(color = c.fgMuted)
    SettingsScreen(onBack = onBack) {
        SettingsGroup(stringResource(Res.string.group_display)) {
            SettingsToggle(stringResource(Res.string.show_waves), display.showWaves, { display.setWaves(it) }, stringResource(Res.string.show_waves_sub))
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.show_mixes), display.showMixes, { display.setMixes(it) }, stringResource(Res.string.show_mixes_sub))
        }
        SettingsGroup(stringResource(Res.string.group_mixes)) {
            BasicText(stringResource(Res.string.explain_mixes), Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 4.dp), style = small)
            Mix.PRESETS.forEach { p ->
                SettingsDivider()
                PresetRow(stringResource(presetName(p)), describe(p), mix.matches(p)) { onPreset(p) }
            }
        }
        SettingsGroup(stringResource(Res.string.group_colors)) {
            BasicText(stringResource(Res.string.explain_colors), Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp), style = muted)
            COLORS.forEachIndexed { i, col ->
                val name = stringResource(col.name)
                val slope = stringResource(col.slope)
                val explain = stringResource(col.explain)
                // Farbe als eine Ansage
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = if (i == 4) 16.dp else 2.dp)
                        .clearAndSetSemantics { contentDescription = "$name, $slope: $explain" },
                ) {
                    Spacer(Modifier.padding(top = 6.dp).size(10.dp).background(col.color, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        BasicText("$name · $slope", style = Aapps.type.body.copy(color = c.fg))
                        BasicText(explain, style = small)
                    }
                }
            }
        }
        Explain(Res.string.group_waves, Res.string.explain_waves)
        Explain(Res.string.group_volume, Res.string.explain_volume)
        Explain(Res.string.group_timer, Res.string.explain_timer)
        SettingsGroup(null) {
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
private fun Explain(group: org.jetbrains.compose.resources.StringResource, text: org.jetbrains.compose.resources.StringResource) {
    SettingsGroup(stringResource(group)) {
        BasicText(stringResource(text), Modifier.padding(18.dp, 16.dp), style = Aapps.type.body.copy(color = Aapps.colors.fgMuted))
    }
}

// Zeile wie SettingsAction, aktive Mischung mit Haken statt Pfeil
@Composable
private fun PresetRow(title: String, subtitle: String, checked: Boolean, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) c.surfaceHigh else Color.Transparent, Springs.gentle(), label = "rowBg")
    val active = stringResource(Res.string.state_active)
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { if (checked) stateDescription = active }
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Aapps.type.body.copy(color = c.fg))
            Spacer(Modifier.height(2.dp))
            BasicText(subtitle, style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f))
        }
        Spacer(Modifier.width(12.dp))
        AappsIcon(if (checked) AappsIcons.Check else AappsIcons.Chevron, null, if (checked) c.accent else c.fgMuted, size = 20.dp)
    }
}
