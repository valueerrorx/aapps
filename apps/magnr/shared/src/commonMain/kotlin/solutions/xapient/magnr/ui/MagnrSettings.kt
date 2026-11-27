package solutions.xapient.magnr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsChoice
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.magnr.res.*
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.magnr.APP_NAME
import solutions.xapient.magnr.Settings
import solutions.xapient.magnr.domain.Sensitivity

@Composable
fun MagnrSettings(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onIntro: () -> Unit,
    onCalibrate: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    val scale = stringResource(Res.string.scale_value, formatScale(settings.sensitivity.fullScale, decimalSeparator()))
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(Res.string.group_measure)) {
            SettingsChoice(
                stringResource(Res.string.sensitivity),
                listOf(
                    Sensitivity.LOW to stringResource(Res.string.sens_low),
                    Sensitivity.MEDIUM to stringResource(Res.string.sens_medium),
                    Sensitivity.HIGH to stringResource(Res.string.sens_high),
                ),
                settings.sensitivity,
            ) { onChange(settings.copy(sensitivity = it)) }
            BasicText(
                stringResource(Res.string.sensitivity_sub, scale),
                Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                style = Aapps.type.body.copy(color = Aapps.colors.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f),
            )
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.drift), settings.drift, { onChange(settings.copy(drift = it)) }, stringResource(Res.string.drift_sub))
        }
        SettingsGroup(stringResource(Res.string.group_feedback)) {
            SettingsToggle(stringResource(Res.string.sound), settings.sound, { onChange(settings.copy(sound = it)) }, stringResource(Res.string.sound_sub))
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.haptics), settings.haptics, { onChange(settings.copy(haptics = it)) }, stringResource(Res.string.haptics_sub))
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.keep_on), settings.keepOn, { onChange(settings.copy(keepOn = it)) }, stringResource(Res.string.keep_on_sub))
        }
        SettingsGroup(stringResource(Res.string.group_help)) {
            SettingsAction(stringResource(Res.string.intro_title), stringResource(Res.string.intro_sub), onClick = onIntro)
            SettingsDivider()
            SettingsAction(stringResource(Res.string.cal_title), stringResource(Res.string.cal_sub), onClick = onCalibrate)
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun MagnrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(Res.string.note_honest), stringResource(Res.string.note_sensor)),
        icon = { Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}
