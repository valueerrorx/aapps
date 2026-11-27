package solutions.xapient.sleepr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.BackEffect
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.sleepr.res.Res
import solutions.xapient.sleepr.res.ic_launcher_about
import solutions.xapient.sleepr.res.note_background
import solutions.xapient.sleepr.res.note_privacy
import solutions.xapient.sleepr.res.tagline
import solutions.xapient.sleepr.ui.MixerScreen
import solutions.xapient.sleepr.ui.SleeprSettings

enum class Screen { MAIN, SETTINGS, ABOUT }

/** Mixer, Einstellungen und Info; der Player kommt von der Plattform */
@Composable
fun SleeprApp(mix: Mix, display: Display, player: NoisePlayer, versionName: String) {
    var screen by remember { mutableStateOf(Screen.MAIN) }
    DisposableEffect(player) { onDispose { player.release() } }
    AappsTheme(SleeprAccent) {
        val haptics = rememberHaptics(true)
        when (screen) {
            Screen.MAIN -> MixerScreen(
                mix = mix,
                display = display,
                player = player,
                onPress = { haptics.press() },
                onSettings = { screen = Screen.SETTINGS },
            )
            Screen.SETTINGS -> {
                BackEffect { screen = Screen.MAIN }
                SleeprSettings(
                    mix = mix,
                    display = display,
                    onPreset = { p ->
                        mix.applyPreset(p)
                        player.update()
                        haptics.press()
                        screen = Screen.MAIN
                    },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.MAIN },
                )
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                SleeprAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}

@Composable
fun SleeprAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(Res.string.note_privacy), stringResource(Res.string.note_background)),
    ) {
        Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp))
    }
}
