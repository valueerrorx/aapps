package com.valueerrorx.scorr.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.valueerrorx.aapps.core.ui.Aapps
import com.valueerrorx.aapps.core.ui.AappsIcon
import com.valueerrorx.aapps.core.ui.SettingsDivider
import com.valueerrorx.aapps.core.ui.SettingsGroup
import com.valueerrorx.aapps.core.ui.Springs
import com.valueerrorx.scorr.R
import com.valueerrorx.scorr.domain.Game

private val Warn = Color(0xFFF2B84E)

// Spieler bearbeiten: Name, Farbe, entfernen, hinzufuegen
@Composable
fun PlayersScreen(
    game: Game,
    onRename: (Int, String) -> Unit,
    onRecolor: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    openColors: Int = -1,
) {
    val c = Aapps.colors
    var colorsOf by rememberSaveable { mutableIntStateOf(openColors) }
    var confirm by rememberSaveable { mutableIntStateOf(-1) }
    ListScreen(stringResource(R.string.players), onBack) {
        SettingsGroup(stringResource(R.string.players_label, game.players.size, Game.MAX_PLAYERS)) {
            game.players.forEachIndexed { i, p ->
                if (i > 0) SettingsDivider()
                val color = playerColor(p.color)
                val shown = displayName(game, i)
                Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Farbpunkt oeffnet die Palette
                    val colorDesc = stringResource(R.string.color_named, shown)
                    Box(
                        Modifier
                            .size(48.dp)
                            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                                colorsOf = if (colorsOf == i) -1 else i
                            }
                            .semantics { contentDescription = colorDesc },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(26.dp).background(color, CircleShape)
                                .border(2.dp, if (colorsOf == i) c.fg else Color.Transparent, CircleShape),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    NameField(p.name, stringResource(R.string.player_n, i + 1), { onRename(i, it) }, Modifier.weight(1f))
                    if (game.players.size > 1) {
                        val armed = confirm == i
                        val tint by animateColorAsState(if (armed) Warn else c.fgMuted, Springs.gentle(), label = "rm")
                        RemoveButton(stringResource(R.string.remove_named, shown), tint) {
                            if (armed || !game.hasActions(i)) {
                                confirm = -1
                                if (colorsOf >= i) colorsOf = -1
                                onRemove(i)
                            } else {
                                confirm = i
                            }
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                }
                AnimatedVisibility(confirm == i, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    BasicText(
                        stringResource(R.string.remove_confirm, shown),
                        Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
                        style = Aapps.type.body.copy(color = Warn),
                    )
                }
                AnimatedVisibility(colorsOf == i, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Palette(p.color) { onRecolor(i, it) }
                }
            }
            if (game.players.size < Game.MAX_PLAYERS) {
                SettingsDivider()
                AddRow(stringResource(R.string.add_player)) { confirm = -1; onAdd() }
            }
        }
    }
}

// Farbwahl: alle 8 in einer Reihe, sonst gleichmaessig 2 x 4
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Palette(selectedColor: Int, onPick: (Int) -> Unit) {
    val c = Aapps.colors
    BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
        val perRow = if (maxWidth >= 48.dp * PlayerColors.size) PlayerColors.size else PlayerColors.size / 2
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, maxItemsInEachRow = perRow) {
            PlayerColors.indices.forEach { k ->
                val selected = k == selectedColor
                val desc = stringResource(R.string.color_n, k + 1)
                Box(
                    Modifier
                        .size(48.dp)
                        .selectable(selected, indication = null, interactionSource = null, role = Role.RadioButton) { onPick(k) }
                        .semantics { contentDescription = desc },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(if (selected) 34.dp else 28.dp)
                            .border(2.dp, if (selected) c.fg else Color.Transparent, CircleShape)
                            .padding(4.dp)
                            .background(PlayerColors[k], CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoveButton(desc: String, tint: Color, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier
            .size(48.dp)
            .background(if (pressed) Aapps.colors.surfaceHigh else Color.Transparent, CircleShape)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AappsIcon(ScorrIcons.Close, desc, tint, size = 22.dp)
    }
}

@Composable
private fun NameField(value: String, placeholder: String, onChange: (String) -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    val style = Aapps.type.body.copy(color = c.fg)
    BasicTextField(
        value = value,
        onValueChange = { onChange(Game.clean(it)) },
        modifier = modifier.semantics { contentDescription = placeholder },
        textStyle = style,
        singleLine = true,
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        decorationBox = { inner ->
            Box(Modifier.heightIn(min = 56.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) BasicText(placeholder, style = style.copy(color = c.fgFaint), maxLines = 1)
                inner()
            }
        },
    )
}

@Composable
private fun AddRow(text: String, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) c.surfaceHigh else Color.Transparent, Springs.gentle(), label = "addBg")
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        AappsIcon(ScorrIcons.Plus, null, c.accent, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        BasicText(text, style = Aapps.type.body.copy(color = c.accent))
    }
}
