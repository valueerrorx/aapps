package solutions.xapient.pulsr.ui

import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.pulsr.APP_NAME
import solutions.xapient.pulsr.res.*
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
        SettingsGroup(stringResource(Res.string.group_sound)) {
            SettingsChoice(
                stringResource(Res.string.sound),
                listOf(
                    Sound.CLICK to stringResource(Res.string.sound_click),
                    Sound.WOOD to stringResource(Res.string.sound_wood),
                    Sound.BEEP to stringResource(Res.string.sound_beep),
                ),
                settings.sound,
            ) { onChange(settings.copy(sound = it)) }
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.accent), settings.accent, { onChange(settings.copy(accent = it)) }, stringResource(Res.string.accent_sub))
        }
        SettingsGroup(stringResource(Res.string.group_display)) {
            SettingsToggle(stringResource(Res.string.keep_on), settings.keepOn, { onChange(settings.copy(keepOn = it)) }, stringResource(Res.string.keep_on_sub))
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun PulsrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(Res.string.note_background)),
        icon = { Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}
