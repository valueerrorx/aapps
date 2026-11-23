package solutions.xapient.thinkr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.thinkr.R
import solutions.xapient.thinkr.domain.GameId

// Stufe je Spiel zum Zuruecksetzen (zweimal tippen)
data class LevelItem(val id: GameId, val name: String, val level: Int)

@Composable
fun ThinkrSettings(
    relaxed: Boolean,
    onRelaxed: (Boolean) -> Unit,
    safeColors: Boolean,
    onSafeColors: (Boolean) -> Unit,
    haptics: Boolean,
    onHaptics: (Boolean) -> Unit,
    sound: Boolean,
    onSound: (Boolean) -> Unit,
    levels: List<LevelItem>,
    onReset: (GameId) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    confirmInitially: GameId? = null,
) {
    var confirm by rememberSaveable { mutableStateOf(confirmInitially) }
    // Bestaetigung verfaellt nach ein paar Sekunden
    if (confirm != null && confirmInitially == null) LaunchedEffect(confirm) { delay(3000); confirm = null }
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(R.string.group_play)) {
            SettingsToggle(stringResource(R.string.relaxed), relaxed, onRelaxed, stringResource(R.string.relaxed_sub))
            SettingsDivider()
            SettingsToggle(stringResource(R.string.safe_colors), safeColors, onSafeColors, stringResource(R.string.safe_colors_sub))
        }
        SettingsGroup(stringResource(R.string.group_device)) {
            SettingsToggle(stringResource(R.string.haptics), haptics, onHaptics)
            SettingsDivider()
            SettingsToggle(stringResource(R.string.sound), sound, onSound, stringResource(R.string.sound_sub))
        }
        SettingsGroup(stringResource(R.string.group_levels)) {
            levels.forEachIndexed { i, item ->
                if (i > 0) SettingsDivider()
                val asking = confirm == item.id
                SettingsAction(
                    stringResource(R.string.level_of, item.name, item.level),
                    stringResource(if (asking) R.string.reset_confirm else R.string.reset_sub),
                    enabled = item.level > 1 || asking,
                ) {
                    if (asking) {
                        confirm = null
                        onReset(item.id)
                    } else {
                        confirm = item.id
                    }
                }
            }
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun ThinkrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        tagline = stringResource(R.string.tagline),
        onBack = onBack,
        notes = listOf(
            stringResource(R.string.note_echo),
            stringResource(R.string.note_switches),
            stringResource(R.string.note_stars),
            stringResource(R.string.note_ink),
            stringResource(R.string.note_daily),
            stringResource(R.string.note_relaxed),
            stringResource(R.string.note_access),
            stringResource(R.string.note_health),
        ),
        icon = {
            Image(painterResource(R.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}
