package solutions.xapient.dozr

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.text.format.DateFormat
import solutions.xapient.aapps.core.billing.TrialGate
import solutions.xapient.dozr.domain.Mode
import java.util.Date

// Schnelleinstellung: startet mit dem letzten Wert bzw. stoppt
class DozrTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        update()
    }

    override fun onClick() {
        super.onClick()
        val store = SleepStore.get(this)
        if (store.state.value.mode == Mode.RUNNING) {
            store.stop()
        } else if (TrialGate.expired(this)) {
            // Testphase vorbei: App zeigt den Dankes-Bildschirm
            openApp(start = false)
        } else {
            SleepService.restorePending(this)
            store.start()
            // System lehnt den Dienst-Start ab: App oeffnen, die startet aus dem Vordergrund
            if (!SleepService.start(this)) {
                store.stop()
                openApp(start = true)
            }
        }
        update()
    }

    // Intent-Variante nur vor Android 14, dort ist sie die einzige
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp(start: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(if (start) MainActivity.ACTION_START else Intent.ACTION_MAIN)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val go = {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
        if (isLocked) unlockAndRun { go() } else go()
    }

    private fun update() {
        val tile = qsTile ?: return
        val s = SleepStore.get(this).state.value
        val running = s.mode == Mode.RUNNING
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = if (running) {
                val wall = System.currentTimeMillis() + (s.endAt - SystemClock.elapsedRealtime())
                getString(R.string.tile_until, DateFormat.getTimeFormat(this).format(Date(wall)))
            } else {
                getString(R.string.tile_minutes, s.setMin)
            }
        }
        tile.updateTile()
    }

    companion object {
        fun component(context: Context) = ComponentName(context, DozrTile::class.java)

        // Kachel nach jeder Aenderung neu zeichnen lassen
        fun refresh(context: Context) {
            try {
                requestListeningState(context, component(context))
            } catch (_: RuntimeException) {
                // Kachel nicht hinzugefuegt oder System lehnt ab
            }
        }
    }
}
