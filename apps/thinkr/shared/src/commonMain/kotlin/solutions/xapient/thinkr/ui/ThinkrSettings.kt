package solutions.xapient.thinkr.ui

import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.thinkr.APP_NAME
import solutions.xapient.thinkr.res.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
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
        SettingsGroup(stringResource(Res.string.group_play)) {
            SettingsToggle(stringResource(Res.string.relaxed), relaxed, onRelaxed, stringResource(Res.string.relaxed_sub))
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.safe_colors), safeColors, onSafeColors, stringResource(Res.string.safe_colors_sub))
        }
        SettingsGroup(stringResource(Res.string.group_device)) {
            SettingsToggle(stringResource(Res.string.haptics), haptics, onHaptics)
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.sound), sound, onSound, stringResource(Res.string.sound_sub))
        }
        SettingsGroup(stringResource(Res.string.group_levels)) {
            levels.forEachIndexed { i, item ->
                if (i > 0) SettingsDivider()
                val asking = confirm == item.id
                SettingsAction(
                    stringResource(Res.string.level_of, item.name, item.level),
                    stringResource(if (asking) Res.string.reset_confirm else Res.string.reset_sub),
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
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun ThinkrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(
            stringResource(Res.string.note_echo),
            stringResource(Res.string.note_switches),
            stringResource(Res.string.note_stars),
            stringResource(Res.string.note_ink),
            stringResource(Res.string.note_swarm),
            stringResource(Res.string.note_bounce),
            stringResource(Res.string.note_drops),
            stringResource(Res.string.note_glance),
            stringResource(Res.string.note_slalom),
            stringResource(Res.string.note_daily),
            stringResource(Res.string.note_relaxed),
            stringResource(Res.string.note_access),
            stringResource(Res.string.note_health),
        ),
        icon = {
            Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}
