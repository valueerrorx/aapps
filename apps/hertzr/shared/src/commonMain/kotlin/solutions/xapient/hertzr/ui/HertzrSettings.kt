package solutions.xapient.hertzr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsChoice
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.hertzr.domain.Output
import solutions.xapient.hertzr.domain.Settings
import solutions.xapient.hertzr.res.Res
import solutions.xapient.hertzr.res.app_name
import solutions.xapient.hertzr.res.group_display
import solutions.xapient.hertzr.res.group_output
import solutions.xapient.hertzr.res.ic_launcher_about
import solutions.xapient.hertzr.res.keep_on
import solutions.xapient.hertzr.res.keep_on_sub
import solutions.xapient.hertzr.res.note_hearing
import solutions.xapient.hertzr.res.output
import solutions.xapient.hertzr.res.output_both
import solutions.xapient.hertzr.res.output_invert
import solutions.xapient.hertzr.res.output_left
import solutions.xapient.hertzr.res.output_right
import solutions.xapient.hertzr.res.tagline

@Composable
fun HertzrSettings(
    settings: Settings,
    onOutput: (Output) -> Unit,
    onKeepOn: (Boolean) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(Res.string.group_output)) {
            SettingsChoice(
                stringResource(Res.string.output),
                listOf(
                    Output.BOTH to stringResource(Res.string.output_both),
                    Output.LEFT to stringResource(Res.string.output_left),
                    Output.RIGHT to stringResource(Res.string.output_right),
                    Output.INVERT to stringResource(Res.string.output_invert),
                ),
                settings.output,
                onOutput,
            )
        }
        SettingsGroup(stringResource(Res.string.group_display)) {
            SettingsToggle(stringResource(Res.string.keep_on), settings.keepOn, onKeepOn, stringResource(Res.string.keep_on_sub))
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun HertzrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(Res.string.app_name),
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(Res.string.note_hearing), backgroundNote()),
        icon = { Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}

// Hinweis zum Weiterspielen: Android ueber die Benachrichtigung, iOS ueber den Sperrbildschirm
@Composable
internal expect fun backgroundNote(): String
