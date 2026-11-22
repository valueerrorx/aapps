package com.valueerrorx.spinr.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valueerrorx.aapps.core.ui.Aapps
import com.valueerrorx.aapps.core.ui.AappsIcon
import com.valueerrorx.aapps.core.ui.AappsIconButton
import com.valueerrorx.aapps.core.ui.AappsIcons
import com.valueerrorx.aapps.core.ui.AappsScaffold
import com.valueerrorx.aapps.core.ui.AappsTopBar
import com.valueerrorx.aapps.core.ui.SettingsAction
import com.valueerrorx.aapps.core.ui.SettingsDivider
import com.valueerrorx.aapps.core.ui.SettingsGroup
import com.valueerrorx.aapps.core.ui.Springs
import com.valueerrorx.spinr.R
import com.valueerrorx.spinr.TemplateGroup
import com.valueerrorx.spinr.domain.NumberRange
import com.valueerrorx.spinr.domain.Wheel
import kotlinx.coroutines.delay

// Scrollbare Liste mit eigener Kopfzeile (wie SettingsScreen, aber mit Titel)
@Composable
private fun ListScreen(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AappsScaffold {
        Column(Modifier.fillMaxWidth()) {
            AappsTopBar(title, onBack, stringResource(com.valueerrorx.aapps.core.ui.R.string.aapps_back))
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

// Kleine Farbvorschau eines Rads
@Composable
private fun Swatch(wheel: Wheel) {
    val n = wheel.labels.size
    Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
        repeat(minOf(n, 4)) { i ->
            Box(Modifier.size(14.dp).background(Aapps.colors.surface, CircleShape).padding(1.5.dp).background(segmentColor(i, n), CircleShape))
        }
        if (n == 0) Box(Modifier.size(14.dp).background(Aapps.colors.fgFaint, CircleShape))
    }
}

@Composable
fun WheelsScreen(
    wheels: List<Wheel>,
    active: Int,
    onSelect: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onNew: () -> Unit,
    onTemplates: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Aapps.colors
    ListScreen(stringResource(R.string.wheels), onBack) {
        SettingsGroup(null) {
            wheels.forEachIndexed { i, wheel ->
                if (i > 0) SettingsDivider()
                val selected = i == active
                val check by animateColorAsState(if (selected) c.accent else Color.Transparent, Springs.gentle(), label = "check")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .weight(1f)
                            .selectable(selected, indication = null, interactionSource = null, role = Role.RadioButton) { onSelect(i) }
                            .heightIn(min = 64.dp)
                            .padding(start = 18.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AappsIcon(AappsIcons.Check, null, check, size = 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            BasicText(
                                wheel.name.ifBlank { "–" },
                                style = Aapps.type.body.copy(color = if (selected) c.accent else c.fg),
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            BasicText(
                                pluralStringResource(R.plurals.entries_count, wheel.labels.size, wheel.labels.size),
                                style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Swatch(wheel)
                    }
                    AappsIconButton(SpinrIcons.Pencil, stringResource(R.string.edit_named, wheel.name), { onEdit(i) })
                    Spacer(Modifier.width(6.dp))
                }
            }
            SettingsDivider()
            AddRow(stringResource(R.string.new_wheel), true, onNew)
            SettingsDivider()
            SettingsAction(stringResource(R.string.from_template), onClick = onTemplates)
        }
    }
}

// Vorschau der Eintraege; lange Listen gekuerzt
private fun preview(labels: List<String>): String {
    val all = labels.joinToString(" · ")
    if (all.length <= PREVIEW_LENGTH || labels.size <= 4) return all
    // so viele vom Anfang, wie mit "… · letzter" passen, mindestens einer
    var k = 1
    while (k < labels.size - 2 && (labels.take(k + 1) + "…" + labels.last()).joinToString(" · ").length <= PREVIEW_LENGTH) k++
    return (labels.take(k) + "…" + labels.last()).joinToString(" · ")
}

private const val PREVIEW_LENGTH = 34

// Eine Vorlage: Name, Vorschau, Farbpunkte
@Composable
private fun TemplateRow(wheel: Wheel, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) c.surfaceHigh else Color.Transparent, Springs.gentle(), label = "tplBg")
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(wheel.name, style = Aapps.type.body.copy(color = c.fg))
            Spacer(Modifier.height(2.dp))
            BasicText(
                preview(wheel.labels),
                style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f),
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Swatch(wheel)
        Spacer(Modifier.width(10.dp))
        AappsIcon(SpinrIcons.Plus, null, c.accent, size = 20.dp)
    }
}

// Zahlenbereich von..bis als neues Rad
@Composable
private fun RangeRow(onAdd: (Wheel) -> Unit) {
    val c = Aapps.colors
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    val entries = NumberRange.parse(from, to)
    val name = "${from.trim()} – ${to.trim()}"
    // Fehler erst zeigen, wenn beide Felder gefuellt sind
    val invalid = entries == null && from.isNotBlank() && to.isNotBlank()
    val box = Modifier.background(c.bg, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp)
    Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 12.dp, bottom = 10.dp)) {
        BasicText(stringResource(R.string.range), style = Aapps.type.body.copy(color = c.fg))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Field(from, stringResource(R.string.range_from), { from = NumberRange.cleanInput(it) }, box.weight(1f), keyboardType = KeyboardType.Number)
            BasicText("–", Modifier.padding(horizontal = 10.dp), style = Aapps.type.body.copy(color = c.fgMuted))
            Field(
                to, stringResource(R.string.range_to), { to = NumberRange.cleanInput(it) }, box.weight(1f),
                ImeAction.Done, KeyboardType.Number,
            )
            Spacer(Modifier.width(4.dp))
            AappsIconButton(SpinrIcons.Plus, stringResource(R.string.range_add, name), {
                entries?.let { onAdd(Wheel(name, it)) }
            }, enabled = entries != null)
        }
        Spacer(Modifier.height(6.dp))
        BasicText(
            if (entries != null) pluralStringResource(R.plurals.entries_count, entries.size, entries.size)
            else stringResource(R.string.range_hint, Wheel.MAX_ENTRIES),
            style = Aapps.type.body.copy(color = if (invalid) Danger else c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f),
        )
    }
}

@Composable
fun TemplatesScreen(groups: List<TemplateGroup>, onAdd: (Wheel) -> Unit, onBack: () -> Unit) {
    ListScreen(stringResource(R.string.templates), onBack) {
        groups.forEach { group ->
            SettingsGroup(group.title) {
                group.wheels.forEachIndexed { i, t ->
                    if (i > 0) SettingsDivider()
                    TemplateRow(t) { onAdd(t) }
                }
                if (group.withRange) {
                    SettingsDivider()
                    RangeRow(onAdd)
                }
            }
        }
    }
}

@Composable
private fun AddRow(text: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) c.surfaceHigh else Color.Transparent, Springs.gentle(), label = "addBg")
    val fg = if (enabled) c.accent else c.fgFaint
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AappsIcon(SpinrIcons.Plus, null, fg, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        BasicText(text, style = Aapps.type.body.copy(color = fg))
    }
}

private val Danger = Color(0xFFFF8A9A)

// Schluessel der Editor-Zeilen (nicht beobachtet, aendert sich nur zusammen mit den Eintraegen)
private class RowIds {
    val ids = mutableListOf<Int>()
    var next = 0
}

// Zerstoerende Aktion: rot, ohne Chevron
@Composable
private fun DangerRow(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) c.surfaceHigh else Color.Transparent, Springs.gentle(), label = "dangerBg")
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Aapps.type.body.copy(color = if (enabled) Danger else c.fgFaint))
            Spacer(Modifier.height(2.dp))
            BasicText(subtitle, style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f))
        }
    }
}

// Ein Textfeld im Look der Einstellungszeilen
@Composable
private fun Field(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val c = Aapps.colors
    val style = Aapps.type.body.copy(color = c.fg)
    BasicTextField(
        value = value,
        onValueChange = { onChange(Wheel.clean(it)) },
        modifier = modifier.semantics { contentDescription = placeholder },
        textStyle = style,
        singleLine = true,
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(KeyboardCapitalization.Sentences, keyboardType = keyboardType, imeAction = imeAction),
        decorationBox = { inner ->
            Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) BasicText(placeholder, style = style.copy(color = c.fgFaint), maxLines = 1)
                inner()
            }
        },
    )
}

@Composable
fun EditorScreen(
    wheel: Wheel,
    canDelete: Boolean,
    onChange: (Wheel) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Aapps.colors
    val entries = wheel.entries
    // Farbe je Eintrag wie auf dem Rad: leere zaehlen nicht mit
    val labelIndex = remember(entries) {
        var k = 0
        entries.map { if (it.isBlank()) -1 else k++ }
    }
    val n = wheel.labels.size
    // stabile Schluessel je Zeile, damit Fokus und Feldzustand beim Loeschen am Eintrag bleiben
    val rows = remember { RowIds() }
    val ids = rows.ids
    while (ids.size > entries.size) ids.removeAt(ids.lastIndex)
    while (ids.size < entries.size) ids.add(rows.next++)
    var focusId by remember { mutableIntStateOf(-1) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Bestaetigung verfaellt nach 3 s
    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            delay(3_000)
            confirmDelete = false
        }
    }

    ListScreen(stringResource(R.string.edit_wheel), onBack) {
        SettingsGroup(stringResource(R.string.name)) {
            Field(
                wheel.name, stringResource(R.string.name), { onChange(wheel.copy(name = it)) },
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
            )
        }
        // Zaehler wie das Limit: Zeilen im Editor
        SettingsGroup(stringResource(R.string.entries_label, entries.size, Wheel.MAX_ENTRIES)) {
            entries.forEachIndexed { i, entry ->
                if (i > 0) SettingsDivider()
                key(ids[i]) {
                    val focus = remember { FocusRequester() }
                    if (ids[i] == focusId) {
                        LaunchedEffect(Unit) {
                            focus.requestFocus()
                            focusId = -1
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val dot = if (labelIndex[i] >= 0) segmentColor(labelIndex[i], n) else c.fgFaint
                        Box(Modifier.size(12.dp).background(dot, CircleShape))
                        Spacer(Modifier.width(14.dp))
                        Field(
                            entry, stringResource(R.string.entry_hint, i + 1),
                            { v -> onChange(wheel.copy(entries = entries.toMutableList().also { it[i] = v })) },
                            Modifier.weight(1f).focusRequester(focus),
                            if (i == entries.lastIndex) ImeAction.Done else ImeAction.Next,
                        )
                        if (entries.size > Wheel.MIN_ENTRIES) {
                            AappsIconButton(SpinrIcons.Close, stringResource(R.string.delete_entry, i + 1), {
                                ids.removeAt(i)
                                onChange(wheel.copy(entries = entries.toMutableList().also { it.removeAt(i) }))
                            })
                        } else {
                            Spacer(Modifier.width(48.dp))
                        }
                    }
                }
            }
            SettingsDivider()
            AddRow(stringResource(R.string.add_entry), entries.size < Wheel.MAX_ENTRIES) {
                ids.add(rows.next)
                focusId = rows.next++
                onChange(wheel.copy(entries = entries + ""))
            }
        }
        SettingsGroup(null) {
            DangerRow(
                stringResource(if (confirmDelete) R.string.delete_confirm else R.string.delete_wheel),
                stringResource(if (canDelete) R.string.delete_wheel_sub else R.string.delete_last_sub),
                enabled = canDelete,
            ) {
                if (confirmDelete) onDelete() else confirmDelete = true
            }
        }
    }
}
