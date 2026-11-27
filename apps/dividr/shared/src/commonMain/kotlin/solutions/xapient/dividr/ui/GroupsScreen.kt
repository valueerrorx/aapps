package solutions.xapient.dividr.ui

import solutions.xapient.dividr.APP_NAME
import solutions.xapient.dividr.currentLocale
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_back
import solutions.xapient.dividr.res.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTopBar
import solutions.xapient.aapps.core.ui.glow
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.Money
import solutions.xapient.dividr.domain.activeMembers
import solutions.xapient.dividr.domain.balances
import solutions.xapient.dividr.domain.resolve
import solutions.xapient.dividr.domain.Locale

data class GroupItem(val key: String, val group: Group, val me: Long?)

// Gruppenliste; leer = Start mit „Neue Gruppe" und „Code scannen"
@Composable
fun GroupsScreen(items: List<GroupItem>, onOpen: (String) -> Unit, onNew: () -> Unit, onScan: () -> Unit, onBack: (() -> Unit)?) {
    val c = Aapps.colors
    val locale = currentLocale()
    AappsScaffold {
        Column(Modifier.fillMaxSize()) {
            if (items.isEmpty()) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(40.dp))
                    Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(104.dp).glow(c.accent, 120.dp, 0.14f))
                    Spacer(Modifier.height(24.dp))
                    BasicText(APP_NAME, style = Aapps.type.headline.copy(color = c.fg))
                    Spacer(Modifier.height(8.dp))
                    BasicText(stringResource(Res.string.start_hint), style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center))
                    Spacer(Modifier.height(40.dp))
                }
            } else {
                AappsTopBar(stringResource(Res.string.groups), onBack, stringResource(CoreRes.string.aapps_back))
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { item -> GroupRow(item, locale) { onOpen(item.key) } }
                }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AappsPillButton(stringResource(Res.string.new_group), onNew, Modifier.widthIn(min = 220.dp), icon = DividrIcons.Plus, active = items.isEmpty())
                Spacer(Modifier.height(10.dp))
                AappsPillButton(stringResource(Res.string.scan_code), onScan, Modifier.widthIn(min = 220.dp), icon = DividrIcons.Camera)
            }
        }
    }
}

@Composable
private fun GroupRow(item: GroupItem, locale: Locale, onClick: () -> Unit) {
    val c = Aapps.colors
    val g = item.group
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val mine = item.me?.let { remember(g) { g.balances()[g.resolve(it)] } }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(pressed, 0.98f)
            .background(if (pressed) c.surfaceHigh else c.surface, RoundedCornerShape(22.dp))
            .border(1.dp, c.line.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(g.name, style = Aapps.type.title.copy(color = c.fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val n = g.activeMembers().size
            BasicText(pluralStringResource(Res.plurals.n_people, n, n), style = Aapps.type.body.copy(color = c.fgMuted))
        }
        if (mine != null) {
            Spacer(Modifier.width(10.dp))
            BasicText(
                Money.format(mine, g.currency, locale, signed = true),
                style = Aapps.type.number.copy(color = if (mine >= 0) c.accent else c.fg),
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(6.dp))
        AappsIcon(AappsIcons.Chevron, null, c.fgMuted, size = 18.dp)
    }
}

// Neue Gruppe: ein Formular
@Composable
fun NewGroupScreen(
    defaultCurrency: String,
    onCreate: (name: String, me: String, currency: String) -> Unit,
    onBack: () -> Unit,
    initialName: String = "",
    initialMe: String = "",
) {
    val c = Aapps.colors
    var name by remember { mutableStateOf(initialName) }
    var me by remember { mutableStateOf(initialMe) }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var other by remember { mutableStateOf("") }
    val options = remember(defaultCurrency) { (listOf(defaultCurrency) + Money.COMMON).distinct() }
    val ready = name.isNotBlank() && me.isNotBlank() && Money.valid(currency)
    AappsScaffold {
        Column(Modifier.fillMaxSize().imePadding()) {
            AappsTopBar(stringResource(Res.string.new_group), onBack, stringResource(CoreRes.string.aapps_back))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                SectionLabel(stringResource(Res.string.group_name))
                InputField(name, { name = Group.cleanTitle(it) }, stringResource(Res.string.group_name_hint), Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                SectionLabel(stringResource(Res.string.your_name))
                InputField(
                    me, { me = Group.cleanName(it) }, stringResource(Res.string.your_name_hint), Modifier.fillMaxWidth(),
                    keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                )
                Spacer(Modifier.height(20.dp))
                SectionLabel(stringResource(Res.string.currency))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { code -> Chip(code, currency == code && other.isEmpty(), { currency = code; other = "" }, role = Role.RadioButton) }
                }
                Spacer(Modifier.height(8.dp))
                InputField(
                    other,
                    { v -> other = v.uppercase().filter { it in 'A'..'Z' }.take(3); if (Money.valid(other)) currency = other },
                    stringResource(Res.string.other_currency),
                    Modifier.width(180.dp),
                    keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                )
                Spacer(Modifier.height(10.dp))
                BasicText(stringResource(Res.string.currency_note), style = Aapps.type.body.copy(color = c.fgFaint, fontSize = Aapps.type.label.fontSize * 1.1f))
                Spacer(Modifier.height(24.dp))
            }
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                AappsPillButton(stringResource(Res.string.create), { onCreate(name.trim(), me.trim(), currency) }, Modifier.widthIn(min = 200.dp), active = ready, enabled = ready)
            }
        }
    }
}
