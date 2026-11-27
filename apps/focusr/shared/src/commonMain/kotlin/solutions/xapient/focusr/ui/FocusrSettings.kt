package solutions.xapient.focusr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsChoice
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.focusr.res.*
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.focusr.APP_NAME
import solutions.xapient.focusr.domain.Scale

@Composable
fun FocusrSettings(
    scale: Scale,
    onScale: (Scale) -> Unit,
    sound: Boolean,
    onSound: (Boolean) -> Unit,
    vibrate: Boolean,
    onVibrate: (Boolean) -> Unit,
    haptics: Boolean,
    onHaptics: (Boolean) -> Unit,
    keepOn: Boolean,
    onKeepOn: (Boolean) -> Unit,
    notificationsMissing: Boolean,
    onNotifications: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    // iOS zeigt im Hintergrund keinen laufenden Countdown, nur das Ende
    countdownNotification: Boolean = true,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(Res.string.group_timer)) {
            SettingsChoice(
                stringResource(Res.string.scale),
                Scale.entries.map { it to it.minutes.toString() },
                scale, onScale,
            )
        }
        SettingsGroup(stringResource(Res.string.group_end)) {
            SettingsToggle(stringResource(Res.string.sound), sound, onSound)
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.vibrate), vibrate, onVibrate)
            if (notificationsMissing) {
                SettingsDivider()
                SettingsAction(stringResource(Res.string.notifications), stringResource(if (countdownNotification) Res.string.notifications_sub else Res.string.notifications_sub_done), onClick = onNotifications)
            }
        }
        SettingsGroup(stringResource(Res.string.group_device)) {
            SettingsToggle(stringResource(Res.string.haptics), haptics, onHaptics)
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.keep_on), keepOn, onKeepOn)
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun FocusrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(Res.string.note_gestures)),
        icon = {
            Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}
