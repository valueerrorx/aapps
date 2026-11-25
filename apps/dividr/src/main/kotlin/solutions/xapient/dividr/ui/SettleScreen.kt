package solutions.xapient.dividr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTopBar
import solutions.xapient.aapps.core.ui.glow
import solutions.xapient.dividr.R
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.Money
import solutions.xapient.dividr.domain.Settle
import solutions.xapient.dividr.domain.Transfer
import solutions.xapient.dividr.domain.balances
import solutions.xapient.dividr.domain.displayName
import solutions.xapient.dividr.domain.resolve
import solutions.xapient.dividr.domain.totalSpent
import java.util.Locale

// Abrechnung: Saldo-Balken je Person, minimale Ueberweisungen mit „Erledigt"
@Composable
fun SettleScreen(group: Group, me: Long?, onDone: (Transfer) -> Unit, onShare: () -> Unit, onBack: () -> Unit) {
    val c = Aapps.colors
    val locale = LocalConfiguration.current.locales[0]
    val bal = remember(group) { group.balances() }
    val plan = remember(bal) { Settle.plan(bal) }
    val mine = me?.let { group.resolve(it) }
    val ordered = remember(plan, mine) { plan.sortedBy { if (it.from == mine || it.to == mine) 0 else 1 } }
    val max = (bal.values.maxOfOrNull { kotlin.math.abs(it) } ?: 0L).coerceAtLeast(1)
    AappsScaffold {
        Column(Modifier.fillMaxSize()) {
            AappsTopBar(stringResource(R.string.settle), onBack, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_back)) {
                AappsIconButton(DividrIcons.Share, stringResource(R.string.share), onShare)
            }
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
                SectionLabel(stringResource(R.string.total_spent))
                BasicText(Money.format(group.totalSpent(), group.currency, locale), style = Aapps.type.headline.copy(color = c.fg))
                Spacer(Modifier.height(24.dp))

                SectionLabel(stringResource(R.string.balances))
                Column(
                    Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(22.dp))
                        .border(1.dp, c.line.copy(alpha = 0.6f), RoundedCornerShape(22.dp)).padding(vertical = 8.dp),
                ) {
                    bal.forEach { (id, v) -> BarRow(group.displayName(id), v, max, group.currency, locale, id == mine) }
                }
                Spacer(Modifier.height(24.dp))

                SectionLabel(stringResource(R.string.transfers))
                if (ordered.isEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().glow(c.accent, 140.dp, 0.12f).background(c.surface, RoundedCornerShape(22.dp))
                            .border(1.dp, c.accent.copy(alpha = 0.5f), RoundedCornerShape(22.dp)).padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(36.dp).background(c.accent.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                            AappsIcon(AappsIcons.Check, null, c.accent, size = 22.dp)
                        }
                        Spacer(Modifier.width(14.dp))
                        BasicText(stringResource(R.string.all_square), style = Aapps.type.title.copy(color = c.fg))
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(22.dp))
                            .border(1.dp, c.line.copy(alpha = 0.6f), RoundedCornerShape(22.dp)).padding(vertical = 6.dp),
                    ) {
                        ordered.forEach { t ->
                            val text = when (mine) {
                                t.from -> stringResource(R.string.you_pay, group.displayName(t.to))
                                t.to -> stringResource(R.string.pays_you, group.displayName(t.from))
                                else -> stringResource(R.string.pays, group.displayName(t.from), group.displayName(t.to))
                            }
                            val own = mine == t.from || mine == t.to
                            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 18.dp, end = 10.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    BasicText(text, style = Aapps.type.body.copy(color = if (own) c.fg else c.fgMuted), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    BasicText(Money.format(t.amount, group.currency, locale), style = Aapps.type.number.copy(color = if (own) c.accent else c.fg))
                                }
                                Spacer(Modifier.width(8.dp))
                                SmallAction(stringResource(R.string.done), { onDone(t) }, icon = AappsIcons.Check)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    BasicText(stringResource(R.string.settle_note), Modifier.padding(horizontal = 4.dp), style = Aapps.type.body.copy(color = c.fgFaint, fontSize = Aapps.type.label.fontSize * 1.1f))
                }
            }
        }
    }
}

// Balken von der Mitte: Guthaben nach rechts (Akzent), Schuld nach links
@Composable
private fun BarRow(name: String, value: Long, max: Long, currency: String, locale: Locale, me: Boolean) {
    val c = Aapps.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(name, Modifier.weight(1f), style = Aapps.type.body.copy(color = if (me) c.fg else c.fgMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            BasicText(
                Money.format(value, currency, locale, signed = true),
                Modifier.widthIn(min = 64.dp),
                style = Aapps.type.number.copy(color = if (value > 0) c.accent else c.fg, fontSize = Aapps.type.body.fontSize),
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val mid = size.width / 2
            drawRoundRect(c.line, cornerRadius = CornerRadius(4.dp.toPx()))
            drawLine(c.fgFaint, Offset(mid, 0f), Offset(mid, size.height), 1.dp.toPx())
            val w = (kotlin.math.abs(value).toFloat() / max) * (mid - 2.dp.toPx())
            if (w > 0f) {
                val right = (value > 0) != rtl
                val x = if (right) mid else mid - w
                drawRoundRect(if (value > 0) c.accent else c.fgMuted, Offset(x, 0f), Size(w, size.height), CornerRadius(4.dp.toPx()))
            }
        }
    }
}
