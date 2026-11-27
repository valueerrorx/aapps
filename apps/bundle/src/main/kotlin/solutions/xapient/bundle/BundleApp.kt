package solutions.xapient.bundle

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.AappsTopBar
import solutions.xapient.aapps.core.ui.AboutScreen
import solutions.xapient.aapps.core.ui.SettingsDivider
import solutions.xapient.aapps.core.ui.SettingsGroup
import solutions.xapient.aapps.core.ui.glow
import solutions.xapient.aapps.core.ui.pressScale

val BundleAccent = Color(0xFFF3C98B)

private enum class Screen { HOME, ABOUT }

@Composable
fun BundleApp() {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var installed by remember { mutableStateOf(installedApps(context)) }
    // nach Rueckkehr aus Google Play neu pruefen
    LifecycleResumeEffect(Unit) {
        installed = installedApps(context)
        onPauseOrDispose { }
    }
    AappsTheme(BundleAccent) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                installed = installed,
                onOpen = { openApp(context, it) },
                onInstall = { openStore(context, it) },
                onAbout = { screen = Screen.ABOUT },
            )
            Screen.ABOUT -> {
                BackHandler { screen = Screen.HOME }
                BundleAbout(BuildConfig.VERSION_NAME) { screen = Screen.HOME }
            }
        }
    }
}

/** Uebersicht: Schluessel-Kopf, installierte Apps, weitere Apps */
@Composable
fun HomeScreen(
    installed: Set<String>,
    onOpen: (XApp) -> Unit,
    onInstall: (XApp) -> Unit,
    onAbout: () -> Unit,
    scroll: ScrollState = rememberScrollState(),
) {
    val c = Aapps.colors
    val mine = Catalog.filter { it.id in installed }
    val more = Catalog.filter { it.id !in installed }
    AappsScaffold {
        Column(Modifier.fillMaxWidth()) {
            AappsTopBar(null, null, "") {
                AappsIconButton(InfoIcon, stringResource(R.string.about), onAbout)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 560.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Hero: Schluessel im Akzent-Halo
                    Image(
                        painterResource(R.drawable.ic_launcher_about),
                        null,
                        Modifier.size(96.dp).glow(c.accent, 64.dp, 0.22f),
                    )
                    Spacer(Modifier.height(20.dp))
                    BasicText(stringResource(R.string.app_name).uppercase(), style = Aapps.type.label.copy(color = c.accent))
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        stringResource(R.string.headline),
                        Modifier.semantics { heading() },
                        style = Aapps.type.headline.copy(color = c.fg, textAlign = TextAlign.Center),
                    )
                    Spacer(Modifier.height(12.dp))
                    BasicText(
                        stringResource(R.string.intro),
                        Modifier.widthIn(max = 420.dp),
                        style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center),
                    )
                    Spacer(Modifier.height(18.dp))
                    BasicText(
                        stringResource(R.string.installed_count, mine.size, Catalog.size),
                        Modifier
                            .background(c.surface, CircleShape)
                            .border(1.dp, c.line, CircleShape)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = Aapps.type.number.copy(color = c.accent, fontSize = Aapps.type.label.fontSize * 1.15f),
                    )
                    Spacer(Modifier.height(28.dp))
                    if (mine.isNotEmpty()) {
                        AppGroup(stringResource(R.string.group_installed), mine, true, onOpen)
                        Spacer(Modifier.height(20.dp))
                    }
                    if (more.isNotEmpty()) AppGroup(stringResource(R.string.group_more), more, false, onInstall)
                }
            }
        }
    }
}

@Composable
private fun AppGroup(label: String, apps: List<XApp>, installed: Boolean, onAction: (XApp) -> Unit) {
    SettingsGroup(label) {
        apps.forEachIndexed { i, app ->
            if (i > 0) SettingsDivider()
            AppRow(app, installed) { onAction(app) }
        }
    }
}

@Composable
private fun AppRow(app: XApp, installed: Boolean, onAction: () -> Unit) {
    val c = Aapps.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val action = stringResource(if (installed) R.string.open else R.string.install)
    val desc = stringResource(if (installed) R.string.open_desc else R.string.install_desc, app.id)
    val status = when {
        app.free -> R.string.status_free
        installed -> R.string.status_unlocked
        else -> R.string.status_included
    }
    val big = LocalDensity.current.fontScale >= 1.5f
    // kleine Pille in der Farbe der jeweiligen App
    val pill = @Composable {
        BasicText(
            action.uppercase(),
            Modifier
                .pressScale(pressed)
                .background(app.accent.copy(alpha = if (installed) 0.16f else 0f), CircleShape)
                .border(1.dp, app.accent.copy(alpha = if (installed) 0.9f else 0.55f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            style = Aapps.type.label.copy(color = app.accent),
            maxLines = 1,
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (pressed) c.surfaceHigh else Color.Transparent)
            .clickable(source, indication = null, role = Role.Button, onClick = onAction)
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .heightIn(min = 76.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(app.icon),
            null,
            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, c.line, RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            BasicText(app.id, style = Aapps.type.title.copy(color = c.fg))
            BasicText(
                stringResource(app.line),
                style = Aapps.type.body.copy(color = c.fgMuted, fontSize = Aapps.type.label.fontSize * 1.1f, lineHeight = 18.sp),
                maxLines = if (big) 4 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (installed && !app.free) {
                    AappsIcon(AappsIcons.Check, null, c.accent, size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                }
                BasicText(
                    stringResource(status).uppercase(),
                    style = Aapps.type.label.copy(color = if (installed && !app.free) c.accent else c.fgFaint),
                )
            }
            if (big) {
                Spacer(Modifier.height(10.dp))
                pill()
            }
        }
        // grosse Schrift: Pille unter den Text, sonst rechts daneben
        if (!big) {
            Spacer(Modifier.width(12.dp))
            pill()
        }
    }
}

@Composable
fun BundleAbout(versionName: String, onBack: () -> Unit) {
    AboutScreen(
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        tagline = stringResource(R.string.tagline),
        onBack = onBack,
        notes = listOf(stringResource(R.string.note_check), stringResource(R.string.note_device)),
        icon = {
            Image(painterResource(R.drawable.ic_launcher_about), null, Modifier.size(88.dp))
        },
    )
}

// Info-Symbol im Linienstil von AappsIcons
private val InfoIcon: ImageVector by lazy {
    ImageVector.Builder("info", 24.dp, 24.dp, 24f, 24f).path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(21f, 12f)
        arcTo(9f, 9f, 0f, true, true, 3f, 12f)
        arcTo(9f, 9f, 0f, true, true, 21f, 12f)
        close()
        moveTo(12f, 11f); lineTo(12f, 16.5f)
        moveTo(12f, 7.6f); lineTo(12f, 7.7f)
    }.build()
}
