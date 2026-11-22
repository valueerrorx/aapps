package solutions.xapient.hertzr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

// Schutz fuer die Ohren, solange der Prozess lebt: Kopfhoerer erkennen, bei Kopfhoerer ab und Telefonat stoppen.
// Haengt am Store, damit er auch greift, wenn der Vordergrund-Dienst nicht starten durfte.
class AudioGuard(context: Context, private val store: ToneStore) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var armed = false
    // AudioManager.OnModeChangedListener ab API 31
    private var modeListener: Any? = null
    private var wasInCall = false

    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) store.stop()
        }
    }

    private val devices = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) = check()
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) = check()
    }

    // ohne Listener (API < 31): Modus jede Sekunde pruefen, nur der Wechsel in ein Gespraech stoppt
    private val poll = object : Runnable {
        override fun run() {
            val now = inCall(audio.mode)
            if (now && !wasInCall) store.stop()
            wasInCall = now
            if (armed) main.postDelayed(this, 1_000)
        }
    }

    init {
        audio.registerAudioDeviceCallback(devices, main)
        check()
        MainScope().launch { store.playing.collect { if (it) arm() else disarm() } }
    }

    private fun check() = store.setHeadphones(audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in HEADPHONE_TYPES })

    private fun arm() {
        if (armed) return
        armed = true
        ContextCompat.registerReceiver(app, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
        if (Build.VERSION.SDK_INT >= 31) {
            val l = AudioManager.OnModeChangedListener { if (inCall(it)) store.stop() }
            audio.addOnModeChangedListener(app.mainExecutor, l)
            modeListener = l
        } else {
            wasInCall = inCall(audio.mode)
            main.postDelayed(poll, 1_000)
        }
    }

    private fun disarm() {
        if (!armed) return
        armed = false
        app.unregisterReceiver(noisy)
        if (Build.VERSION.SDK_INT >= 31) {
            (modeListener as? AudioManager.OnModeChangedListener)?.let { audio.removeOnModeChangedListener(it) }
            modeListener = null
        } else {
            main.removeCallbacks(poll)
        }
    }

    private fun inCall(mode: Int) = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION

    companion object {
        val HEADPHONE_TYPES = buildSet {
            add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_USB_HEADSET)
            if (Build.VERSION.SDK_INT >= 28) add(AudioDeviceInfo.TYPE_HEARING_AID)
            if (Build.VERSION.SDK_INT >= 31) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
    }
}
