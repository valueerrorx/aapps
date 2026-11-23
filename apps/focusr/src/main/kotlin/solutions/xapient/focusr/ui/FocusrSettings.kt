package solutions.xapient.focusr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsChoice
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.focusr.R
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
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(R.string.group_timer)) {
            SettingsChoice(
                stringResource(R.string.scale),
                Scale.entries.map { it to it.minutes.toString() },
                scale, onScale,
            )
        }
        SettingsGroup(stringResource(R.string.group_end)) {
            SettingsToggle(stringResource(R.string.sound), sound, onSound)
            SettingsDivider()
            SettingsToggle(stringResource(R.string.vibrate), vibrate, onVibrate)
            if (notificationsMissing) {
                SettingsDivider()
                SettingsAction(stringResource(R.string.notifications), stringResource(R.string.notifications_sub), onClick = onNotifications)
            }
        }
        SettingsGroup(stringResource(R.string.group_device)) {
            SettingsToggle(stringResource(R.string.haptics), haptics, onHaptics)
            SettingsDivider()
            SettingsToggle(stringResource(R.string.keep_on), keepOn, onKeepOn)
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun FocusrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        tagline = stringResource(R.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(R.string.note_gestures)),
        icon = {
            Image(painterResource(R.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}
