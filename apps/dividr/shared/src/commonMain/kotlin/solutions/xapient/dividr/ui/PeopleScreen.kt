package solutions.xapient.dividr.ui

import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_back
import solutions.xapient.dividr.res.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTopBar
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.activeMembers
import solutions.xapient.dividr.domain.displayName
import solutions.xapient.dividr.domain.references
import solutions.xapient.dividr.domain.resolve

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeopleScreen(
    group: Group,
    me: Long?,
    onRename: (Long, String) -> Unit,
    onFromStart: (Long, Boolean) -> Unit,
    onMerge: (Long, Long) -> Unit,
    onUnmerge: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onMe: (Long) -> Unit,
    onAdd: (String) -> Unit,
    onBack: () -> Unit,
    open: Long = 0L,
) {
    val c = Aapps.colors
    val people = group.activeMembers()
    val merged = group.members.values.filter { !it.removed && it.alias != null && group.resolve(it.id) != it.id }.sortedBy { it.id }
    var expanded by remember { mutableLongStateOf(open) }
    val mine = me?.let { group.resolve(it) }
    ListScreen(stringResource(Res.string.people), onBack) {
        SettingsGroup(pluralStringResource(Res.plurals.n_people, people.size, people.size)) {
            people.forEachIndexed { i, p ->
                if (i > 0) SettingsDivider()
                Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        NameField(p.name, stringResource(Res.string.name_of, group.displayName(p.id))) { onRename(p.id, it) }
                        val tags = listOfNotNull(
                            stringResource(Res.string.you).takeIf { p.id == mine },
                            stringResource(Res.string.counts_from_now).takeIf { !p.fromStart },
                        )
                        if (tags.isNotEmpty()) {
                            BasicText(tags.joinToString(" · "), Modifier.padding(bottom = 10.dp), style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f))
                        }
                    }
                    AappsIconButton(
                        if (expanded == p.id) DividrIcons.Down else AappsIcons.Chevron,
                        stringResource(Res.string.options_for, group.displayName(p.id)),
                        { expanded = if (expanded == p.id) 0L else p.id },
                    )
                }
                AnimatedVisibility(expanded == p.id, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                        Segment(listOf(true to stringResource(Res.string.from_start), false to stringResource(Res.string.from_now)), p.fromStart) {
                            onFromStart(p.id, it)
                        }
                        Spacer(Modifier.padding(top = 10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (p.id != mine) SmallAction(stringResource(Res.string.this_is_me), { onMe(p.id) })
                            val refs = group.references(p.id)
                            if (p.id != mine && refs == 0) SmallAction(stringResource(Res.string.remove), { expanded = 0L; onRemove(p.id) }, icon = DividrIcons.Close, color = Warn)
                        }
                        if (people.size > 1) {
                            Spacer(Modifier.padding(top = 10.dp))
                            SectionLabel(stringResource(Res.string.merge_into), Modifier.padding(start = 4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                people.filter { it.id != p.id }.forEach { o ->
                                    Chip(group.displayName(o.id), false, { expanded = 0L; onMerge(p.id, o.id) }, role = Role.Button)
                                }
                            }
                        }
                    }
                }
            }
            SettingsDivider()
            AddRow(onAdd)
        }
        if (merged.isNotEmpty()) {
            SettingsGroup(stringResource(Res.string.merged)) {
                merged.forEachIndexed { i, m ->
                    if (i > 0) SettingsDivider()
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 18.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            stringResource(Res.string.merged_line, m.name, group.displayName(m.id)),
                            Modifier.weight(1f),
                            style = Aapps.type.body.copy(color = c.fgMuted),
                        )
                        SmallAction(stringResource(Res.string.separate), { onUnmerge(m.id) })
                    }
                }
            }
        }
    }
}

// Name aendern: erst bei Fertig, Fokusverlust oder Verlassen uebernehmen
@Composable
private fun NameField(value: String, desc: String, onCommit: (String) -> Unit) {
    val c = Aapps.colors
    var text by remember(value) { mutableStateOf(value) }
    val current = rememberUpdatedState(text)
    val commit = rememberUpdatedState { if (current.value.isNotBlank() && current.value.trim() != value) onCommit(current.value) }
    DisposableEffect(Unit) { onDispose { commit.value() } }
    val focus = LocalFocusManager.current
    BasicTextField(
        value = text,
        onValueChange = { text = Group.cleanName(it) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 14.dp)
            .onFocusChanged { if (!it.isFocused) commit.value() }.semantics { contentDescription = desc },
        textStyle = Aapps.type.body.copy(color = c.fg),
        singleLine = true,
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit.value(); focus.clearFocus() }),
    )
}

@Composable
private fun AddRow(onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val add = { if (name.isNotBlank()) { onAdd(name.trim()); name = "" } }
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        InputField(
            name, { name = Group.cleanName(it) }, stringResource(Res.string.add_person), Modifier.weight(1f),
            keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            onDone = add,
        )
        Spacer(Modifier.width(6.dp))
        AappsIconButton(DividrIcons.Plus, stringResource(Res.string.add_person), add, enabled = name.isNotBlank())
    }
}

// Wie SettingsScreen, aber mit eigenem Titel
@Composable
private fun ListScreen(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AappsScaffold {
        Column(Modifier.fillMaxWidth()) {
            AappsTopBar(title, onBack, stringResource(CoreRes.string.aapps_back))
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content,
            )
        }
    }
}
