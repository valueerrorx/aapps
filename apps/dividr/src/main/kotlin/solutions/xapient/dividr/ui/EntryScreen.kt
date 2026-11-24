package solutions.xapient.dividr.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTopBar
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.dividr.R
import solutions.xapient.dividr.domain.Entry
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.Kind
import solutions.xapient.dividr.domain.Money
import solutions.xapient.dividr.domain.Stamp
import solutions.xapient.dividr.domain.activeMembers
import solutions.xapient.dividr.domain.displayName
import solutions.xapient.dividr.domain.resolve
import java.util.Locale

// Ausgabe oder Zahlung anlegen/aendern; id 0 = neu
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(
    group: Group,
    initial: Entry?,
    me: Long?,
    today: Int,
    recentTitles: List<String>,
    lastRate: (String) -> Long?,
    onSave: (Entry) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    focus: Boolean = true,
    preset: Entry? = null, // nur Screenshots: Startwerte einer neuen Eingabe
) {
    val c = Aapps.colors
    val locale = LocalConfiguration.current.locales[0]
    val people = remember(group) { group.activeMembers() }
    val start = initial ?: preset
    var kind by remember { mutableStateOf(start?.kind ?: Kind.EXPENSE) }
    var currency by remember { mutableStateOf(start?.currency?.ifEmpty { null } ?: group.currency) }
    var amount by remember { mutableStateOf(start?.let { Money.plain(it.amount, currency, locale).replace(groupingOf(locale).toString(), "") } ?: "") }
    var rate by remember { mutableStateOf(start?.rateMicros?.takeIf { it > 0 }?.let { Money.formatRate(it, locale) } ?: "") }
    var title by remember { mutableStateOf(start?.title ?: "") }
    var payer by remember { mutableStateOf(start?.payer?.let { group.resolve(it) } ?: me?.let { group.resolve(it) } ?: people.firstOrNull()?.id ?: 0L) }
    var forAll by remember { mutableStateOf(start?.let { it.kind == Kind.EXPENSE && it.forAll } ?: true) }
    var forIds by remember { mutableStateOf(start?.takeIf { !it.forAll }?.forIds?.map { group.resolve(it) }?.toSet() ?: emptySet()) }
    var to by remember { mutableStateOf(start?.takeIf { it.kind == Kind.PAYMENT }?.forIds?.firstOrNull()?.let { group.resolve(it) }) }
    var day by remember { mutableIntStateOf(start?.day ?: today) }
    var showCurrencies by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) LaunchedEffect(Unit) { delay(3000); confirmDelete = false }

    val foreign = currency != group.currency
    val minor = Money.parse(amount, currency)?.takeIf { it > 0 }
    val micros = if (foreign) Money.parseRate(rate) else 0L
    val valid = minor != null && micros != null && payer != 0L && when (kind) {
        Kind.PAYMENT -> to != null && to != payer
        Kind.EXPENSE -> forAll || forIds.isNotEmpty()
    }

    fun save() {
        if (!valid) return
        onSave(
            Entry(
                id = initial?.id ?: 0L,
                stamp = initial?.stamp ?: Stamp(0, 0),
                kind = kind,
                title = if (kind == Kind.PAYMENT) "" else title,
                amount = minor!!,
                currency = currency,
                rateMicros = micros ?: 0,
                payer = payer,
                forAll = kind == Kind.EXPENSE && forAll,
                forIds = if (kind == Kind.PAYMENT) setOfNotNull(to) else if (forAll) emptySet() else forIds,
                day = day,
            ),
        )
    }

    AappsScaffold {
        Column(Modifier.fillMaxSize().imePadding()) {
            AappsTopBar(
                stringResource(if (initial == null) R.string.new_entry else R.string.edit_entry),
                onBack, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_back),
            ) {
                if (initial != null) {
                    AappsIconButton(DividrIcons.Trash, stringResource(R.string.delete), {
                        if (confirmDelete) onDelete() else confirmDelete = true
                    })
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            ) {
                AnimatedVisibility(confirmDelete, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    BasicText(stringResource(R.string.delete_confirm), Modifier.padding(bottom = 12.dp), style = Aapps.type.body.copy(color = Warn))
                }
                Segment(
                    listOf(Kind.EXPENSE to stringResource(R.string.expense), Kind.PAYMENT to stringResource(R.string.payment)),
                    kind,
                ) { kind = it }
                Spacer(Modifier.height(18.dp))
                AmountField(amount, { amount = it }, currency, focus && initial == null) { showCurrencies = !showCurrencies }
                AnimatedVisibility(showCurrencies, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    val options = remember(group.currency) { listOf(group.currency) + Money.COMMON.filter { it != group.currency } }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { code ->
                            Chip(code, code == currency, {
                                currency = code
                                showCurrencies = false
                                if (code != group.currency && Money.parseRate(rate) == null) {
                                    rate = lastRate(code)?.let { Money.formatRate(it, locale) } ?: ""
                                }
                            }, role = Role.RadioButton)
                        }
                    }
                }
                AnimatedVisibility(foreign, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    RateRow(currency, group.currency, rate, { rate = it }, minor, micros, locale)
                }
                Spacer(Modifier.height(20.dp))
                if (kind == Kind.EXPENSE) {
                    InputField(title, { title = Group.cleanTitle(it) }, stringResource(R.string.title_hint), Modifier.fillMaxWidth())
                    val suggestions = recentTitles.filter { it != title && (title.isEmpty() || it.startsWith(title, ignoreCase = true)) }.take(6)
                    if (suggestions.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggestions.forEach { s -> Chip(s, false, { title = s }) }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                SectionLabel(stringResource(R.string.paid_by))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    people.forEach { p -> Chip(group.displayName(p.id), payer == p.id, { payer = p.id; if (to == p.id) to = null }, role = Role.RadioButton) }
                }
                Spacer(Modifier.height(16.dp))
                if (kind == Kind.EXPENSE) {
                    SectionLabel(stringResource(R.string.paid_for_label))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(stringResource(R.string.everyone), forAll, { forAll = true; forIds = emptySet() })
                        people.forEach { p ->
                            Chip(group.displayName(p.id), !forAll && p.id in forIds, {
                                val next = if (forAll) setOf(p.id) else if (p.id in forIds) forIds - p.id else forIds + p.id
                                forAll = next.isEmpty()
                                forIds = next
                            })
                        }
                    }
                } else {
                    SectionLabel(stringResource(R.string.paid_to_label))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        people.filter { it.id != payer }.forEach { p -> Chip(group.displayName(p.id), to == p.id, { to = p.id }, role = Role.RadioButton) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.date))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AappsIconButton(DividrIcons.Left, stringResource(R.string.day_before), { day -= 1 })
                    BasicText(
                        dayLabel(day, today, locale),
                        Modifier.widthIn(min = 140.dp),
                        style = Aapps.type.body.copy(color = c.fg, textAlign = TextAlign.Center),
                    )
                    AappsIconButton(DividrIcons.Right, stringResource(R.string.day_after), { if (day < today) day += 1 }, enabled = day < today)
                }
                Spacer(Modifier.height(24.dp))
            }
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                AappsPillButton(stringResource(R.string.save), ::save, Modifier.widthIn(min = 200.dp), active = valid, enabled = valid)
            }
        }
    }
}

private fun groupingOf(locale: Locale) = java.text.DecimalFormatSymbols.getInstance(locale).groupingSeparator

@Composable
private fun AmountField(value: String, onChange: (String) -> Unit, currency: String, focus: Boolean, onCurrency: () -> Unit) {
    val c = Aapps.colors
    val requester = remember { FocusRequester() }
    if (focus) LaunchedEffect(Unit) { requester.requestFocus() }
    val hint = stringResource(R.string.amount)
    val style = Aapps.type.display.copy(color = c.accent, fontSize = 64.sp, textAlign = TextAlign.Center)
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = value,
            onValueChange = { v -> onChange(v.filter { it.isDigit() || it == ',' || it == '.' }.take(14)) },
            modifier = Modifier.weight(1f).focusRequester(requester).semantics { contentDescription = hint },
            textStyle = style,
            singleLine = true,
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            decorationBox = { inner ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 84.dp), contentAlignment = Alignment.Center) {
                        if (value.isEmpty()) BasicText("0", style = style.copy(color = c.fgFaint))
                        inner()
                    }
                    Box(Modifier.fillMaxWidth(0.7f).height(2.dp).background(c.accent.copy(alpha = 0.5f), CircleShape))
                }
            },
        )
        Spacer(Modifier.width(8.dp))
        Chip(currency, true, onCurrency, role = Role.Button)
    }
}

@Composable
private fun RateRow(from: String, to: String, rate: String, onRate: (String) -> Unit, minor: Long?, micros: Long?, locale: Locale) {
    val c = Aapps.colors
    Column(Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText("1 $from =", style = Aapps.type.body.copy(color = c.fgMuted))
            Spacer(Modifier.width(10.dp))
            InputField(
                rate, { v -> onRate(v.filter { it.isDigit() || it == ',' || it == '.' }.take(14)) }, stringResource(R.string.rate),
                Modifier.weight(1f),
                style = Aapps.type.number.copy(color = c.fg),
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            )
            Spacer(Modifier.width(10.dp))
            BasicText(to, style = Aapps.type.body.copy(color = c.fgMuted))
        }
        if (minor != null && micros != null && micros > 0) {
            BasicText(
                "= " + Money.format(Money.convert(minor, from, micros, to), to, locale),
                Modifier.padding(top = 6.dp, start = 4.dp),
                style = Aapps.type.number.copy(color = c.accent, fontSize = Aapps.type.body.fontSize),
            )
        }
    }
}

// Segment-Leiste wie in den Einstellungen
@Composable
fun <T> Segment(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val c = Aapps.colors
    Row(Modifier.fillMaxWidth().background(c.surface, CircleShape).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            val sel = value == selected
            val bg by animateColorAsState(if (sel) c.accent else Color.Transparent, Springs.gentle(), label = "seg")
            val fg by animateColorAsState(if (sel) c.onAccent else c.fgMuted, Springs.gentle(), label = "segFg")
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .selectable(sel, indication = null, interactionSource = null, role = Role.RadioButton) { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(label, Modifier.padding(horizontal = 8.dp, vertical = 10.dp), style = Aapps.type.body.copy(color = fg, textAlign = TextAlign.Center), maxLines = 1)
            }
        }
    }
}
