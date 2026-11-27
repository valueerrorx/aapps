package solutions.xapient.scorr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsTextButton
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.glow
import solutions.xapient.scorr.res.*
import solutions.xapient.scorr.domain.Board

// Zahleneingabe ueber der Tafel; Akzent = Spielerfarbe
@Composable
fun EntryOverlay(
    name: String,
    color: Color,
    score: Int,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit,
    initial: String = "",
    focus: Boolean = true,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val amount = Board.parseAmount(text) ?: 0
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    // nebeneinander nur, wenn beide Knoepfe sicher einzeilig bleiben
    val stacked = !landscape || LocalDensity.current.fontScale > 1.3f
    val requester = remember { FocusRequester() }
    if (focus) LaunchedEffect(Unit) { requester.requestFocus() }

    AappsTheme(color) {
        val c = Aapps.colors
        Box(
            Modifier
                .fillMaxSize()
                .background(c.bg.copy(alpha = 0.9f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = if (landscape) Alignment.TopCenter else Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .glow(color, 220.dp, 0.10f)
                    .background(c.surface, RoundedCornerShape(28.dp))
                    .border(1.dp, c.line, RoundedCornerShape(28.dp))
                    // Tippen in der Karte schliesst nicht; kein Klick-Knoten fuer TalkBack
                    .pointerInput(Unit) { detectTapGestures {} }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(color, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    BasicText(
                        name, Modifier.weight(1f),
                        style = Aapps.type.title.copy(color = c.fg), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(Board.format(score), style = Aapps.type.number.copy(color = c.fgMuted))
                    // Querformat: Schliessen oben statt unten, sonst reicht die Hoehe nicht
                    if (landscape) {
                        Spacer(Modifier.width(4.dp))
                        AappsIconButton(ScorrIcons.Close, stringResource(Res.string.cancel), onDismiss)
                    }
                }
                Spacer(Modifier.height(if (landscape) 8.dp else 18.dp))
                val hint = stringResource(Res.string.entry_hint)
                val style = Aapps.type.display.copy(color = color, fontSize = 72.sp, textAlign = TextAlign.Center)
                BasicTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }.take(6) },
                    modifier = Modifier.fillMaxWidth().focusRequester(requester).semantics { contentDescription = hint },
                    textStyle = style,
                    singleLine = true,
                    cursorBrush = SolidColor(color),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (amount > 0) onApply(amount) }),
                    decorationBox = { inner ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().heightIn(min = 88.dp), contentAlignment = Alignment.Center) {
                                if (text.isEmpty()) BasicText("0", style = style.copy(color = c.fgFaint))
                                inner()
                            }
                            Box(Modifier.fillMaxWidth(0.6f).height(2.dp).background(color.copy(alpha = 0.5f), CircleShape))
                        }
                    },
                )
                Spacer(Modifier.height(if (landscape) 12.dp else 22.dp))
                val sub: @Composable (Modifier) -> Unit = { m ->
                    AappsPillButton(
                        stringResource(Res.string.subtract), { onApply(-amount) }, m,
                        icon = ScorrIcons.Minus, enabled = amount > 0,
                    )
                }
                val add: @Composable (Modifier) -> Unit = { m ->
                    AappsPillButton(
                        stringResource(Res.string.add), { onApply(amount) }, m,
                        icon = ScorrIcons.Plus, active = amount > 0, enabled = amount > 0,
                    )
                }
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        add(Modifier.fillMaxWidth())
                        sub(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        sub(Modifier.weight(1f))
                        add(Modifier.weight(1f))
                    }
                }
                if (!landscape) {
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AappsTextButton(stringResource(Res.string.cancel), onDismiss)
                    }
                }
            }
        }
    }
}
