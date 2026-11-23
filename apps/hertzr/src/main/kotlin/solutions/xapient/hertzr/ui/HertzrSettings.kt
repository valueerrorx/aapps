package solutions.xapient.hertzr.ui

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
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.hertzr.R
import solutions.xapient.hertzr.domain.Output
import solutions.xapient.hertzr.domain.Settings

@Composable
fun HertzrSettings(
    settings: Settings,
    onOutput: (Output) -> Unit,
    onKeepOn: (Boolean) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(R.string.group_output)) {
            SettingsChoice(
                stringResource(R.string.output),
                listOf(
                    Output.BOTH to stringResource(R.string.output_both),
                    Output.LEFT to stringResource(R.string.output_left),
                    Output.RIGHT to stringResource(R.string.output_right),
                    Output.INVERT to stringResource(R.string.output_invert),
                ),
                settings.output,
                onOutput,
            )
        }
        SettingsGroup(stringResource(R.string.group_display)) {
            SettingsToggle(stringResource(R.string.keep_on), settings.keepOn, onKeepOn, stringResource(R.string.keep_on_sub))
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun HertzrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        tagline = stringResource(R.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(R.string.note_hearing), stringResource(R.string.note_background)),
        icon = { Image(painterResource(R.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}
