package solutions.xapient.aapps.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
    )
}
