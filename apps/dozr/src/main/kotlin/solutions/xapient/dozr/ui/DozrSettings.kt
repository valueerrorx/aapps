package solutions.xapient.dozr.ui

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
import solutions.xapient.dozr.R
import solutions.xapient.dozr.domain.FadeLength

@Composable
fun DozrSettings(
    fade: FadeLength,
    onFade: (FadeLength) -> Unit,
    shake: Boolean,
    onShake: (Boolean) -> Unit,
    haptics: Boolean,
    onHaptics: (Boolean) -> Unit,
    notificationsMissing: Boolean,
    onNotifications: () -> Unit,
    // null = Kachel-Hinzufuegen nicht moeglich (vor Android 13)
    onAddTile: (() -> Unit)?,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(R.string.group_end)) {
            SettingsChoice(
                stringResource(R.string.fade_length),
                listOf(
                    FadeLength.M1 to stringResource(R.string.len_1m),
                    FadeLength.M3 to stringResource(R.string.len_3m),
                    FadeLength.M5 to stringResource(R.string.len_5m),
                ),
                fade, onFade,
            )
        }
        SettingsGroup(stringResource(R.string.group_timer)) {
            SettingsToggle(stringResource(R.string.shake), shake, onShake, stringResource(R.string.shake_sub))
            SettingsDivider()
            SettingsToggle(stringResource(R.string.haptics), haptics, onHaptics)
            if (notificationsMissing) {
                SettingsDivider()
                SettingsAction(stringResource(R.string.notifications), stringResource(R.string.notifications_sub), onClick = onNotifications)
            }
            if (onAddTile != null) {
                SettingsDivider()
                SettingsAction(stringResource(R.string.add_tile), stringResource(R.string.add_tile_sub), onClick = onAddTile)
            }
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun DozrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        tagline = stringResource(R.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(R.string.note_how)),
        icon = {
            Image(painterResource(R.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}
