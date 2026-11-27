package solutions.xapient.pulsr

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
import solutions.xapient.aapps.core.ui.SharedPrefsKeyValues

@Volatile private var instance: PulseStore? = null

// Ein Metronom pro Prozess, geteilt von Oberflaeche und Dienst
fun PulseStore.Companion.get(context: Context): PulseStore = instance ?: synchronized(this) {
    instance ?: Prefs(SharedPrefsKeyValues(context.applicationContext, "pulsr")).let { p ->
        PulseStore({ p.settings }, { p.settings = it }, AudioEngine())
    }.also { instance = it }
}

// nur Tests
fun PulseStore.Companion.install(store: PulseStore?) {
    instance = store
}

// Android-Rahmen: Benachrichtigungs-Recht vor dem Start, Dienst haelt das Spielen bei gesperrtem Bildschirm am Leben
@Composable
fun AndroidPulsrApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(SharedPrefsKeyValues(context.applicationContext, "pulsr")) }
    val store = remember { PulseStore.get(context) }
    val playing by store.playing.collectAsState()

    // Recht fuer die Benachrichtigung VOR dem Start erfragen; gespielt wird auch ohne.
    // Endgueltige Ablehnung erkennen: Hinweis war da und ist nach dem Nein weg. Weggetippt = beim naechsten Start wieder.
    fun rationale(): Boolean =
        Build.VERSION.SDK_INT >= 33 && context.activity()?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
    var rationaleBefore by remember { mutableStateOf(false) }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && rationaleBefore && !rationale()) prefs.notifyBlocked = true
        store.start()
    }
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

    LaunchedEffect(playing) {
        if (playing) PulseService.start(context)
    }

    PulsrApp(store, BuildConfig.VERSION_NAME, ::toggle)
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
