package solutions.xapient.aapps.core.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import solutions.xapient.aapps.core.billing.Access
import solutions.xapient.aapps.core.billing.TrialGate

/** Testphase vor der App: 24 h frei, danach Dankes-Bildschirm bis zum Kauf */
@Composable
fun TrialGated(accent: Color, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val gate = remember { TrialGate.factory(context).also { it.handleDebug(activity?.intent) } }
    var state by remember { mutableStateOf(gate.state) }
    DisposableEffect(gate) {
        gate.onChange = { state = it }
        onDispose { gate.close() }
    }
    // Bei jedem Start Kaufstatus abfragen
    LifecycleStartEffect(gate) {
        gate.refresh()
        onStopOrDispose { }
    }
    if (state.access == Access.EXPIRED) {
        val name = remember { context.applicationInfo.loadLabel(context.packageManager).toString() }
        AappsTheme(accent) {
            ThanksScreen(name, state, onBuy = { activity?.let(gate::buy) }, onRestore = gate::restore)
        }
    } else {
        content()
    }
}
