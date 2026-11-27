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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
import solutions.xapient.nordr.res.*
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.nordr.APP_NAME
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
        SettingsGroup(stringResource(Res.string.group_north)) {
            SettingsChoice(
                stringResource(Res.string.reference),
                listOf(
                    NorthRef.MAGNETIC to stringResource(Res.string.north_magnetic),
                    NorthRef.TRUE to stringResource(Res.string.north_true),
                ),
                northRef,
                onNorthRef,
            )
            SettingsDivider()
            SettingsAction(
                stringResource(Res.string.region),
                stringResource(Res.string.region_sub, regionName(region), declinationText(declination)),
                onClick = onRegion,
            )
        }
        SettingsGroup(stringResource(Res.string.group_compass)) {
            SettingsToggle(stringResource(Res.string.haptics), haptics, onHaptics)
            SettingsDivider()
            SettingsAction(
                stringResource(Res.string.show_calibration),
                stringResource(Res.string.show_calibration_sub),
                onClick = onShowCalibration,
            )
        }
        if (diagnostics != null) {
            SettingsGroup(stringResource(Res.string.group_debug)) { diagnostics() }
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

// Regionsliste mit Deklination; eine nahe Stadt reicht
@Composable
fun RegionPicker(selected: Region, declinationOf: (Region) -> Float, onSelect: (Region) -> Unit, onBack: () -> Unit) {
    val c = Aapps.colors
    SettingsScreen(onBack) {
        BasicText(
            stringResource(Res.string.region_note),
            Modifier.padding(horizontal = 12.dp),
            style = Aapps.type.body.copy(color = c.fgMuted),
        )
        SettingsGroup(stringResource(Res.string.region)) {
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
    val v = formatDeclination(deg, decimalSeparator())
    return when {
        deg >= 0.05f -> stringResource(Res.string.decl_east, v)
        deg <= -0.05f -> stringResource(Res.string.decl_west, v)
        else -> v
    }
}

@Composable
fun regionName(region: Region): String = regionNameRes(region.id)?.let { stringResource(it) } ?: region.id

// Name je Region; null = unbekannt (Test prueft, dass alle Regionen einen Namen haben)
fun regionNameRes(id: String): StringResource? = when (id) {
    "vienna" -> Res.string.region_vienna
    "innsbruck" -> Res.string.region_innsbruck
    "berlin" -> Res.string.region_berlin
    "hamburg" -> Res.string.region_hamburg
    "cologne" -> Res.string.region_cologne
    "munich" -> Res.string.region_munich
    "zurich" -> Res.string.region_zurich
    "amsterdam" -> Res.string.region_amsterdam
    "london" -> Res.string.region_london
    "paris" -> Res.string.region_paris
    "madrid" -> Res.string.region_madrid
    "rome" -> Res.string.region_rome
    "ljubljana" -> Res.string.region_ljubljana
    "prague" -> Res.string.region_prague
    "budapest" -> Res.string.region_budapest
    "warsaw" -> Res.string.region_warsaw
    "copenhagen" -> Res.string.region_copenhagen
    "stockholm" -> Res.string.region_stockholm
    "athens" -> Res.string.region_athens
    "istanbul" -> Res.string.region_istanbul
    "new_york" -> Res.string.region_new_york
    "chicago" -> Res.string.region_chicago
    "denver" -> Res.string.region_denver
    "los_angeles" -> Res.string.region_los_angeles
    "mexico_city" -> Res.string.region_mexico_city
    "sao_paulo" -> Res.string.region_sao_paulo
    "cape_town" -> Res.string.region_cape_town
    "dubai" -> Res.string.region_dubai
    "delhi" -> Res.string.region_delhi
    "beijing" -> Res.string.region_beijing
    "tokyo" -> Res.string.region_tokyo
    "sydney" -> Res.string.region_sydney
    "auckland" -> Res.string.region_auckland
    else -> null
}

@Composable
fun NordrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(Res.string.note_navigation)),
        icon = {
            Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}
