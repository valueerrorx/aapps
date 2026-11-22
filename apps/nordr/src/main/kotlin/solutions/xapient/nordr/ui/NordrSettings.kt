package solutions.xapient.nordr.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsChoice
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.nordr.R
import solutions.xapient.nordr.domain.NorthRef
import solutions.xapient.nordr.domain.Region
import solutions.xapient.nordr.domain.Regions

@Composable
fun NordrSettings(
    northRef: NorthRef,
    onNorthRef: (NorthRef) -> Unit,
    region: Region,
    declination: Float,
    onRegion: () -> Unit,
    haptics: Boolean,
    onHaptics: (Boolean) -> Unit,
    onShowCalibration: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    diagnostics: (@Composable () -> Unit)? = null,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(R.string.group_north)) {
            SettingsChoice(
                stringResource(R.string.reference),
                listOf(
                    NorthRef.MAGNETIC to stringResource(R.string.north_magnetic),
                    NorthRef.TRUE to stringResource(R.string.north_true),
                ),
                northRef,
                onNorthRef,
            )
            SettingsDivider()
            SettingsAction(
                stringResource(R.string.region),
                stringResource(R.string.region_sub, regionName(region), declinationText(declination)),
                onClick = onRegion,
            )
        }
        SettingsGroup(stringResource(R.string.group_compass)) {
            SettingsToggle(stringResource(R.string.haptics), haptics, onHaptics)
            SettingsDivider()
            SettingsAction(
                stringResource(R.string.show_calibration),
                stringResource(R.string.show_calibration_sub),
                onClick = onShowCalibration,
            )
        }
        if (diagnostics != null) {
            SettingsGroup(stringResource(R.string.group_debug)) { diagnostics() }
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_about), onClick = onAbout)
        }
    }
}

// Regionsliste mit Deklination; eine nahe Stadt reicht
@Composable
fun RegionPicker(selected: Region, declinationOf: (Region) -> Float, onSelect: (Region) -> Unit, onBack: () -> Unit) {
    val c = Aapps.colors
    SettingsScreen(onBack) {
        BasicText(
            stringResource(R.string.region_note),
            Modifier.padding(horizontal = 12.dp),
            style = Aapps.type.body.copy(color = c.fgMuted),
        )
        SettingsGroup(stringResource(R.string.region)) {
            Regions.all.forEachIndexed { i, region ->
                if (i > 0) SettingsDivider()
                RegionRow(regionName(region), declinationText(declinationOf(region)), region.id == selected.id) { onSelect(region) }
            }
        }
    }
}

@Composable
private fun RegionRow(name: String, declination: String, selected: Boolean, onClick: () -> Unit) {
    val c = Aapps.colors
    val check by animateColorAsState(if (selected) c.accent else Color.Transparent, Springs.gentle(), label = "check")
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected, indication = null, interactionSource = null, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(name, style = Aapps.type.body.copy(color = if (selected) c.accent else c.fg))
        }
        BasicText(declination, style = Aapps.type.number.copy(color = c.fgMuted, fontSize = Aapps.type.body.fontSize))
        Spacer(Modifier.width(12.dp))
        AappsIcon(AappsIcons.Check, null, check, size = 20.dp)
    }
}

@Composable
fun declinationText(deg: Float): String {
    val locale = LocalConfiguration.current.locales[0]
    val v = formatDeclination(deg, locale)
    return when {
        deg >= 0.05f -> stringResource(R.string.decl_east, v)
        deg <= -0.05f -> stringResource(R.string.decl_west, v)
        else -> v
    }
}

@Composable
fun regionName(region: Region): String = stringResource(regionNameRes(region.id))

// Name je Region; 0 = unbekannt (Test prueft, dass alle Regionen einen Namen haben)
fun regionNameRes(id: String): Int = when (id) {
    "vienna" -> R.string.region_vienna
    "innsbruck" -> R.string.region_innsbruck
    "berlin" -> R.string.region_berlin
    "hamburg" -> R.string.region_hamburg
    "cologne" -> R.string.region_cologne
    "munich" -> R.string.region_munich
    "zurich" -> R.string.region_zurich
    "amsterdam" -> R.string.region_amsterdam
    "london" -> R.string.region_london
    "paris" -> R.string.region_paris
    "madrid" -> R.string.region_madrid
    "rome" -> R.string.region_rome
    "ljubljana" -> R.string.region_ljubljana
    "prague" -> R.string.region_prague
    "budapest" -> R.string.region_budapest
    "warsaw" -> R.string.region_warsaw
    "copenhagen" -> R.string.region_copenhagen
    "stockholm" -> R.string.region_stockholm
    "athens" -> R.string.region_athens
    "istanbul" -> R.string.region_istanbul
    "new_york" -> R.string.region_new_york
    "chicago" -> R.string.region_chicago
    "denver" -> R.string.region_denver
    "los_angeles" -> R.string.region_los_angeles
    "mexico_city" -> R.string.region_mexico_city
    "sao_paulo" -> R.string.region_sao_paulo
    "cape_town" -> R.string.region_cape_town
    "dubai" -> R.string.region_dubai
    "delhi" -> R.string.region_delhi
    "beijing" -> R.string.region_beijing
    "tokyo" -> R.string.region_tokyo
    "sydney" -> R.string.region_sydney
    "auckland" -> R.string.region_auckland
    else -> 0
}

@Composable
fun NordrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        tagline = stringResource(R.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(R.string.note_navigation)),
        icon = {
            Image(painterResource(R.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}
