package solutions.xapient.hertzr

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionPortBluetoothA2DP
import platform.AVFAudio.AVAudioSessionPortBluetoothHFP
import platform.AVFAudio.AVAudioSessionPortBluetoothLE
import platform.AVFAudio.AVAudioSessionPortDescription
import platform.AVFAudio.AVAudioSessionPortHeadphones
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.currentRoute
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import solutions.xapient.hertzr.domain.Freq
import solutions.xapient.hertzr.domain.Mode

/**
 * Schutz fuer die Ohren wie auf Android: Kopfhoerer erkennen (Pegelgrenze), bei abgezogenen Kopfhoerern und bei
 * Unterbrechung (Anruf, Wecker) stoppen. Dazu Anzeige und Stopp am Sperrbildschirm statt der Android-Benachrichtigung.
 */
@OptIn(ExperimentalForeignApi::class)
class IosAudioGuard(private val store: ToneStore, private val decimal: Char) {
    init {
        val center = NSNotificationCenter.defaultCenter
        center.addObserverForName(AVAudioSessionInterruptionNotification, null, NSOperationQueue.mainQueue) { n -> onInterruption(n) }
        center.addObserverForName(AVAudioSessionRouteChangeNotification, null, NSOperationQueue.mainQueue) { n -> onRouteChange(n) }
        check()
        remoteCommands()
        MainScope().launch {
            store.playing.collect { nowPlaying() }
        }
        MainScope().launch {
            store.settings.collect { if (store.playing.value) nowPlaying() }
        }
    }

    private fun check() {
        val outs = AVAudioSession.sharedInstance().currentRoute.outputs
        store.setHeadphones(outs.any { (it as? AVAudioSessionPortDescription)?.portType in HEADPHONE_PORTS })
    }

    private fun onInterruption(n: NSNotification?) {
        val type = (n?.userInfo?.get(AVAudioSessionInterruptionTypeKey) as? NSNumber)?.unsignedLongValue ?: return
        if (type == INTERRUPTION_BEGAN) store.stop()
    }

    private fun onRouteChange(n: NSNotification?) {
        val reason = (n?.userInfo?.get(AVAudioSessionRouteChangeReasonKey) as? NSNumber)?.unsignedLongValue
        if (reason == ROUTE_OLD_DEVICE_UNAVAILABLE) store.stop()
        check()
    }

    // Frequenz und Pegel am Sperrbildschirm
    private fun nowPlaying() {
        val info = MPNowPlayingInfoCenter.defaultCenter()
        if (!store.playing.value) {
            info.nowPlayingInfo = null
            return
        }
        val s = store.settings.value
        val what = if (s.mode == Mode.SWEEP) "${Freq.short(s.sweepFrom, decimal)} – ${Freq.short(s.sweepTo, decimal)}" else Freq.short(s.hz, decimal)
        info.nowPlayingInfo = mapOf<Any?, Any?>(
            MPMediaItemPropertyTitle to what,
            MPMediaItemPropertyArtist to "hertzr · ${s.levelDb} dB",
            MPNowPlayingInfoPropertyPlaybackRate to 1.0,
        )
    }

    private fun remoteCommands() {
        val c = MPRemoteCommandCenter.sharedCommandCenter()
        val ok = MPRemoteCommandHandlerStatusSuccess
        c.pauseCommand.addTargetWithHandler { _ -> store.stop(); ok }
        c.stopCommand.addTargetWithHandler { _ -> store.stop(); ok }
        c.playCommand.addTargetWithHandler { _ -> store.start(); ok }
        c.togglePlayPauseCommand.addTargetWithHandler { _ -> store.toggle(); ok }
    }

    private companion object {
        val HEADPHONE_PORTS = setOf(AVAudioSessionPortHeadphones, AVAudioSessionPortBluetoothA2DP, AVAudioSessionPortBluetoothLE, AVAudioSessionPortBluetoothHFP)
        const val INTERRUPTION_BEGAN = 1uL          // AVAudioSessionInterruptionTypeBegan
        const val ROUTE_OLD_DEVICE_UNAVAILABLE = 2uL // AVAudioSessionRouteChangeReasonOldDeviceUnavailable
    }
}
