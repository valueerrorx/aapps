package solutions.xapient.dividr.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.KeepScreenOn
import solutions.xapient.aapps.core.ui.glow
import solutions.xapient.dividr.R
import solutions.xapient.dividr.qr.QrImage
import solutions.xapient.dividr.qr.QrMatrix

// Volle Helligkeit, solange sichtbar
@Composable
private fun FullBrightness() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        val old = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = 1f }
        onDispose { window.attributes = window.attributes.apply { screenBrightness = old } }
    }
}

// Eigener Code im Vollbild, so gross wie moeglich; mehrere Teile nur als Notfall-Folge (4 Bilder/s)
@Composable
fun QrScreen(codes: List<QrMatrix>, hint: String, onBack: () -> Unit, animate: Boolean = true, part: Int = 0) {
    val c = Aapps.colors
    if (animate) {
        FullBrightness()
        KeepScreenOn()
    }
    var index by remember { mutableIntStateOf(part) }
    if (animate && codes.size > 1) {
        LaunchedEffect(codes) { while (true) { delay(250); index = (index + 1) % codes.size } }
    }
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    AappsScaffold {
        Box(Modifier.fillMaxSize()) {
            AappsIconButton(AappsIcons.Back, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_back), onBack, Modifier.padding(8.dp))
            val text: @Composable (Modifier) -> Unit = { m ->
                Column(m, horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(hint, style = Aapps.type.title.copy(color = c.fg, textAlign = TextAlign.Center))
                    if (codes.size > 1) {
                        Spacer(Modifier.height(12.dp))
                        Dots(codes.size, setOf(index % codes.size), current = index % codes.size)
                    }
                }
            }
            val code: @Composable (Modifier) -> Unit = { m ->
                BoxWithConstraints(m, contentAlignment = Alignment.Center) {
                    val side = minOf(maxWidth, maxHeight)
                    if (codes.isEmpty()) {
                        BasicText(stringResource(R.string.too_big), Modifier.padding(24.dp), style = Aapps.type.body.copy(color = c.fgMuted, textAlign = TextAlign.Center))
                    } else {
                        // weisse Karte = Ruhezone; Code fuellt sie
                        Box(
                            Modifier.size(side).glow(Color.White, side * 0.7f, 0.10f).background(Color.White, RoundedCornerShape(20.dp)).padding(6.dp),
                        ) {
                            QrImage(codes[index % codes.size], stringResource(R.string.my_code), Modifier.fillMaxSize(), corner = 14.dp)
                        }
                    }
                }
            }
            if (landscape) {
                Row(Modifier.fillMaxSize().padding(start = 64.dp, end = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    text(Modifier.weight(0.8f).padding(end = 16.dp))
                    code(Modifier.weight(1f).fillMaxSize())
                }
            } else {
                Column(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 64.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    code(Modifier.fillMaxWidth().weight(1f))
                    Spacer(Modifier.height(20.dp))
                    text(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun Dots(total: Int, have: Set<Int>, current: Int = -1) {
    val c = Aapps.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total.coerceAtMost(16)) { i ->
            val on = i in have
            Box(
                Modifier.size(if (i == current) 10.dp else 8.dp)
                    .background(if (on) c.accent else Color.Transparent, CircleShape)
                    .border(1.dp, if (on) c.accent else c.fgFaint, CircleShape),
            )
        }
    }
}

enum class CameraUi { ASK, BLOCKED, ON, ERROR }

// Kamera-Vollbild mit Suchrahmen; die Kamera kommt als Slot (Screenshots: Platzhalter)
@Composable
fun ScanScreen(
    camera: CameraUi,
    have: Set<Int>,
    total: Int,
    message: String?,
    onAllow: () -> Unit,
    onBack: () -> Unit,
    preview: @Composable () -> Unit,
) {
    val c = Aapps.colors
    if (camera == CameraUi.ON) KeepScreenOn()
    AappsScaffold(background = { if (camera == CameraUi.ON) preview() }) {
        Box(Modifier.fillMaxSize()) {
            if (camera == CameraUi.ON) {
                Canvas(Modifier.fillMaxSize()) {
                    val side = minOf(size.width, size.height) * 0.72f
                    val left = (size.width - side) / 2
                    val top = (size.height - side) / 2.4f
                    val arm = side * 0.14f
                    val w = 4.dp.toPx()
                    val corners = listOf(
                        Offset(left, top) to Offset(1f, 1f), Offset(left + side, top) to Offset(-1f, 1f),
                        Offset(left, top + side) to Offset(1f, -1f), Offset(left + side, top + side) to Offset(-1f, -1f),
                    )
                    corners.forEach { (p, d) ->
                        drawLine(c.accent, p, Offset(p.x + d.x * arm, p.y), w, StrokeCap.Round)
                        drawLine(c.accent, p, Offset(p.x, p.y + d.y * arm), w, StrokeCap.Round)
                    }
                }
            }
            AappsIconButton(AappsIcons.Back, stringResource(solutions.xapient.aapps.core.ui.R.string.aapps_back), onBack, Modifier.padding(8.dp).background(c.bg.copy(alpha = 0.6f), CircleShape))
            Column(
                Modifier.align(if (camera == CameraUi.ON) Alignment.BottomCenter else Alignment.Center).padding(20.dp).widthIn(max = 460.dp).fillMaxWidth()
                    .background(c.surface.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
                    .border(1.dp, c.line, RoundedCornerShape(24.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (camera) {
                    CameraUi.ON -> {
                        BasicText(
                            message ?: if (total > 1) stringResource(R.string.part_of, have.size, total) else stringResource(R.string.scan_aim),
                            style = Aapps.type.body.copy(color = if (message != null) Warn else c.fg, textAlign = TextAlign.Center),
                        )
                        if (total > 1) {
                            Spacer(Modifier.height(10.dp))
                            Dots(total, have)
                        }
                    }
                    else -> {
                        AappsIcon(DividrIcons.Camera, null, c.accent, size = 36.dp)
                        Spacer(Modifier.height(12.dp))
                        BasicText(
                            stringResource(
                                when (camera) {
                                    CameraUi.BLOCKED -> R.string.camera_blocked
                                    CameraUi.ERROR -> R.string.camera_error
                                    else -> R.string.camera_why
                                },
                            ),
                            style = Aapps.type.body.copy(color = c.fg, textAlign = TextAlign.Center),
                        )
                        if (camera != CameraUi.ERROR) {
                            Spacer(Modifier.height(16.dp))
                            AappsPillButton(
                                stringResource(if (camera == CameraUi.BLOCKED) R.string.open_settings else R.string.allow_camera),
                                onAllow, active = true,
                            )
                        }
                    }
                }
            }
        }
    }
}
