package solutions.xapient.roundr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import solutions.xapient.roundr.domain.Workout

// Recht fuer die Benachrichtigung einmal VOR dem ersten Start erfragen; gestartet wird danach, der Timer laeuft auch ohne
@Composable
fun rememberStart(prefs: Prefs, store: RunStore): (Workout) -> Unit {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<Workout>(1) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pending[0]?.let(store::start)
        pending[0] = null
    }
    return { w ->
        if (Build.VERSION.SDK_INT >= 33 && !prefs.askedNotify &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            prefs.askedNotify = true
            pending[0] = w
            ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            store.start(w)
        }
    }
}
