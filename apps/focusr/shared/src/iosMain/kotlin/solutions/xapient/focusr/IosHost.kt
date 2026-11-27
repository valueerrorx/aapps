package solutions.xapient.focusr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateByAddingTimeInterval
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import solutions.xapient.focusr.domain.TimeText
import solutions.xapient.focusr.domain.Timer
import solutions.xapient.focusr.res.Res
import solutions.xapient.focusr.res.notif_done_text
import solutions.xapient.focusr.res.state_done

/**
 * Ende auf iOS: eine lokale Benachrichtigung mit Glockenton (kommt auch bei gesperrtem Bildschirm und beendeter App);
 * ist die App im Vordergrund, zeigt iOS sie nicht, dann spielt focusr Glocke und Vibration selbst.
 */
@OptIn(ExperimentalForeignApi::class)
class IosHost(private val prefs: IosPrefs) : FocusrHost {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val scope = MainScope()
    private var job: Job? = null
    private var player: AVAudioPlayer? = null

    /** Vom Oberflaechen-Lebenszyklus gesetzt: nur im Vordergrund selbst klingeln */
    var active = false

    override val countdownNotification = false

    override fun endText(inMs: Long): String {
        // Stunde:Minute im Format der Region (14:35 bzw. 2:35 PM)
        val f = NSDateFormatter().apply { setLocalizedDateFormatFromTemplate("jmm") }
        return f.stringFromDate(NSDate().dateByAddingTimeInterval(inMs / 1000.0))
    }

    override fun sync(timer: Timer) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(ID))
        job?.cancel()
        val now = timerClock()
        val at = timer.alarmAt(now) ?: return
        val sound = prefs.sound
        job = scope.launch {
            val content = UNMutableNotificationContent().apply {
                setTitle(getString(Res.string.state_done))
                setBody(getString(Res.string.notif_done_text, TimeText.clock(timer.totalMs)))
                if (sound) setSound(UNNotificationSound.soundNamed(CHIME))
            }
            val left = ((at - timerClock()) / 1000.0).coerceAtLeast(1.0)
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(left, false)
            center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(ID, content, trigger), null)
            delay((at - timerClock()).coerceAtLeast(0L))
            // nach der Rueckkehr aus dem Hintergrund nicht verspaetet klingeln: das hat die Benachrichtigung erledigt
            if (active && timerClock() - at < LATE_MS) alert()
        }
    }

    /** App sichtbar: gezeigtes „Zeit um" wegraeumen (wie Android beim Oeffnen) */
    fun clearDelivered() = center.removeDeliveredNotificationsWithIdentifiers(listOf(ID))

    private fun alert() {
        if (prefs.vibrate) {
            scope.launch {
                // Muster wie Android (~2 s): drei Stoesse; iPads ohne Vibrationsmotor bleiben still
                repeat(3) {
                    AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
                    delay(600)
                }
            }
        }
        if (prefs.sound) chime()
    }

    // Glocke wie ein Wecker auch bei Stummschalter; andere Wiedergabe wird nur leiser
    private fun chime() {
        val url = NSBundle.mainBundle.URLForResource("chime", "wav") ?: return
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, AVAudioSessionCategoryOptionDuckOthers, null)
        session.setActive(true, null)
        val p = AVAudioPlayer(contentsOfURL = url, error = null)
        player = p
        p.play()
        scope.launch {
            delay((p.duration * 1000).toLong() + 300)
            if (player === p) {
                player = null
                session.setActive(false, AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, null)
            }
        }
    }

    companion object {
        private const val ID = "focusr.done"
        private const val CHIME = "chime.wav"
        private const val LATE_MS = 2000L
    }
}
