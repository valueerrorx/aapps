package solutions.xapient.spinr.ui

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
import solutions.xapient.spinr.res.*
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.spinr.APP_NAME

@Composable
fun SpinrSettings(
    haptics: Boolean,
    onHaptics: (Boolean) -> Unit,
    sound: Boolean,
    onSound: (Boolean) -> Unit,
    activeName: String,
    onWheels: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(Res.string.group_wheel)) {
            SettingsToggle(stringResource(Res.string.haptics), haptics, onHaptics)
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.sound), sound, onSound, stringResource(Res.string.sound_sub))
            SettingsDivider()
            SettingsAction(stringResource(Res.string.wheels), activeName, onClick = onWheels)
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun SpinrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(Res.string.note_fair)),
        icon = {
            Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}
