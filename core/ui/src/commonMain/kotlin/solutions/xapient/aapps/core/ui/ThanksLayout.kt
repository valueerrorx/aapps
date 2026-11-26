package solutions.xapient.aapps.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Ein Versprechen auf dem Kauf-Bildschirm; [onClick] macht die Zeile antippbar (z. B. Quellcode-Link) */
data class ThanksPoint(val icon: ImageVector, val text: String, val subtitle: String? = null, val onClick: (() -> Unit)? = null)

// Aufbau des Dankes-/Angebots-Bildschirms, Texte liefert die Plattform (Google Play bzw. App Store)
@Composable
fun ThanksLayout(
    appName: String,
    title: String,
    text: String,
    points: List<ThanksPoint>,
    primary: @Composable () -> Unit,
    hint: String?,
    secondary: @Composable () -> Unit,
    footer: String,
    // kleiner Kopf und engere Abstaende, damit auch auf kleinen iPhones alles ohne Scrollen passt
    compact: Boolean = false,
) {
    val c = Aapps.colors
    val gap = if (compact) 0.6f else 1f
    AappsScaffold {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp * gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hero: Schloss im Akzent-Halo
            Box(
                Modifier.size(if (compact) 72.dp else 112.dp).glow(c.accent, if (compact) 48.dp else 72.dp, 0.3f)
                    .background(c.surface, CircleShape).border(1.5.dp, c.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AappsIcon(AappsIcons.Unlock, null, c.accent, size = if (compact) 32.dp else 48.dp)
            }
            Spacer(Modifier.height(24.dp * gap))
            BasicText(appName.uppercase(), style = Aapps.type.label.copy(color = c.accent))
            Spacer(Modifier.height(6.dp))
            BasicText(
                title,
                Modifier.semantics { heading() },
                style = Aapps.type.headline.copy(color = c.fg, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(14.dp * gap))
            BasicText(
                text,
                Modifier.widthIn(max = 480.dp),
                style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(24.dp * gap))
            Box(Modifier.widthIn(max = 480.dp)) {
                SettingsGroup(null) {
                    points.forEach { Point(it) }
                }
            }
            Spacer(Modifier.height(28.dp * gap))
            primary()
            if (hint != null) {
                Spacer(Modifier.height(14.dp * gap))
                BasicText(
                    hint,
                    Modifier.widthIn(max = 480.dp),
                    style = Aapps.type.body.copy(color = c.fg, textAlign = TextAlign.Center),
                )
            }
            Spacer(Modifier.height(8.dp))
            secondary()
            Spacer(Modifier.height(12.dp * gap))
            BasicText(
                footer,
                Modifier.widthIn(max = 480.dp),
                style = Aapps.type.body.copy(
                    color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f, textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun Point(p: ThanksPoint) {
    val c = Aapps.colors
    val click = p.onClick
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (click != null) Modifier.clickable(role = Role.Button, onClick = click) else Modifier)
            .heightIn(min = 48.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).background(c.accent.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            AappsIcon(p.icon, null, c.accent, size = 18.dp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(p.text, style = Aapps.type.body.copy(color = c.fg))
            if (p.subtitle != null) {
                BasicText(p.subtitle, style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f))
            }
        }
        // antippbar: Pfeil wie in den Einstellungen
        if (click != null) AappsIcon(AappsIcons.Chevron, null, c.fgMuted, size = 20.dp)
    }
}
