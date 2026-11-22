package solutions.xapient.hertzr.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsTextButton
import solutions.xapient.aapps.core.ui.glow
import solutions.xapient.hertzr.R
import solutions.xapient.hertzr.domain.Freq

// Direkte Eingabe: "440", "1,5k", "12 kHz"; Werte ausserhalb werden auf den Bereich begrenzt
@Composable
fun FreqInput(
    title: String,
    current: Double,
    maxHz: Double,
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit,
    initial: String = "",
    focus: Boolean = true,
) {
    val c = Aapps.colors
    val dec = stringResource(R.string.decimal_sep).first()
    var text by rememberSaveable { mutableStateOf(initial) }
    val parsed = Freq.parse(text)?.let { Freq.quantize(Freq.clamp(it, maxHz)) }
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val requester = remember { FocusRequester() }
    if (focus) LaunchedEffect(Unit) { requester.requestFocus() }
    val apply = { if (parsed != null) onApply(parsed) }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg.copy(alpha = 0.92f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .imePadding(),
        contentAlignment = if (landscape) Alignment.TopCenter else Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .glow(c.accent, 220.dp, 0.10f)
                .background(c.surface, RoundedCornerShape(28.dp))
                .border(1.dp, c.line, RoundedCornerShape(28.dp))
                // Tippen in der Karte schliesst nicht
                .pointerInput(Unit) { detectTapGestures {} }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(title, style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center), maxLines = 2)
            Spacer(Modifier.height(if (landscape) 6.dp else 14.dp))
            val hint = stringResource(R.string.enter_frequency)
            val style = Aapps.type.display.copy(color = c.accent, fontSize = 60.sp, textAlign = TextAlign.Center)
            BasicTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' || it == ',' || it == 'k' || it == 'K' }.take(9) },
                modifier = Modifier.fillMaxWidth().focusRequester(requester).semantics { contentDescription = hint },
                textStyle = style,
                singleLine = true,
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { apply() }),
                decorationBox = { inner ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().heightIn(min = 76.dp), contentAlignment = Alignment.Center) {
                            if (text.isEmpty()) BasicText(Freq.format(current, dec), style = style.copy(color = c.fgFaint))
                            inner()
                        }
                        Box(Modifier.fillMaxWidth(0.6f).height(2.dp).background(c.accent.copy(alpha = 0.5f), CircleShape))
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            // Vorschau des Ergebnisses oder der erlaubte Bereich
            BasicText(
                if (parsed != null) "= ${Freq.short(parsed, dec)}" else stringResource(R.string.input_range, Freq.short(Freq.MIN, dec), Freq.short(maxHz, dec)),
                style = Aapps.type.number.copy(color = c.fgMuted, fontSize = 16.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 16.sp, stepSize = 0.5.sp),
            )
            BasicText(
                stringResource(R.string.input_hint),
                style = Aapps.type.body.copy(color = c.fgFaint, fontSize = 13.sp, textAlign = TextAlign.Center),
                maxLines = 2,
            )
            Spacer(Modifier.height(if (landscape) 10.dp else 18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                AappsTextButton(stringResource(R.string.cancel), onDismiss)
                Spacer(Modifier.width(8.dp))
                AappsPillButton(stringResource(R.string.set), apply, active = parsed != null, enabled = parsed != null)
            }
        }
    }
}
