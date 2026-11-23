package solutions.xapient.roundr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.roundr.R

@Composable
fun RoundrSettings(
    sound: Boolean,
    onSound: (Boolean) -> Unit,
    vibrate: Boolean,
    onVibrate: (Boolean) -> Unit,
    keepOn: Boolean,
    onKeepOn: (Boolean) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(R.string.group_signals)) {
            SettingsToggle(stringResource(R.string.sound), sound, onSound, stringResource(R.string.sound_sub))
            SettingsDivider()
            SettingsToggle(stringResource(R.string.vibrate), vibrate, onVibrate)
        }
        SettingsGroup(stringResource(R.string.group_display)) {
            SettingsToggle(stringResource(R.string.keep_on), keepOn, onKeepOn, stringResource(R.string.keep_on_sub))
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun RoundrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        tagline = stringResource(R.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(R.string.note_background)),
        icon = { Image(painterResource(R.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}
