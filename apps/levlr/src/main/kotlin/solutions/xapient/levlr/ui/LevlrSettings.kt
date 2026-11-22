package solutions.xapient.levlr.ui

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
import solutions.xapient.levlr.R
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
        SettingsGroup(stringResource(R.string.group_measure)) {
            SettingsChoice(
                stringResource(R.string.unit),
                listOf(
                    AngleUnit.DEGREE to stringResource(R.string.unit_degree),
                    AngleUnit.PERCENT to stringResource(R.string.unit_percent),
                    AngleUnit.MM_PER_M to "mm/m",
                ),
                unit,
                onUnit,
            )
            SettingsDivider()
            SettingsToggle(stringResource(R.string.haptics), haptics, onHaptics)
        }
        SettingsGroup(stringResource(R.string.group_calibration)) {
            SettingsAction(stringResource(R.string.calibrate), stringResource(R.string.calibrate_sub), onClick = onCalibrate)
            SettingsDivider()
            val yes = stringResource(R.string.calibrated)
            val no = stringResource(R.string.not_calibrated)
            SettingsAction(
                stringResource(R.string.reset_calibration),
                stringResource(
                    R.string.calibration_state,
                    if (calibration.flat.isZero) no else yes,
                    if (calibration.edge.isZero) no else yes,
                ),
                enabled = !calibration.flat.isZero || !calibration.edge.isZero,
                onClick = onResetCalibration,
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

@Composable
fun LevlrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        tagline = stringResource(R.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(R.string.note_survey)),
        icon = {
            Image(painterResource(R.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}
