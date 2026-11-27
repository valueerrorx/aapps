package solutions.xapient.scorr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsAction
import solutions.xapient.aapps.core.ui.SettingsChoice
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.SettingsScreen
import solutions.xapient.aapps.core.ui.SettingsToggle
import solutions.xapient.scorr.res.*
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.aapps.core.ui.aapps_about
import solutions.xapient.scorr.APP_NAME
import solutions.xapient.scorr.domain.Board
import solutions.xapient.scorr.domain.Scoring
import solutions.xapient.scorr.domain.WinRule
import kotlinx.coroutines.delay

@Composable
fun ScorrSettings(
    playerNames: String,
    target: Int?,
    onTarget: (Int?) -> Unit,
    rule: WinRule,
    onRule: (WinRule) -> Unit,
    rounds: Boolean,
    onRounds: (Boolean) -> Unit,
    scoring: Scoring,
    onScoring: (Scoring) -> Unit,
    haptics: Boolean,
    onHaptics: (Boolean) -> Unit,
    keepOn: Boolean,
    onKeepOn: (Boolean) -> Unit,
    onPlayers: () -> Unit,
    onNewGame: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmNew by rememberSaveable { mutableStateOf(false) }
    // Bestaetigung verfaellt nach ein paar Sekunden
    if (confirmNew) LaunchedEffect(Unit) { delay(3000); confirmNew = false }
    SettingsScreen(onBack) {
        SettingsGroup(stringResource(Res.string.group_game)) {
            SettingsAction(stringResource(Res.string.players), playerNames, onClick = onPlayers)
            SettingsDivider()
            TargetRow(target, rounds && scoring == Scoring.WINS, onTarget)
            SettingsDivider()
            SettingsChoice(
                stringResource(Res.string.rule),
                listOf(WinRule.MOST to stringResource(Res.string.rule_most), WinRule.FEWEST to stringResource(Res.string.rule_fewest)),
                rule, onRule,
            )
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.rounds), rounds, onRounds, stringResource(Res.string.rounds_sub))
            // Wertung nur mit Runden
            if (rounds) {
                SettingsDivider()
                SettingsChoice(
                    stringResource(Res.string.scoring),
                    listOf(Scoring.WINS to stringResource(Res.string.scoring_wins), Scoring.POINTS to stringResource(Res.string.scoring_points)),
                    scoring, onScoring,
                )
            }
        }
        SettingsGroup(stringResource(Res.string.group_device)) {
            SettingsToggle(stringResource(Res.string.haptics), haptics, onHaptics)
            SettingsDivider()
            SettingsToggle(stringResource(Res.string.keep_on), keepOn, onKeepOn)
        }
        SettingsGroup(null) {
            SettingsAction(
                stringResource(if (confirmNew) Res.string.new_game_confirm else Res.string.new_game),
                stringResource(Res.string.new_game_sub),
            ) {
                if (confirmNew) {
                    confirmNew = false
                    onNewGame()
                } else {
                    confirmNew = true
                }
            }
            SettingsDivider()
            SettingsAction(stringResource(CoreRes.string.aapps_about), onClick = onAbout)
        }
    }
}

// Ziel als Zahlenfeld (Punkte oder Rundensiege); leer = kein Ziel
@Composable
private fun TargetRow(target: Int?, wins: Boolean, onTarget: (Int?) -> Unit) {
    val c = Aapps.colors
    var text by rememberSaveable { mutableStateOf(target?.toString() ?: "") }
    val title = stringResource(if (wins) Res.string.target_wins else Res.string.target)
    val off = stringResource(Res.string.target_off)
    val style = Aapps.type.number.copy(color = c.accent, textAlign = TextAlign.End, fontSize = Aapps.type.body.fontSize * 1.15f)
    val fs = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    // erst bei Fertig, Fokusverlust oder Verlassen uebernehmen, nicht bei jeder Ziffer
    val current = rememberUpdatedState(text)
    val commit = rememberUpdatedState {
        val t = Board.parseAmount(current.value)?.takeIf { it > 0 }
        if (t != target) onTarget(t)
    }
    DisposableEffect(Unit) { onDispose { commit.value() } }
    val focus = LocalFocusManager.current
    val texts: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            BasicText(title, style = Aapps.type.body.copy(color = c.fg))
            Spacer(Modifier.height(2.dp))
            BasicText(stringResource(if (wins) Res.string.target_wins_sub else Res.string.target_sub), style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f))
        }
    }
    val field: @Composable () -> Unit = {
        BasicTextField(
            value = text,
            onValueChange = { v -> text = v.filter { it.isDigit() }.take(6) },
            modifier = Modifier
                .width(92.dp * fs)
                .onFocusChanged { if (!it.isFocused) commit.value() }
                .background(c.bg, CircleShape)
                .semantics { contentDescription = title },
            textStyle = style,
            singleLine = true,
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit.value(); focus.clearFocus() }),
            decorationBox = { inner ->
                Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterEnd) {
                    if (text.isEmpty()) BasicText(off, style = style.copy(color = c.fgFaint))
                    inner()
                }
            },
        )
    }
    val pad = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
    // grosse Schrift: Feld unter dem Text, sonst bricht der Titel Wort fuer Wort um
    if (fs > 1.4f) {
        Column(pad) {
            texts(Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { field() }
        }
    } else {
        Row(pad, verticalAlignment = Alignment.CenterVertically) {
            texts(Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            field()
        }
    }
}

@Composable
fun ScorrAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = APP_NAME,
        versionName = versionName,
        tagline = stringResource(Res.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(Res.string.note_gestures)),
        icon = {
            Image(painterResource(Res.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}
