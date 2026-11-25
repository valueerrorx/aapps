package solutions.xapient.dividr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTextButton
import solutions.xapient.aapps.core.ui.AappsTopBar
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.dividr.R
import solutions.xapient.dividr.domain.Entry
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.ImportSummary
import solutions.xapient.dividr.domain.Kind
import solutions.xapient.dividr.domain.Money
import solutions.xapient.dividr.domain.activeMembers
import solutions.xapient.dividr.domain.displayName
import solutions.xapient.dividr.domain.inGroup
import java.util.Locale

// Import-Dialog: Ergebnis des probeweisen Merges in Klartext
@Composable
fun ImportScreen(
    summary: ImportSummary,
    onApply: (fromStart: Map<Long, Boolean>, same: Set<Long>) -> Unit,
    onCancel: () -> Unit,
) {
    val c = Aapps.colors
    val locale = LocalConfiguration.current.locales[0]
    val g = summary.merged
    val fromStart = remember { mutableStateMapOf<Long, Boolean>() }
    val same = remember { mutableStateMapOf<Long, Boolean>() }
    AappsScaffold {
        Column(Modifier.fillMaxSize()) {
            AappsTopBar(stringResource(R.string.import_title), onCancel, stringResource(R.string.cancel))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                BasicText(g.name, style = Aapps.type.headline.copy(color = c.fg), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                val counts = listOfNotNull(
                    summary.added.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.n_new, it, it) },
                    summary.changed.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.n_changed, it, it) },
                    summary.deleted.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.n_deleted, it, it) },
                )
                BasicText(counts.joinToString(" · ").ifEmpty { stringResource(R.string.only_people) }, style = Aapps.type.body.copy(color = c.fgMuted))
                Spacer(Modifier.height(20.dp))

                summary.newPeople.forEach { p ->
                    val id = p.member.id
                    Card(pad = false) {
                        BasicText(
                            stringResource(R.string.new_person, p.member.name),
                            Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp),
                            style = Aapps.type.title.copy(color = c.fg),
                        )
                        val sameOn = same[id] ?: false
                        if (p.sameAs != null) {
                            SettingsToggle(stringResource(R.string.same_person, summary.local.displayName(p.sameAs.id)), sameOn, { same[id] = it })
                        }
                        if (!sameOn && p.forAllFromStart > 0) {
                            val start = fromStart[id] ?: p.member.fromStart
                            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 12.dp)) {
                                Segment(
                                    listOf(true to stringResource(R.string.from_start), false to stringResource(R.string.from_now)),
                                    start,
                                ) { fromStart[id] = it }
                                Spacer(Modifier.height(8.dp))
                                val n = if (start) p.forAllFromStart else p.forAllFromNow
                                BasicText(
                                    pluralStringResource(R.plurals.counts_in, n, p.member.name, n),
                                    Modifier.padding(horizontal = 4.dp),
                                    style = Aapps.type.body.copy(color = c.fgMuted),
                                )
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (summary.added.isNotEmpty()) {
                    SectionLabel(pluralStringResource(R.plurals.n_new, summary.added.size, summary.added.size), Modifier.padding(top = 8.dp))
                    Card(pad = false) { summary.added.forEach { EntryLine(g, it, locale) } }
                    Spacer(Modifier.height(16.dp))
                }
                if (summary.changed.isNotEmpty()) {
                    SectionLabel(pluralStringResource(R.plurals.n_changed, summary.changed.size, summary.changed.size), color = Warn)
                    Card(pad = false) {
                        summary.changed.forEach { ch ->
                            val before = Money.format(summary.local.inGroup(ch.before), g.currency, locale)
                            EntryLine(g, ch.after, locale, extra = if (ch.before.amount != ch.after.amount || ch.before.currency != ch.after.currency) before else null)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (summary.deleted.isNotEmpty()) {
                    SectionLabel(pluralStringResource(R.plurals.n_deleted, summary.deleted.size, summary.deleted.size), color = Warn)
                    Card(pad = false) { summary.deleted.forEach { EntryLine(summary.local, it, locale, struck = true) } }
                    Spacer(Modifier.height(16.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                AappsTextButton(stringResource(R.string.cancel), onCancel)
                Spacer(Modifier.width(12.dp))
                AappsPillButton(stringResource(R.string.apply), {
                    onApply(fromStart.toMap(), same.filterValues { it }.keys.toSet())
                }, Modifier.widthIn(min = 160.dp), active = true)
            }
        }
    }
}

@Composable
private fun Card(pad: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val c = Aapps.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(22.dp))
            .border(1.dp, c.line.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .padding(if (pad) 18.dp else 0.dp, if (pad) 16.dp else 6.dp),
        content = content,
    )
}

@Composable
private fun EntryLine(g: Group, e: Entry, locale: Locale, extra: String? = null, struck: Boolean = false) {
    val c = Aapps.colors
    val title = when {
        e.kind == Kind.PAYMENT -> stringResource(R.string.payment_line, g.displayName(e.payer), g.displayName(e.forIds.firstOrNull() ?: 0))
        e.title.isEmpty() -> stringResource(R.string.expense)
        else -> e.title
    }
    val deco = if (struck) TextDecoration.LineThrough else null
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Aapps.type.body.copy(color = c.fg, textDecoration = deco), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (e.kind == Kind.EXPENSE) {
                BasicText(
                    stringResource(R.string.paid_by_short, g.displayName(e.payer)),
                    style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f),
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            BasicText(Money.format(g.inGroup(e), g.currency, locale), style = Aapps.type.number.copy(color = if (struck) c.fgMuted else c.fg, textDecoration = deco))
            if (extra != null) BasicText(extra, style = Aapps.type.number.copy(color = c.fgFaint, fontSize = Aapps.type.label.fontSize * 1.05f, textDecoration = TextDecoration.LineThrough))
        }
    }
}

// Beitritt: fremde Gruppe, „Wer bist du?"
sealed interface JoinChoice {
    data class Existing(val id: Long) : JoinChoice
    data class New(val name: String, val fromStart: Boolean) : JoinChoice
    data object Watch : JoinChoice
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JoinScreen(
    incoming: Group,
    mergeWith: Group?, // offene eigene Gruppe gleicher Waehrung
    onJoin: (JoinChoice) -> Unit,
    onMerge: () -> Unit,
    onCancel: () -> Unit,
    initialName: String = "",
) {
    val c = Aapps.colors
    val people = remember(incoming) { incoming.activeMembers() }
    var choice by remember { mutableStateOf<Long?>(null) } // null = neu, -1 = nur zusehen
    var name by remember { mutableStateOf(initialName) }
    var fromStart by remember { mutableStateOf(true) }
    val entries = incoming.entries.values.count { !it.deleted }
    val forAll = incoming.entries.values.count { !it.deleted && it.kind == Kind.EXPENSE && it.forAll }
    val ready = choice != null || name.isNotBlank()
    AappsScaffold {
        Column(Modifier.fillMaxSize().imePadding()) {
            AappsTopBar(stringResource(R.string.join_title), onCancel, stringResource(R.string.cancel))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                BasicText(incoming.name, style = Aapps.type.headline.copy(color = c.fg), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                BasicText(
                    pluralStringResource(R.plurals.n_people, people.size, people.size) + " · " + pluralStringResource(R.plurals.n_expenses, entries, entries),
                    style = Aapps.type.body.copy(color = c.fgMuted),
                )
                Spacer(Modifier.height(24.dp))
                SectionLabel(stringResource(R.string.who_are_you))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    people.forEach { p -> Chip(incoming.displayName(p.id), choice == p.id, { choice = p.id }, role = Role.RadioButton) }
                    Chip(stringResource(R.string.just_watching), choice == -1L, { choice = -1L }, role = Role.RadioButton, color = c.fgMuted)
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.or_new))
                InputField(
                    name, { name = Group.cleanName(it); if (it.isNotBlank()) choice = null }, stringResource(R.string.your_name), Modifier.fillMaxWidth(),
                    keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                )
                if (choice == null && name.isNotBlank() && forAll > 0) {
                    Spacer(Modifier.height(12.dp))
                    Segment(listOf(true to stringResource(R.string.from_start), false to stringResource(R.string.from_now)), fromStart) { fromStart = it }
                    Spacer(Modifier.height(8.dp))
                    val n = if (fromStart) forAll else 0
                    BasicText(pluralStringResource(R.plurals.counts_in, n, name.trim(), n), style = Aapps.type.body.copy(color = c.fgMuted))
                }
                if (mergeWith != null) {
                    Spacer(Modifier.height(28.dp))
                    SectionLabel(stringResource(R.string.or_merge))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(c.surface, RoundedCornerShape(22.dp))
                            .border(1.dp, c.line.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AappsIcon(DividrIcons.Merge, null, c.fgMuted, size = 22.dp)
                        Spacer(Modifier.width(12.dp))
                        BasicText(
                            stringResource(R.string.merge_with, mergeWith.name),
                            Modifier.weight(1f),
                            style = Aapps.type.body.copy(color = c.fg),
                        )
                        SmallAction(stringResource(R.string.merge), onMerge)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                AappsPillButton(stringResource(R.string.join), {
                    val ch = choice
                    onJoin(
                        when {
                            ch == -1L -> JoinChoice.Watch
                            ch != null -> JoinChoice.Existing(ch)
                            else -> JoinChoice.New(name.trim(), fromStart)
                        },
                    )
                }, Modifier.widthIn(min = 200.dp), active = ready, enabled = ready)
            }
        }
    }
}
