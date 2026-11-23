package solutions.xapient.magnr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsChoice
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.magnr.R
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
    val scale = stringResource(R.string.scale_value, formatScale(settings.sensitivity.fullScale, decimalSeparator()))
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(R.string.group_measure)) {
            SettingsChoice(
                stringResource(R.string.sensitivity),
                listOf(
                    Sensitivity.LOW to stringResource(R.string.sens_low),
                    Sensitivity.MEDIUM to stringResource(R.string.sens_medium),
                    Sensitivity.HIGH to stringResource(R.string.sens_high),
                ),
                settings.sensitivity,
            ) { onChange(settings.copy(sensitivity = it)) }
            BasicText(
                stringResource(R.string.sensitivity_sub, scale),
                Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                style = Aapps.type.body.copy(color = Aapps.colors.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f),
            )
            SettingsDivider()
            SettingsToggle(stringResource(R.string.drift), settings.drift, { onChange(settings.copy(drift = it)) }, stringResource(R.string.drift_sub))
        }
        SettingsGroup(stringResource(R.string.group_feedback)) {
            SettingsToggle(stringResource(R.string.sound), settings.sound, { onChange(settings.copy(sound = it)) }, stringResource(R.string.sound_sub))
            SettingsDivider()
            SettingsToggle(stringResource(R.string.haptics), settings.haptics, { onChange(settings.copy(haptics = it)) }, stringResource(R.string.haptics_sub))
            SettingsDivider()
            SettingsToggle(stringResource(R.string.keep_on), settings.keepOn, { onChange(settings.copy(keepOn = it)) }, stringResource(R.string.keep_on_sub))
        }
        SettingsGroup(stringResource(R.string.group_help)) {
            SettingsAction(stringResource(R.string.intro_title), stringResource(R.string.intro_sub), onClick = onIntro)
            SettingsDivider()
            SettingsAction(stringResource(R.string.cal_title), stringResource(R.string.cal_sub), onClick = onCalibrate)
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun MagnrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        tagline = stringResource(R.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(R.string.note_honest), stringResource(R.string.note_sensor)),
        icon = { Image(painterResource(R.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}
