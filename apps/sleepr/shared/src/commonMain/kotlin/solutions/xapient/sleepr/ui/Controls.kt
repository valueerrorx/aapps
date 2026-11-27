package solutions.xapient.sleepr.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.vectorResource
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.Springs
import solutions.xapient.aapps.core.ui.pressScale
import solutions.xapient.sleepr.Track
import solutions.xapient.sleepr.res.Res
import solutions.xapient.sleepr.res.ic_wave
import kotlin.math.roundToInt

// Karte wie auf Android: Flaeche, 1 dp Linie, 14 dp Ecken, 16 dp Innenabstand
@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Aapps.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(14.dp))
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content,
    )
}

// Versal-Label ueber Gruppen
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    BasicText(text.uppercase(), modifier, style = Aapps.type.label.copy(color = Aapps.colors.fgMuted, fontSize = Aapps.type.label.fontSize * 0.92f))
}

/** Regler 0..max wie die SeekBar auf Android: Spur, Fortschritt und Knopf in der Farbe; meldet Anfassen und Loslassen */
@Composable
fun NoiseSlider(
    value: Int,
    color: Color,
    description: String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    max: Int = 100,
    onTouch: (Boolean) -> Unit = {},
    valueText: String = "$value",
) {
    val onValue by rememberUpdatedState(onChange)
    val touch by rememberUpdatedState(onTouch)
    val pad = 12.dp
    fun pick(x: Float, width: Int, padPx: Float): Int {
        val w = (width - 2 * padPx).coerceAtLeast(1f)
        return (((x - padPx) / w).coerceIn(0f, 1f) * max).roundToInt()
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .semantics {
                contentDescription = description
                stateDescription = valueText
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..max.toFloat(), steps = 0)
                setProgress { v -> onValue(v.roundToInt().coerceIn(0, max)); true }
            }
            .pointerInput(max) {
                // waagrecht ziehen stellt ein, senkrecht bleibt fuer das Scrollen; kurzes Tippen setzt den Wert
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { ch, _ -> ch.consume() }
                    if (drag != null) {
                        touch(true)
                        onValue(pick(drag.position.x, size.width, pad.toPx()))
                        horizontalDrag(drag.id) { ch ->
                            onValue(pick(ch.position.x, size.width, pad.toPx()))
                            ch.consume()
                        }
                        touch(false)
                    } else {
                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                        if (up != null && !up.pressed && !up.isConsumed) {
                            touch(true)
                            onValue(pick(down.position.x, size.width, pad.toPx()))
                            touch(false)
                        }
                    }
                }
            },
    ) {
        val p = pad.toPx()
        val w = size.width - 2 * p
        val cy = size.height / 2
        val th = 4.dp.toPx()
        val f = value.toFloat() / max
        drawRoundRect(Track, Offset(p, cy - th / 2), Size(w, th), CornerRadius(th / 2))
        drawRoundRect(color, Offset(p, cy - th / 2), Size(w * f, th), CornerRadius(th / 2))
        drawCircle(color, 8.dp.toPx(), Offset(p + w * f, cy))
    }
}

/** Auswahl-Chip, gefuellt mit Akzent, wenn aktiv */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, description: String? = null) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (selected) c.accent else c.surface, Springs.gentle(), label = "chip")
    val fg by animateColorAsState(if (selected) c.onAccent else c.fg, Springs.gentle(), label = "chipFg")
    BasicText(
        text,
        Modifier
            .pressScale(pressed)
            .background(bg, CircleShape)
            .border(1.dp, if (selected) c.accent else c.line, CircleShape)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .semantics {
                this.selected = selected
                if (description != null) contentDescription = description
            }
            .heightIn(min = 40.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        style = Aapps.type.body.copy(color = fg, fontSize = Aapps.type.body.fontSize * 0.93f),
    )
}

@Composable
fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/** Wellen-Schalter neben einer Rauschfarbe: gefuellt in der Farbe, wenn an */
@Composable
fun WaveButton(on: Boolean, color: Color, description: String, onClick: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (on) color else c.surface, Springs.gentle(), label = "wave")
    Box(
        Modifier
            .padding(start = 6.dp)
            .size(width = 40.dp, height = 32.dp)
            .graphicsLayer { val s = if (pressed) 0.9f else 1f; scaleX = s; scaleY = s }
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, if (on) color else c.line, RoundedCornerShape(10.dp))
            .clickable(source, indication = null, onClick = onClick)
            .semantics { role = Role.Button; contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        AappsIcon(vectorResource(Res.drawable.ic_wave), null, if (on) c.bg else c.fgMuted, size = 22.dp)
    }
}
