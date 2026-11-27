package solutions.xapient.aapps.core.ui

import android.hardware.display.DisplayManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import java.text.DecimalFormatSymbols

// Auch 90 -> 270 ohne Config-Wechsel
@Composable
actual fun DisplayRotationEffect(onRotation: (Int) -> Unit) {
    val view = LocalView.current
    val callback = rememberUpdatedState(onRotation)
    DisposableEffect(view) {
        val dm = view.context.getSystemService(DisplayManager::class.java)
        val update = { callback.value(view.display?.rotation ?: 0) }
        update()
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayChanged(displayId: Int) = update()
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
        }
        dm?.registerDisplayListener(listener, null)
        onDispose { dm?.unregisterDisplayListener(listener) }
    }
}

@Composable
actual fun BackEffect(onBack: () -> Unit) = BackHandler(onBack = onBack)

@Composable
actual fun decimalSeparator(): Char =
    DecimalFormatSymbols.getInstance(LocalConfiguration.current.locales[0]).decimalSeparator
