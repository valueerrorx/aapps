package solutions.xapient.aapps.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Vollflaechig hinter den Systemleisten, Inhalt innerhalb der sicheren Zone
@Composable
fun AappsScaffold(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(Aapps.colors.bg)) {
        background()
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), content = content)
    }
}

@Composable
fun AappsTopBar(
    title: String?,
    onBack: (() -> Unit)?,
    backDescription: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            AappsIconButton(AappsIcons.Back, backDescription, onBack)
        } else {
            Spacer(Modifier.width(8.dp))
        }
        if (title != null) {
            BasicText(
                title,
                Modifier.weight(1f).padding(horizontal = 8.dp),
                style = Aapps.type.title.copy(color = Aapps.colors.fg),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        actions()
    }
}

// Leichtes Eindruecken per Feder beim Tippen
fun Modifier.pressScale(pressed: Boolean, scale: Float = 0.94f): Modifier = graphicsLayer {
    val s = if (pressed) scale else 1f
    scaleX = s; scaleY = s
}

@Composable
fun AappsIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, Springs.bouncy(), label = "iconScale")
    val c = Aapps.colors
    Box(
        modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(if (pressed) c.surfaceHigh else Color.Transparent, CircleShape)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AappsIcon(icon, contentDescription, c.fgMuted, size = 24.dp)
    }
}

// Die eine Hauptaktion: Pille in Akzentfarbe, gefuellt wenn aktiv
@Composable
fun AappsPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, Springs.bouncy(), label = "pillScale")
    val fill by animateColorAsState(if (active) c.accent else c.surface, Springs.gentle(), label = "pillFill")
    val fg by animateColorAsState(
        when {
            !enabled -> c.fgFaint
            active -> c.onAccent
            else -> c.fg
        },
        Springs.gentle(), label = "pillFg",
    )
    val border by animateColorAsState(if (active) c.accent else c.line, Springs.gentle(), label = "pillBorder")
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .glow(c.accent, 72.dp, if (active) 0.28f else 0f)
            .heightIn(min = 52.dp)
            .background(fill, CircleShape)
            .border(1.dp, border, CircleShape)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            // Icon waechst mit der Schriftgroesse mit
            AappsIcon(icon, null, fg, size = 20.dp * LocalDensity.current.fontScale.coerceAtMost(2f))
            Spacer(Modifier.width(10.dp))
        }
        // Labels in Versalien laut DESIGN.md
        BasicText(text.uppercase(), style = Aapps.type.label.copy(color = fg, fontSize = Aapps.type.label.fontSize * 1.15f))
    }
}

// Zurueckhaltender Text-Knopf fuer Nebenaktionen
@Composable
fun AappsTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    BasicText(
        text.uppercase(),
        modifier
            .pressScale(pressed)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        style = Aapps.type.label.copy(color = if (pressed) Aapps.colors.fg else Aapps.colors.fgMuted, fontSize = Aapps.type.label.fontSize * 1.15f),
    )
}
