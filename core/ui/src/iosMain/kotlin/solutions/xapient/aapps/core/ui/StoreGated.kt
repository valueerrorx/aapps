package solutions.xapient.aapps.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreBridge
import solutions.xapient.aapps.core.store.StoreGate
import solutions.xapient.aapps.core.store.StoreNotice
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.store.UserDefaultsStoreCache

/** iOS: Angebot der Testphase, 1 Tag frei, danach Dankes-Bildschirm bis zum Kauf; [debug] nur aus Debug-Builds */
@Composable
fun StoreGated(accent: Color, appName: String, bridge: StoreBridge, debug: String? = null, content: @Composable () -> Unit) {
    val gate = remember { StoreGate(UserDefaultsStoreCache(), bridge, ::nowMillis).also { g -> debug?.let(g::debug) } }
    var state by remember { mutableStateOf(gate.state) }
    DisposableEffect(gate) {
        gate.onChange = { state = it }
        onDispose { gate.close() }
    }
    // Bei jedem Start und jeder Rueckkehr in die App: Kaufstatus abfragen, Ablauf pruefen
    LifecycleStartEffect(gate) {
        gate.start()
        onStopOrDispose { }
    }
    // Testphase laeuft auch bei offener App ab
    LaunchedEffect(state.access) {
        val left = gate.trialRemainingMillis() ?: return@LaunchedEffect
        delay(left + 500)
        gate.refresh()
    }
    when (state.access) {
        StoreAccess.FULL, StoreAccess.TRIAL -> content()
        StoreAccess.OFFER, StoreAccess.EXPIRED -> AappsTheme(accent) { StoreScreen(appName, state, gate::startTrial, gate::buy, gate::restore) }
    }
}

// Angebot (vor der Testphase) und Dank (danach) teilen sich den Aufbau
@Composable
fun StoreScreen(appName: String, state: StoreState, onTrial: () -> Unit, onBuy: () -> Unit, onRestore: () -> Unit) {
    val offer = state.access == StoreAccess.OFFER
    val price = state.price
    // auch bei ausstehendem Kauf aktiv: eine abgelehnte Kaufanfrage meldet StoreKit nicht
    val canBuy = price != null && !state.busy
    val uri = LocalUriHandler.current
    val unlockLabel = if (price != null) stringResource(Res.string.store_unlock_price, price) else stringResource(Res.string.store_unlock)
    val hint = when {
        state.pending -> Res.string.store_hint_pending
        state.notice == StoreNotice.OFFLINE -> Res.string.store_hint_offline
        state.notice == StoreNotice.NOT_FOUND -> Res.string.store_hint_notfound
        state.notice == StoreNotice.UNAVAILABLE -> Res.string.store_hint_unavailable
        state.notice == StoreNotice.FAILED -> Res.string.store_hint_failed
        price == null -> Res.string.store_hint_noprice
        else -> null
    }
    // Apple 3.1.1: Dauer, was danach gesperrt ist, und der Preis stehen vor dem Start der Testphase
    val text = stringResource(if (offer) Res.string.store_offer_text else Res.string.store_text)
    val priceLine = if (offer && price != null) "\n\n" + stringResource(Res.string.store_offer_price, price) else ""
    ThanksLayout(
        appName = appName,
        title = stringResource(if (offer) Res.string.store_offer_title else Res.string.store_title),
        text = text + priceLine,
        points = listOf(
            ThanksPoint(AappsIcons.Shield, stringResource(Res.string.store_point_data)),
            ThanksPoint(AappsIcons.Offline, stringResource(Res.string.store_point_offline)),
            ThanksPoint(AappsIcons.Code, stringResource(Res.string.store_point_source), SOURCE_URL.removePrefix("https://")) {
                try {
                    uri.openUri(SOURCE_URL)
                } catch (_: Exception) {
                    // ohne Browser: nichts tun
                }
            },
        ),
        primary = {
            if (offer) {
                val canTrial = state.trialAvailable && !state.busy
                AappsPillButton(stringResource(Res.string.store_trial_start), onTrial, icon = AappsIcons.Unlock, active = canTrial, enabled = canTrial)
            } else {
                AappsPillButton(unlockLabel, onBuy, icon = AappsIcons.Unlock, active = canBuy, enabled = canBuy)
            }
        },
        hint = hint?.let { stringResource(it) },
        secondary = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (offer && canBuy) AappsTextButton(unlockLabel, onBuy)
                AappsTextButton(
                    stringResource(if (state.busy) Res.string.store_restoring else Res.string.store_restore),
                    { if (!state.busy) onRestore() },
                )
            }
        },
        footer = stringResource(Res.string.store_once),
    )
}
