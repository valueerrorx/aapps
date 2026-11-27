package solutions.xapient.hudr.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsChoice
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.hudr.res.*
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.hudr.APP_NAME
import solutions.xapient.hudr.Settings
import solutions.xapient.hudr.domain.Limit
import solutions.xapient.hudr.domain.SpeedUnit

@Composable
fun HudrSettings(
    settings: Settings,
    onChange: (Settings) -> Unit,
    haptics: Haptics?,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(Res.string.group_units)) {
            SettingsChoice(
                stringResource(Res.string.unit),
                SpeedUnit.entries.map { it to stringResource(unitRes(it)) },
                settings.unit,
            ) { onChange(settings.withUnit(it)) }
        }
        SettingsGroup(stringResource(Res.string.group_limit)) {
            SettingsToggle(
                stringResource(Res.string.limit_on), settings.limitOn,
                { onChange(settings.copy(limitOn = it)) }, stringResource(Res.string.limit_on_sub),
            )
            if (settings.limitOn) {
                SettingsDivider()
                LimitRow(settings, haptics) { onChange(settings.copy(limit = it)) }
                SettingsDivider()
                SettingsToggle(
                    stringResource(Res.string.limit_sound), settings.sound,
                    { onChange(settings.copy(sound = it)) }, stringResource(Res.string.limit_sound_sub),
                )
            }
        }
        SettingsGroup(stringResource(Res.string.group_display)) {
            SettingsToggle(stringResource(Res.string.keep_on), settings.keepOn, { onChange(settings.copy(keepOn = it)) }, stringResource(Res.string.keep_on_sub))
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.hud_mirror), settings.mirror, { onChange(settings.copy(mirror = it)) }, stringResource(Res.string.hud_mirror_sub))
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.hud_bright), settings.bright, { onChange(settings.copy(bright = it)) }, stringResource(Res.string.hud_bright_sub))
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

// Schwelle in 5er-Schritten in der gewaehlten Einheit
@Composable
private fun LimitRow(settings: Settings, haptics: Haptics?, onLimit: (Int) -> Unit) {
    val c = Aapps.colors
    val value = stringResource(Res.string.limit_value, settings.limit, stringResource(unitRes(settings.unit)))
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
        BasicText(stringResource(Res.string.limit), style = Aapps.type.body.copy(color = c.fg))
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().background(c.bg, CircleShape).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Step(HudrIcons.Minus, stringResource(Res.string.lower), settings.limit > Limit.MIN) {
                haptics?.press()
                onLimit(Limit.step(settings.limit, -1))
            }
            BasicText(
                value,
                Modifier.weight(1f).semantics { stateDescription = value },
                style = Aapps.type.number.copy(color = Amber, fontSize = 22.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 22.sp, stepSize = 0.5.sp),
            )
            Step(HudrIcons.Plus, stringResource(Res.string.raise), settings.limit < Limit.MAX) {
                haptics?.press()
                onLimit(Limit.step(settings.limit, +1))
            }
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
fun HudrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(HudrTexts.noteLocation), stringResource(Res.string.note_estimate)),
        icon = { Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}
