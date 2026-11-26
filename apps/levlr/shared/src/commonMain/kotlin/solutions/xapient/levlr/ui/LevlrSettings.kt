package solutions.xapient.levlr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsChoice
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.levlr.res.*
import solutions.xapient.levlr.APP_NAME
import solutions.xapient.levlr.domain.AngleUnit
import solutions.xapient.levlr.domain.Calibration

@Composable
fun LevlrSettings(
    unit: AngleUnit,
    onUnit: (AngleUnit) -> Unit,
    haptics: Boolean,
    onHaptics: (Boolean) -> Unit,
    calibration: Calibration,
    onCalibrate: () -> Unit,
    onResetCalibration: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    diagnostics: (@Composable () -> Unit)? = null,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(Res.string.group_measure)) {
            SettingsChoice(
                stringResource(Res.string.unit),
                listOf(
                    AngleUnit.DEGREE to stringResource(Res.string.unit_degree),
                    AngleUnit.PERCENT to stringResource(Res.string.unit_percent),
                    AngleUnit.MM_PER_M to "mm/m",
                ),
                unit,
                onUnit,
            )
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.haptics), haptics, onHaptics)
        }
        SettingsGroup(stringResource(Res.string.group_calibration)) {
            SettingsAction(stringResource(Res.string.calibrate), stringResource(Res.string.calibrate_sub), onClick = onCalibrate)
            SettingsDivider()
            val yes = stringResource(Res.string.calibrated)
            val no = stringResource(Res.string.not_calibrated)
            SettingsAction(
                stringResource(Res.string.reset_calibration),
                stringResource(
                    Res.string.calibration_state,
                    if (calibration.flat.isZero) no else yes,
                    if (calibration.edge.isZero) no else yes,
                ),
                enabled = !calibration.flat.isZero || !calibration.edge.isZero,
                onClick = onResetCalibration,
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

@Composable
fun LevlrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(Res.string.note_survey)),
        icon = {
            Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}
