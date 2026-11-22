package solutions.xapient.aapps.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Grundgeruest fuer Einstellungen: Kopfzeile, scrollbare Gruppen
@Composable
fun SettingsScreen(onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AappsScaffold {
        Column(Modifier.fillMaxWidth()) {
            AappsTopBar(stringResource(R.string.aapps_settings), onBack, stringResource(R.string.aapps_back))
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content,
            )
        }
    }
}

@Composable
fun SettingsGroup(label: String?, content: @Composable ColumnScope.() -> Unit) {
    val c = Aapps.colors
    Column {
        if (label != null) {
            BasicText(
                label.uppercase(),
                Modifier.padding(start = 12.dp, bottom = 8.dp),
                style = Aapps.type.label.copy(color = c.fgMuted),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(c.surface)
                .border(1.dp, c.line.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
            content = content,
        )
    }
}

@Composable
private fun RowTexts(title: String, subtitle: String?, enabled: Boolean, modifier: Modifier) {
    val c = Aapps.colors
    Column(modifier) {
        BasicText(title, style = Aapps.type.body.copy(color = if (enabled) c.fg else c.fgFaint))
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            BasicText(subtitle, style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f))
        }
    }
}

@Composable
fun SettingsAction(title: String, subtitle: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) Aapps.colors.surfaceHigh else Color.Transparent, Springs.gentle(), label = "rowBg")
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowTexts(title, subtitle, enabled, Modifier.weight(1f))
        AappsIcon(AappsIcons.Chevron, null, if (enabled) Aapps.colors.fgMuted else Aapps.colors.fgFaint, size = 20.dp)
    }
}

@Composable
fun SettingsToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, subtitle: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(checked, indication = null, interactionSource = null, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowTexts(title, subtitle, true, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        AappsSwitch(checked)
    }
}

@Composable
private fun AappsSwitch(checked: Boolean) {
    val c = Aapps.colors
    val t by animateFloatAsState(if (checked) 1f else 0f, Springs.snappy(), label = "switch")
    val track by animateColorAsState(if (checked) c.accent.copy(alpha = 0.22f) else c.bg, Springs.gentle(), label = "track")
    val thumb by animateColorAsState(if (checked) c.accent else c.fgFaint, Springs.gentle(), label = "thumb")
    Box(
        Modifier
            .size(width = 48.dp, height = 28.dp)
            .background(track, CircleShape)
            .border(1.dp, if (checked) c.accent.copy(alpha = 0.5f) else c.line, CircleShape)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset { IntOffset((20.dp * t).roundToPx(), 0) }
                .size(20.dp)
                .glow(c.accent, 22.dp, 0.5f * t)
                .background(thumb, CircleShape),
        )
    }
}

// Auswahl als Segment-Leiste unter dem Titel
@Composable
fun <T> SettingsChoice(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val c = Aapps.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
        BasicText(title, style = Aapps.type.body.copy(color = c.fg))
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().background(c.bg, CircleShape).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (value, label) ->
                val isSel = value == selected
                val bg by animateColorAsState(if (isSel) c.accent else Color.Transparent, Springs.gentle(), label = "seg")
                val fg by animateColorAsState(if (isSel) c.onAccent else c.fgMuted, Springs.gentle(), label = "segFg")
                Box(
                    Modifier
                        .weight(1f)
                        .widthIn(min = 48.dp)
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .selectable(isSel, indication = null, interactionSource = null, role = Role.RadioButton) { onSelect(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    // einzeilig, bei grosser Schrift kleiner statt Umbruch mitten im Wort
                    BasicText(
                        label,
                        Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        style = Aapps.type.number.copy(color = fg, fontSize = Aapps.type.body.fontSize, textAlign = TextAlign.Center),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = Aapps.type.body.fontSize, stepSize = 0.5.sp),
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(1.dp).background(Aapps.colors.line.copy(alpha = 0.6f)))
}
