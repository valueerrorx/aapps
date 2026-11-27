package solutions.xapient.dividr.ui

import solutions.xapient.dividr.APP_NAME
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.dividr.res.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.Money
import solutions.xapient.dividr.domain.activeMembers
import solutions.xapient.dividr.domain.canChangeCurrency

@Composable
fun DividrSettings(
    group: Group,
    haptics: Boolean,
    onHaptics: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onCurrency: (String) -> Unit,
    onPeople: () -> Unit,
    onGroups: () -> Unit,
    onLeave: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmLeave by remember { mutableStateOf(false) }
    if (confirmLeave) LaunchedEffect(Unit) { delay(3000); confirmLeave = false }
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(Res.string.group)) {
            var name by remember(group.name) { mutableStateOf(group.name) }
            val current = rememberUpdatedState(name)
            DisposableEffect(Unit) { onDispose { if (current.value.isNotBlank() && current.value.trim() != group.name) onRename(current.value) } }
            InputField(
                name, { name = Group.cleanTitle(it) }, stringResource(Res.string.group_name),
                Modifier.fillMaxWidth().padding(12.dp),
                keyboard = KeyboardOptions(imeAction = ImeAction.Done),
                onDone = { if (name.isNotBlank()) onRename(name) },
            )
            SettingsDivider()
            val n = group.activeMembers().size
            SettingsAction(stringResource(Res.string.people), pluralStringResource(Res.plurals.n_people, n, n), onClick = onPeople)
            if (group.canChangeCurrency()) {
                SettingsDivider()
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf(group.currency) + Money.COMMON).distinct().forEach { code ->
                        Chip(code, code == group.currency, { onCurrency(code) }, role = Role.RadioButton)
                    }
                }
            }
        }
        SettingsGroup(stringResource(Res.string.device)) {
            SettingsToggle(stringResource(Res.string.haptics), haptics, onHaptics)
        }
        SettingsGroup(null) {
            SettingsAction(stringResource(Res.string.groups), stringResource(Res.string.groups_sub), onClick = onGroups)
            SettingsDivider()
            SettingsAction(
                stringResource(if (confirmLeave) Res.string.leave_confirm else Res.string.leave),
                stringResource(Res.string.leave_sub),
            ) {
                if (confirmLeave) onLeave() else confirmLeave = true
            }
            SettingsDivider()
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

@Composable
fun DividrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(Res.string.note_sync), stringResource(Res.string.note_privacy)),
        icon = { Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp)) },
    )
}
