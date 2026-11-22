package solutions.xapient.focusr

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import solutions.xapient.focusr.domain.TimeText
import java.util.Date

// Benachrichtigungen: Countdown im Hintergrund (still) und "Zeit um"; Ton und Vibration macht Alert
object Notifier {
    private const val CH_DONE = "done"
    private const val CH_RUNNING = "running"
    const val ID_DONE = 1
    const val ID_RUNNING = 2
    private const val ACCENT = 0xFFFF6E5A.toInt()

    // Countdown nie negativ: kurz nach dem Ende weg, auch wenn der Alarm ausbleibt
    const val TIMEOUT_SLACK_MS = 2000L

    fun allowed(ctx: Context): Boolean {
        val perm = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return perm && NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    private fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, ctx.getString(R.string.channel_done), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_RUNNING, ctx.getString(R.string.channel_running), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        )
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun clockText(ctx: Context, at: Long): String = DateFormat.getTimeFormat(ctx).format(Date(at))

    fun showDone(ctx: Context, setMs: Long) {
        if (!allowed(ctx)) return
        channels(ctx)
        val n = NotificationCompat.Builder(ctx, CH_DONE)
            .setSmallIcon(R.drawable.ic_stat_focusr)
            .setColor(ACCENT)
            .setContentTitle(ctx.getString(R.string.state_done))
            .setContentText(ctx.getString(R.string.notif_done_text, TimeText.clock(setMs)))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .build()
        notify(ctx, ID_DONE, n)
    }

    // Countdown bis leftMs; Wanduhr nur fuer Anzeige, nach dem Ende verschwindet sie von selbst
    fun showRunning(ctx: Context, leftMs: Long) {
        if (!allowed(ctx) || leftMs <= 0) return
        channels(ctx)
        val endAt = System.currentTimeMillis() + leftMs
        val n = NotificationCompat.Builder(ctx, CH_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_focusr)
            .setColor(ACCENT)
            .setContentTitle(ctx.getString(R.string.app_name))
            .setContentText(ctx.getString(R.string.notif_running_text, clockText(ctx, endAt)))
            .setWhen(endAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter(leftMs + TIMEOUT_SLACK_MS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp(ctx))
            .build()
        notify(ctx, ID_RUNNING, n)
    }

    fun cancel(ctx: Context, id: Int) = NotificationManagerCompat.from(ctx).cancel(id)

    private fun notify(ctx: Context, id: Int, n: android.app.Notification) {
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: SecurityException) {
            // Recht zwischen Pruefung und Anzeige entzogen
        }
    }
}
