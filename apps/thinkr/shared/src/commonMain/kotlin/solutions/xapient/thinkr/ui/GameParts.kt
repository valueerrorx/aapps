package solutions.xapient.thinkr.ui

import solutions.xapient.thinkr.res.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.Springs

// Einzaehlen ueber einem Spielfeld: Frage auf eigenem Grund, darunter die Zahl
@Composable
fun CountOverlay(prompt: String, count: Int) {
    val c = Aapps.colors
    Box(Modifier.fillMaxSize().background(c.bg.copy(alpha = 0.62f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(24.dp)
                .background(c.bg.copy(alpha = 0.92f), RoundedCornerShape(28.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(prompt, style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center), maxLines = 4)
            Spacer(Modifier.height(12.dp))
            BasicText(count.toString(), style = Aapps.type.display.copy(color = c.accent, fontSize = Aapps.type.display.fontSize * 0.8f))
        }
    }
}

// Frist als Linie, die zur Mitte schrumpft; knapp wird sie Bernstein. fraction = verbleibender Anteil
@Composable
fun DeadlineBar(fraction: Float, visible: Boolean, modifier: Modifier) {
    val c = Aapps.colors
    val f = if (visible) fraction.coerceIn(0f, 1f) else 0f
    val alpha by animateFloatAsState(if (visible) 1f else 0f, Springs.gentle(), label = "deadline")
    Canvas(modifier.height(6.dp).graphicsLayer { this.alpha = alpha }) {
        val y = size.height / 2
        val w = size.width * f
        val x0 = (size.width - w) / 2
        drawLine(c.line, Offset(0f, y), Offset(size.width, y), 3.dp.toPx(), StrokeCap.Round)
        if (w > 0f) drawLine(if (f < 0.3f) Amber else c.accent, Offset(x0, y), Offset(x0 + w, y), 3.dp.toPx(), StrokeCap.Round)
    }
}

// Antwort-Taste Ja/Nein: loest beim Aufsetzen aus (Reaktionszeit), Glas mit kurzem Aufleuchten
@Composable
fun AnswerButton(yes: Boolean, enabled: Boolean, flash: Float, flashColor: Color, onClick: () -> Unit, modifier: Modifier) {
    val c = Aapps.colors
    var pressed by remember { mutableStateOf(false) }
    val click by rememberUpdatedState(onClick)
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, Springs.bouncy(), label = "answerScale")
    val dim by animateFloatAsState(if (enabled) 1f else 0.4f, Springs.gentle(), label = "answerDim")
    val label = stringResource(if (yes) Res.string.yes else Res.string.no)
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = dim }
            .semantics {
                role = Role.Button
                contentDescription = label
                if (enabled) onClick { click(); true }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    click()
                    pressed = true
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        pressed = false
                    }
                }
            }
            .drawBehind {
                val r = 26.dp.toPx()
                drawGlass(Offset.Zero, size, r, if (flash > 0.01f) flashColor.copy(alpha = 0.3f + 0.7f * flash) else c.line)
                if (flash > 0.01f) {
                    drawRoundRect(flashColor.copy(alpha = 0.16f * flash), cornerRadius = CornerRadius(r, r))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
            AappsIcon(if (yes) ThinkrIcons.Yes else ThinkrIcons.No, null, if (yes) c.accent else c.fg, size = 30.dp)
            Spacer(Modifier.height(6.dp))
            BasicText(label.uppercase(), style = Aapps.type.label.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f))
        }
    }
}
