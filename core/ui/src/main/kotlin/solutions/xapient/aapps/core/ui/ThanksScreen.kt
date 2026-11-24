package solutions.xapient.aapps.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.billing.GateState
import solutions.xapient.aapps.core.billing.Notice
import solutions.xapient.aapps.core.billing.R as BR

// Dankes-Bildschirm nach der Testphase: Versprechen, Kauf, Wiederherstellen
@Composable
fun ThanksScreen(
    appName: String,
    state: GateState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
) {
    val c = Aapps.colors
    AappsScaffold {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hero: Schloss im Akzent-Halo
            Box(
                Modifier.size(112.dp).glow(c.accent, 72.dp, 0.3f).background(c.surface, CircleShape)
                    .border(1.5.dp, c.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AappsIcon(AappsIcons.Unlock, null, c.accent, size = 48.dp)
            }
            Spacer(Modifier.height(24.dp))
            BasicText(appName.uppercase(), style = Aapps.type.label.copy(color = c.accent))
            Spacer(Modifier.height(6.dp))
            BasicText(
                stringResource(BR.string.billing_title),
                Modifier.semantics { heading() },
                style = Aapps.type.headline.copy(color = c.fg, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(14.dp))
            BasicText(
                stringResource(BR.string.billing_text),
                Modifier.widthIn(max = 480.dp),
                style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(24.dp))
            Box(Modifier.widthIn(max = 480.dp)) {
                SettingsGroup(null) {
                    Point(AappsIcons.Shield, stringResource(BR.string.billing_point_data))
                    Point(AappsIcons.Offline, stringResource(BR.string.billing_point_offline))
                    Point(AappsIcons.Code, stringResource(BR.string.billing_point_source))
                }
            }
            Spacer(Modifier.height(28.dp))
            val price = state.price
            val canBuy = price != null && !state.pending
            AappsPillButton(
                if (price != null) stringResource(BR.string.billing_unlock_price, price) else stringResource(BR.string.billing_unlock),
                onBuy,
                icon = AappsIcons.Unlock,
                active = canBuy,
                enabled = canBuy,
            )
            val hint = when {
                state.pending -> BR.string.billing_hint_pending
                state.notice == Notice.OFFLINE -> BR.string.billing_hint_offline
                state.notice == Notice.NOT_FOUND -> BR.string.billing_hint_notfound
                state.notice == Notice.UNAVAILABLE -> BR.string.billing_hint_unavailable
                price == null -> BR.string.billing_hint_noprice
                else -> null
            }
            if (hint != null) {
                Spacer(Modifier.height(14.dp))
                BasicText(
                    stringResource(hint),
                    Modifier.widthIn(max = 480.dp),
                    style = Aapps.type.body.copy(color = c.fg, textAlign = TextAlign.Center),
                )
            }
            Spacer(Modifier.height(8.dp))
            AappsTextButton(
                stringResource(if (state.busy) BR.string.billing_restoring else BR.string.billing_restore),
                { if (!state.busy) onRestore() },
            )
            Spacer(Modifier.height(12.dp))
            BasicText(
                stringResource(BR.string.billing_once),
                Modifier.widthIn(max = 480.dp),
                style = Aapps.type.body.copy(
                    color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f, textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun Point(icon: ImageVector, text: String) {
    val c = Aapps.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).background(c.accent.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            AappsIcon(icon, null, c.accent, size = 18.dp)
        }
        Spacer(Modifier.width(14.dp))
        BasicText(text, Modifier.weight(1f), style = Aapps.type.body.copy(color = c.fg))
    }
}
