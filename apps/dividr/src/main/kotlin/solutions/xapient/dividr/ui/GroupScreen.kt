package solutions.xapient.dividr.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
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
import solutions.xapient.aapps.core.ui.glow
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.dividr.R
import solutions.xapient.dividr.domain.Entry
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.Kind
import solutions.xapient.dividr.domain.Money
import solutions.xapient.dividr.domain.balances
import solutions.xapient.dividr.domain.displayName
import solutions.xapient.dividr.domain.inGroup
import solutions.xapient.dividr.domain.isFallback
import solutions.xapient.dividr.domain.resolve
import solutions.xapient.dividr.domain.totalSpent
import solutions.xapient.dividr.domain.visibleEntries
import solutions.xapient.dividr.qr.QrImage
import solutions.xapient.dividr.qr.QrMatrix
import java.util.Locale

// Hauptscreen: Saldo, Ausgabenliste, eigener QR und Scannen
@Composable
fun GroupScreen(
    group: Group,
    me: Long?,
    codes: List<QrMatrix>, // leer = zu gross fuer QR
    today: Int,
    onGroups: () -> Unit,
    onSettings: () -> Unit,
    onSettle: () -> Unit,
    onAdd: () -> Unit,
    onEntry: (Long) -> Unit,
    onQr: () -> Unit,
    onScan: () -> Unit,
    notice: String? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val entries = remember(group) { group.visibleEntries() }
    AappsScaffold {
        Column(Modifier.fillMaxSize()) {
            Header(group, onGroups, onSettings)
            if (landscape) {
                Row(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Balance(group, me, locale, onSettle)
                        EntryList(group, entries, today, locale, onEntry, Modifier.weight(1f))
                    }
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(0.8f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                        Exchange(codes, onQr, onScan, Modifier.weight(1f, fill = false))
                        Spacer(Modifier.height(12.dp))
                        AappsPillButton(stringResource(R.string.add_expense), onAdd, Modifier.fillMaxWidth(), icon = DividrIcons.Plus, active = true)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    Balance(group, me, locale, onSettle)
                    EntryList(group, entries, today, locale, onEntry, Modifier.weight(1f))
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AappsPillButton(stringResource(R.string.add_expense), onAdd, icon = DividrIcons.Plus, active = true)
                    }
                    Spacer(Modifier.height(12.dp))
                    Exchange(codes, onQr, onScan)
                }
            }
        }
        AnimatedVisibility(notice != null, Modifier.align(Alignment.TopCenter).padding(top = 64.dp), enter = fadeIn(), exit = fadeOut()) {
            BasicText(
                notice ?: "",
                Modifier
                    .background(Aapps.colors.surfaceHigh, CircleShape)
                    .border(1.dp, Aapps.colors.accent.copy(alpha = 0.6f), CircleShape)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                style = Aapps.type.body.copy(color = Aapps.colors.fg),
            )
        }
    }
}

@Composable
private fun Header(group: Group, onGroups: () -> Unit, onSettings: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .pressScale(pressed, 0.97f)
                .clip(CircleShape)
                .clickable(source, indication = null, role = Role.Button, onClick = onGroups)
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                group.name.ifEmpty { stringResource(R.string.app_name) },
                Modifier.weight(1f, fill = false).semantics { heading() },
                style = Aapps.type.title.copy(color = c.fg),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(4.dp))
            AappsIcon(DividrIcons.Down, stringResource(R.string.groups), c.fgMuted, size = 18.dp)
        }
        AappsIconButton(AappsIcons.Gear, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_settings), onSettings)
    }
}

// Eigener Saldo gross (Akzent bei Guthaben), Tipp oeffnet die Abrechnung
@Composable
private fun Balance(group: Group, me: Long?, locale: Locale, onSettle: () -> Unit) {
    val c = Aapps.colors
    val bal = remember(group) { group.balances() }
    val mine = me?.let { bal[group.resolve(it)] }
    val total = remember(group) { group.totalSpent() }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Column(
        Modifier
            .fillMaxWidth()
            .pressScale(pressed, 0.98f)
            .clickable(source, indication = null, role = Role.Button, onClickLabel = stringResource(R.string.settle), onClick = onSettle)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        val label = when {
            mine == null -> stringResource(R.string.total_spent)
            mine > 0 -> stringResource(R.string.you_get)
            mine < 0 -> stringResource(R.string.you_owe)
            else -> stringResource(R.string.you_are_even)
        }
        SectionLabel(label, Modifier.padding(start = 2.dp))
        val value = mine ?: total
        val color = if (mine != null && mine < 0) c.fg else c.accent
        BasicText(
            Money.format(kotlin.math.abs(value), group.currency, locale),
            Modifier.fillMaxWidth().glow(c.accent, 120.dp, if (color == c.accent) 0.10f else 0f),
            style = Aapps.type.display.copy(color = color, fontSize = 56.sp),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 22.sp, maxFontSize = 56.sp, stepSize = 2.sp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                if (mine != null) stringResource(R.string.total_line, Money.format(total, group.currency, locale)) else stringResource(R.string.settle_hint),
                Modifier.weight(1f, fill = false),
                style = Aapps.type.body.copy(color = c.fgMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(6.dp))
            AappsIcon(AappsIcons.Chevron, null, c.fgMuted, size = 16.dp)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun EntryList(group: Group, entries: List<Entry>, today: Int, locale: Locale, onEntry: (Long) -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    if (entries.isEmpty()) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(stringResource(R.string.empty_title), style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center))
            Spacer(Modifier.height(6.dp))
            BasicText(
                stringResource(R.string.empty_hint),
                Modifier.padding(horizontal = 24.dp),
                style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center),
            )
        }
        return
    }
    val byDay = remember(entries) { entries.groupBy { it.day } }
    LazyColumn(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.surface)
            .border(1.dp, c.line.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
    ) {
        byDay.forEach { (day, list) ->
            item(key = "d$day") {
                BasicText(
                    dayLabel(day, today, locale).uppercase(),
                    Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 2.dp),
                    style = Aapps.type.label.copy(color = c.fgFaint),
                )
            }
            items(list, key = { it.id }) { e -> EntryRow(group, e, locale) { onEntry(e.id) } }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun dayLabel(day: Int, today: Int, locale: Locale): String = when (day) {
    today -> stringResource(R.string.today)
    today - 1 -> stringResource(R.string.yesterday)
    else -> {
        val d = java.time.LocalDate.ofEpochDay(day.toLong())
        val pattern = if (d.year == java.time.LocalDate.ofEpochDay(today.toLong()).year) "EEE d MMM" else "d MMM yyyy"
        d.format(java.time.format.DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, pattern), locale))
    }
}

@Composable
private fun EntryRow(group: Group, e: Entry, locale: Locale, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val payer = group.displayName(e.payer)
    val payment = e.kind == Kind.PAYMENT
    val title = when {
        payment -> stringResource(R.string.payment)
        e.title.isEmpty() -> stringResource(R.string.expense)
        else -> e.title
    }
    val sub = when {
        payment -> stringResource(R.string.paid_to, payer, group.displayName(e.forIds.firstOrNull() ?: 0))
        e.forAll || group.isFallback(e) -> stringResource(R.string.paid_for_all, payer)
        else -> stringResource(R.string.paid_for, payer, e.forIds.map { group.displayName(it) }.distinct().joinToString(", "))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (pressed) c.surfaceHigh else Color.Transparent)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(8.dp).background(
                when {
                    group.isFallback(e) -> Warn
                    payment -> c.fgFaint
                    else -> c.accent.copy(alpha = 0.7f)
                },
                CircleShape,
            ),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Aapps.type.body.copy(color = if (payment) c.fgMuted else c.fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(sub, style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            BasicText(
                Money.format(group.inGroup(e), group.currency, locale),
                style = Aapps.type.number.copy(color = if (payment) c.fgMuted else c.fg, fontSize = Aapps.type.body.fontSize * 1.05f),
                maxLines = 1,
            )
            if (e.currency != group.currency) {
                BasicText(
                    Money.withCode(e.amount, e.currency, locale),
                    style = Aapps.type.number.copy(color = c.fgFaint, fontSize = Aapps.type.label.fontSize * 1.05f),
                    maxLines = 1,
                )
            }
        }
    }
}

// Unten: eigener Code (weisse Karte) links, Scannen rechts
@Composable
private fun Exchange(codes: List<QrMatrix>, onQr: () -> Unit, onScan: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(max = 220.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QrCard(codes, onQr, Modifier.weight(0.45f))
        ScanTile(onScan, Modifier.weight(0.55f).aspectRatio(1.22f))
    }
}

@Composable
private fun QrCard(codes: List<QrMatrix>, onQr: () -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier
            .aspectRatio(1f)
            .pressScale(pressed, 0.96f)
            .clip(RoundedCornerShape(24.dp))
            .background(if (codes.size == 1) Color.White else c.surface)
            .border(1.5.dp, if (codes.size == 1) Color.White.copy(alpha = 0.5f) else c.line, RoundedCornerShape(24.dp))
            .clickable(source, indication = null, role = Role.Button, onClickLabel = stringResource(R.string.show_code), onClick = onQr)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (codes.size) {
            1 -> QrImage(codes[0], stringResource(R.string.my_code), Modifier.fillMaxSize(), corner = 10.dp)
            else -> {
                val text = if (codes.isEmpty()) stringResource(R.string.too_big_short)
                else pluralStringResource(R.plurals.parts_tap, codes.size, codes.size)
                ColumnCenter {
                    AappsIcon(DividrIcons.Scan, null, c.accent, size = 30.dp)
                    Spacer(Modifier.height(8.dp))
                    BasicText(text, style = Aapps.type.body.copy(color = c.fg, textAlign = TextAlign.Center))
                }
            }
        }
    }
}

@Composable
private fun ColumnCenter(content: @Composable ColumnScope.() -> Unit) =
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = content)

@Composable
private fun ScanTile(onScan: () -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Column(
        modifier
            .fillMaxHeight()
            .pressScale(pressed, 0.96f)
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface)
            .border(1.dp, c.accent.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .glow(c.accent, 90.dp, 0.10f)
            .clickable(source, indication = null, role = Role.Button, onClick = onScan)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp).background(c.accent.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            AappsIcon(DividrIcons.Camera, null, c.accent, size = 30.dp)
        }
        Spacer(Modifier.height(10.dp))
        BasicText(stringResource(R.string.scan).uppercase(), style = Aapps.type.label.copy(color = c.accent, fontSize = Aapps.type.label.fontSize * 1.15f))
        Spacer(Modifier.height(2.dp))
        BasicText(
            stringResource(R.string.scan_sub),
            style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.05f, textAlign = TextAlign.Center),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
