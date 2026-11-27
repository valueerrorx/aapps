package solutions.xapient.roundr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.roundr.res.*
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.roundr.APP_NAME

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
        SettingsGroup(stringResource(Res.string.group_signals)) {
            SettingsToggle(stringResource(Res.string.sound), sound, onSound, stringResource(Res.string.sound_sub))
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.vibrate), vibrate, onVibrate)
        }
        SettingsGroup(stringResource(Res.string.group_display)) {
            SettingsToggle(stringResource(Res.string.keep_on), keepOn, onKeepOn, stringResource(Res.string.keep_on_sub))
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun RoundrAbout(versionName: String, iosNote: Boolean = false, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(if (iosNote) Res.string.note_background_ios else Res.string.note_background)),
        icon = { Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}
