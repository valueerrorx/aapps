package solutions.xapient.dividr.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.pressScale

// Gruen der Ausgeglichenheit (Hue ~126, Luecke zwischen moovr und tunr)
val DividrAccent = Color(0xFF6EE87A)
val Warn = Color(0xFFF2B84E)
val QrInk = Color(0xFF0B1014)

object DividrIcons {
    val Plus: ImageVector by lazy { line("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Close: ImageVector by lazy { line("close") { moveTo(7f, 7f); lineTo(17f, 17f); moveTo(17f, 7f); lineTo(7f, 17f) } }
    val Down: ImageVector by lazy { line("down") { moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f) } }
    val Left: ImageVector by lazy { line("left") { moveTo(14.5f, 6f); lineTo(8.5f, 12f); lineTo(14.5f, 18f) } }
    val Right: ImageVector by lazy { line("right") { moveTo(9.5f, 6f); lineTo(15.5f, 12f); lineTo(9.5f, 18f) } }
    val Arrow: ImageVector by lazy { line("arrow") { moveTo(5f, 12f); lineTo(19f, 12f); moveTo(14f, 7f); lineTo(19f, 12f); lineTo(14f, 17f) } }

    val Camera: ImageVector by lazy {
        line("camera") {
            moveTo(4f, 8.5f); lineTo(7.5f, 8.5f); lineTo(9f, 6f); lineTo(15f, 6f); lineTo(16.5f, 8.5f); lineTo(20f, 8.5f)
            lineTo(20f, 18.5f); lineTo(4f, 18.5f); close()
            circle(12f, 13.2f, 3.4f)
        }
    }

    // Suchrahmen-Ecken als Scan-Symbol
    val Scan: ImageVector by lazy {
        line("scan") {
            moveTo(4f, 9f); lineTo(4f, 4f); lineTo(9f, 4f)
            moveTo(15f, 4f); lineTo(20f, 4f); lineTo(20f, 9f)
            moveTo(20f, 15f); lineTo(20f, 20f); lineTo(15f, 20f)
            moveTo(9f, 20f); lineTo(4f, 20f); lineTo(4f, 15f)
            moveTo(8f, 12f); lineTo(16f, 12f)
        }
    }

    val People: ImageVector by lazy {
        line("people") {
            circle(9f, 8.5f, 3f)
            moveTo(3.5f, 19f); arcTo(5.5f, 5.5f, 0f, false, true, 14.5f, 19f)
            moveTo(15.5f, 5.8f); arcTo(3f, 3f, 0f, false, true, 15.5f, 11.2f)
            moveTo(17f, 14f); arcTo(5.5f, 5.5f, 0f, false, true, 20.5f, 19f)
        }
    }

    val Share: ImageVector by lazy {
        line("share") {
            circle(17f, 6f, 2.4f); circle(7f, 12f, 2.4f); circle(17f, 18f, 2.4f)
            moveTo(9.1f, 10.9f); lineTo(14.9f, 7.1f)
            moveTo(9.1f, 13.1f); lineTo(14.9f, 16.9f)
        }
    }

    val Trash: ImageVector by lazy {
        line("trash") {
            moveTo(5f, 7f); lineTo(19f, 7f)
            moveTo(9.5f, 7f); lineTo(9.5f, 4.5f); lineTo(14.5f, 4.5f); lineTo(14.5f, 7f)
            moveTo(6.5f, 7f); lineTo(7.5f, 19.5f); lineTo(16.5f, 19.5f); lineTo(17.5f, 7f)
        }
    }

    val Merge: ImageVector by lazy {
        line("merge") {
            moveTo(6f, 5f); lineTo(6f, 9f); quadTo(6f, 12f, 12f, 13f); lineTo(12f, 19f)
            moveTo(18f, 5f); lineTo(18f, 9f); quadTo(18f, 12f, 12f, 13f)
            moveTo(9f, 16.5f); lineTo(12f, 19.5f); lineTo(15f, 16.5f)
        }
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx + r, cy)
        arcTo(r, r, 0f, true, true, cx - r, cy)
        arcTo(r, r, 0f, true, true, cx + r, cy)
        close()
    }

    private fun line(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()
}

// Auswahl-Chip: Akzent-Kante wenn gewaehlt
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Aapps.colors.accent,
    role: Role = Role.Checkbox,
) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (selected) color.copy(alpha = 0.16f) else c.surface, Springs.gentle(), label = "chipBg")
    val border by animateColorAsState(if (selected) color else c.line, Springs.gentle(), label = "chipBorder")
    val fg by animateColorAsState(if (selected) color else c.fg, Springs.gentle(), label = "chipFg")
    Box(
        modifier
            .pressScale(pressed, 0.95f)
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .padding(vertical = 4.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(source, indication = null, role = role, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = Aapps.type.body.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Aapps.colors.fgMuted) {
    BasicText(text.uppercase(), modifier.padding(bottom = 8.dp), style = Aapps.type.label.copy(color = color))
}

// Eingabefeld als Pille, Platzhalter in schwach
@Composable
fun InputField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Aapps.type.body.copy(color = Aapps.colors.fg),
    keyboard: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
    onDone: () -> Unit = {},
) {
    val c = Aapps.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.semantics { contentDescription = placeholder },
        textStyle = style,
        singleLine = true,
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = keyboard,
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { inner ->
            Box(
                Modifier
                    .heightIn(min = 52.dp)
                    .background(c.surface, CircleShape)
                    .border(1.dp, c.line, CircleShape)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) BasicText(placeholder, style = style.copy(color = c.fgFaint), maxLines = 1)
                inner()
            }
        },
    )
}

// Kleine runde Aktion mit Symbol und Text (z. B. „Erledigt")
@Composable
fun SmallAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = Aapps.colors.accent) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(
        modifier
            .pressScale(pressed)
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            AappsIcon(icon, null, color, size = 18.dp)
            Spacer(Modifier.width(6.dp))
        }
        BasicText(text.uppercase(), style = Aapps.type.label.copy(color = color))
    }
}
