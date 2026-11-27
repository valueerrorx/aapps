package solutions.xapient.hertzr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

// Android-Rahmen: Benachrichtigungs-Recht und Vordergrund-Dienst, Oberflaeche in HertzrContent
@Composable
fun HertzrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val store = remember { ToneStore.get(context) }
    val playing by store.playing.collectAsState()
    val rate = remember { AudioEngine.nativeRate() }

    // Recht fuer die Benachrichtigung VOR dem Start erfragen; gespielt wird auch ohne.
    // Endgueltige Ablehnung erkennen: Hinweis war da und ist nach dem Nein weg. Weggetippt = beim naechsten Start wieder.
    fun rationale(): Boolean =
        Build.VERSION.SDK_INT >= 33 && context.activity()?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
    var rationaleBefore by remember { mutableStateOf(false) }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && rationaleBefore && !rationale()) prefs.notifyBlocked = true
        store.start()
    }
    // liest den Store direkt: die gemerkten Aktionen sehen sonst einen alten Stand
    fun toggle() {
        if (store.playing.value) {
            store.stop()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !prefs.notifyBlocked &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            rationaleBefore = rationale()
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        store.start()
    }

    // Dienst haelt den Ton bei ausgeschaltetem Bildschirm am Leben
    LaunchedEffect(playing) {
        if (playing) ToneService.start(context)
    }

    HertzrContent(store, rate, BuildConfig.VERSION_NAME, onToggle = ::toggle)
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
