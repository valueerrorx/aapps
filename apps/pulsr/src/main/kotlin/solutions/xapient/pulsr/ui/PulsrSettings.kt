package solutions.xapient.pulsr.ui

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
import solutions.xapient.pulsr.R
import solutions.xapient.pulsr.domain.Settings
import solutions.xapient.pulsr.domain.Sound

@Composable
fun PulsrSettings(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(R.string.group_sound)) {
            SettingsChoice(
                stringResource(R.string.sound),
                listOf(
                    Sound.CLICK to stringResource(R.string.sound_click),
                    Sound.WOOD to stringResource(R.string.sound_wood),
                    Sound.BEEP to stringResource(R.string.sound_beep),
                ),
                settings.sound,
            ) { onChange(settings.copy(sound = it)) }
            SettingsDivider()
            SettingsToggle(stringResource(R.string.accent), settings.accent, { onChange(settings.copy(accent = it)) }, stringResource(R.string.accent_sub))
        }
        SettingsGroup(stringResource(R.string.group_display)) {
            SettingsToggle(stringResource(R.string.keep_on), settings.keepOn, { onChange(settings.copy(keepOn = it)) }, stringResource(R.string.keep_on_sub))
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun PulsrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        tagline = stringResource(R.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(R.string.note_background)),
        icon = { Image(painterResource(R.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}
