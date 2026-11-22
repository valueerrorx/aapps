package solutions.xapient.focusr

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import solutions.xapient.focusr.domain.Timer

// Exakter Alarm zum Ende; kein Dienst, die App darf dazwischen sterben
object EndAlarm {
    const val ACTION = "solutions.xapient.focusr.END"

    private fun pending(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, 0,
        Intent(ctx, EndReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // Alarm passend zum Timer setzen oder loeschen
    fun sync(ctx: Context, timer: Timer, now: Long = System.currentTimeMillis()) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val at = timer.alarmAt(now)
        if (at == null) {
            am.cancel(pending(ctx))
            return
        }
        val pi = pending(ctx)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (_: SecurityException) {
            // Recht genau jetzt entzogen: ungenau ist besser als gar nicht
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}
