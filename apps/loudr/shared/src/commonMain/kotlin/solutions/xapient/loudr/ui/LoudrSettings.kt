package solutions.xapient.loudr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsChoice
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.loudr.Settings
import solutions.xapient.loudr.domain.Calibration
import solutions.xapient.loudr.domain.TimeWeighting
import solutions.xapient.loudr.domain.formatDb
import solutions.xapient.loudr.res.Res
import solutions.xapient.loudr.res.app_name
import solutions.xapient.loudr.res.calibration
import solutions.xapient.loudr.res.calibration_now
import solutions.xapient.loudr.res.calibration_reset
import solutions.xapient.loudr.res.calibration_sub
import solutions.xapient.loudr.res.calibration_value
import solutions.xapient.loudr.res.group_display
import solutions.xapient.loudr.res.group_measure
import solutions.xapient.loudr.res.group_note
import solutions.xapient.loudr.res.ic_launcher_about
import solutions.xapient.loudr.res.keep_on
import solutions.xapient.loudr.res.keep_on_sub
import solutions.xapient.loudr.res.lower
import solutions.xapient.loudr.res.note_estimate
import solutions.xapient.loudr.res.note_mic
import solutions.xapient.loudr.res.raise
import solutions.xapient.loudr.res.tagline
import solutions.xapient.loudr.res.weighting
import solutions.xapient.loudr.res.weighting_fast
import solutions.xapient.loudr.res.weighting_slow

@Composable
fun LoudrSettings(
    settings: Settings,
    onChange: (Settings) -> Unit,
    // aktueller Pegel mit Kalibrierung, NaN ohne Messung
    live: () -> Float,
    haptics: Haptics?,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(Res.string.group_measure)) {
            SettingsChoice(
                stringResource(Res.string.weighting),
                listOf(
                    TimeWeighting.FAST to stringResource(Res.string.weighting_fast),
                    TimeWeighting.SLOW to stringResource(Res.string.weighting_slow),
                ),
                settings.weighting,
            ) { onChange(settings.copy(weighting = it)) }
            SettingsDivider()
            CalibrationRow(settings.offset, live, haptics) { onChange(settings.copy(offset = it)) }
            SettingsDivider()
            SettingsAction(stringResource(Res.string.calibration_reset), enabled = settings.offset != 0f) {
                onChange(settings.copy(offset = 0f))
            }
        }
        SettingsGroup(stringResource(Res.string.group_display)) {
            SettingsToggle(stringResource(Res.string.keep_on), settings.keepOn, { onChange(settings.copy(keepOn = it)) }, stringResource(Res.string.keep_on_sub))
        }
        SettingsGroup(stringResource(Res.string.group_note)) {
            BasicText(
                stringResource(Res.string.note_estimate),
                Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                style = Aapps.type.body.copy(color = Aapps.colors.fgMuted),
            )
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

// Versatz in 0,5-dB-Schritten, darunter der Live-Wert zum Abgleich
@Composable
private fun CalibrationRow(offset: Float, live: () -> Float, haptics: Haptics?, onOffset: (Float) -> Unit) {
    val c = Aapps.colors
    val sep = decimalSeparator()
    val value = stringResource(Res.string.calibration_value, Calibration.label(offset, sep))
    // Live-Wert viermal pro Sekunde, nicht je Bild
    val read by rememberUpdatedState(live)
    var now by remember { mutableFloatStateOf(read()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = read()
            delay(250)
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
        BasicText(stringResource(Res.string.calibration), style = Aapps.type.body.copy(color = c.fg))
        Spacer(Modifier.height(2.dp))
        BasicText(
            stringResource(Res.string.calibration_sub),
            style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().background(c.bg, CircleShape).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Step(LoudrIcons.Minus, stringResource(Res.string.lower), offset > Calibration.MIN) {
                haptics?.press()
                onOffset(Calibration.step(offset, -1))
            }
            BasicText(
                value,
                Modifier.weight(1f).semantics { stateDescription = value },
                style = Aapps.type.number.copy(color = if (offset == 0f) c.fg else c.accent, fontSize = 22.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 22.sp, stepSize = 0.5.sp),
            )
            Step(LoudrIcons.Plus, stringResource(Res.string.raise), offset < Calibration.MAX) {
                haptics?.press()
                onOffset(Calibration.step(offset, +1))
            }
        }
        if (!now.isNaN()) {
            Spacer(Modifier.height(10.dp))
            BasicText(
                stringResource(Res.string.calibration_now, formatDb(now)),
                Modifier.fillMaxWidth(),
                style = Aapps.type.number.copy(color = c.fgMuted, fontSize = Aapps.type.body.fontSize, textAlign = TextAlign.Center),
            )
        }
    }
}

@Composable
private fun Step(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier
            .size(48.dp)
            .pressScale(pressed, 0.88f)
            .background(if (pressed) c.surfaceHigh else c.surface, CircleShape)
            .border(1.dp, c.line, CircleShape)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        AappsIcon(icon, null, if (enabled) c.fg else c.fgMuted, size = 20.dp)
    }
}

@Composable
fun LoudrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(Res.string.app_name),
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(Res.string.note_mic), stringResource(Res.string.note_estimate)),
        icon = { Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}
