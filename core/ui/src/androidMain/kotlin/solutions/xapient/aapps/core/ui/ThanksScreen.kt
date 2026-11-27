package solutions.xapient.aapps.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.billing.BundleKey
import solutions.xapient.aapps.core.billing.GateState
import solutions.xapient.aapps.core.billing.Notice
import solutions.xapient.aapps.core.billing.R as BR

// Dankes-Bildschirm nach der Testphase: Versprechen, Kauf, Wiederherstellen (Texte fuer Google Play)
@Composable
fun ThanksScreen(
    appName: String,
    state: GateState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
) {
    val price = state.price
    val canBuy = price != null && !state.pending
    val hint = when {
        state.pending -> BR.string.billing_hint_pending
        state.notice == Notice.OFFLINE -> BR.string.billing_hint_offline
        state.notice == Notice.NOT_FOUND -> BR.string.billing_hint_notfound
        state.notice == Notice.UNAVAILABLE -> BR.string.billing_hint_unavailable
        price == null -> BR.string.billing_hint_noprice
        else -> null
    }
    ThanksLayout(
        appName = appName,
        title = stringResource(BR.string.billing_title),
        text = stringResource(BR.string.billing_text),
        points = listOf(
            ThanksPoint(AappsIcons.Shield, stringResource(BR.string.billing_point_data)),
            ThanksPoint(AappsIcons.Offline, stringResource(BR.string.billing_point_offline)),
            ThanksPoint(AappsIcons.Code, stringResource(BR.string.billing_point_source)),
        ),
        primary = {
            AappsPillButton(
                if (price != null) stringResource(BR.string.billing_unlock_price, price) else stringResource(BR.string.billing_unlock),
                onBuy,
                icon = AappsIcons.Unlock,
                active = canBuy,
                enabled = canBuy,
            )
        },
        hint = hint?.let { stringResource(it) },
        secondary = {
            AappsTextButton(
                stringResource(if (state.busy) BR.string.billing_restoring else BR.string.billing_restore),
                { if (!state.busy) onRestore() },
            )
        },
        footer = stringResource(BR.string.billing_once),
        extra = { BundleLink() },
    )
}

// Dezenter Verweis auf die Schluessel-App im Play Store (Anzahl und Preis stehen im Text)
@Composable
private fun BundleLink() {
    val context = LocalContext.current
    val c = Aapps.colors
    val small = Aapps.type.body.copy(fontSize = Aapps.type.label.fontSize * 1.1f, textAlign = TextAlign.Center)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Column(
        Modifier
            .pressScale(pressed)
            .clickable(source, indication = null, role = Role.Button) { BundleKey.openStore(context) }
            .widthIn(max = 480.dp)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(stringResource(BR.string.billing_bundle), style = small.copy(color = c.fgMuted))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(stringResource(BR.string.billing_bundle_name), style = small.copy(color = c.accent))
            AappsIcon(AappsIcons.Chevron, null, c.accent, size = 16.dp)
        }
    }
}
