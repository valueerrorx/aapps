package solutions.xapient.sleepr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIconButton
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.aapps_settings
import solutions.xapient.aapps.core.ui.Res as CoreRes
import solutions.xapient.sleepr.APP_NAME
import solutions.xapient.sleepr.COLORS
import solutions.xapient.sleepr.Display
import solutions.xapient.sleepr.Mix
import solutions.xapient.sleepr.NoisePlayer
import solutions.xapient.sleepr.describe
import solutions.xapient.sleepr.fmtHz
import solutions.xapient.sleepr.fmtTime
import solutions.xapient.sleepr.presetName
import solutions.xapient.sleepr.res.Res
import solutions.xapient.sleepr.res.colors
import solutions.xapient.sleepr.res.group_mixes
import solutions.xapient.sleepr.res.group_timer
import solutions.xapient.sleepr.res.ic_play
import solutions.xapient.sleepr.res.ic_stop
import solutions.xapient.sleepr.res.spectrum
import solutions.xapient.sleepr.res.spectrum_unit
import solutions.xapient.sleepr.res.start
import solutions.xapient.sleepr.res.state_fading
import solutions.xapient.sleepr.res.state_left
import solutions.xapient.sleepr.res.state_playing
import solutions.xapient.sleepr.res.state_ready
import solutions.xapient.sleepr.res.stop
import solutions.xapient.sleepr.res.timer_chip
import solutions.xapient.sleepr.res.timer_minutes
import solutions.xapient.sleepr.res.timer_off
import solutions.xapient.sleepr.res.treble
import solutions.xapient.sleepr.res.volume
import solutions.xapient.sleepr.res.wave_length
import solutions.xapient.sleepr.res.waves_off
import solutions.xapient.sleepr.res.waves_on
import solutions.xapient.sleepr.res.with_timer
import solutions.xapient.sleepr.res.all_zero

private val Mono = FontFamily.Monospace

@Composable
private fun Txt(
    s: String,
    size: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    mono: Boolean = false,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        s,
        modifier,
        style = Aapps.type.body.copy(
            color = color, fontSize = size, lineHeight = size * 1.3f,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = if (mono) Mono else Aapps.type.body.fontFamily,
            textAlign = align,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Mixer wie auf Android: Kopfzeile, Spektrum, Mischungen, Rauschfarben, Lautstaerke/Hoehen, Timer,
 * Status und Start/Stop unten (Hochformat) bzw. links (Querformat).
 */
@Composable
fun MixerScreen(
    mix: Mix,
    display: Display,
    player: NoisePlayer,
    onPress: () -> Unit,
    onSettings: () -> Unit,
    scrollToEnd: Boolean = false,
) {
    val c = Aapps.colors
    // Takt fuer Restzeit und mitlaufende Wellen (wie der 100-ms-Takt auf Android)
    var playing by remember { mutableStateOf(player.playing) }
    var remaining by remember { mutableStateOf(-1L) }
    var fading by remember { mutableStateOf(false) }
    var live by remember { mutableStateOf<DoubleArray?>(null) }
    LaunchedEffect(player) {
        while (true) {
            val r = player.renderer
            playing = player.playing
            remaining = if (playing && r != null) r.remainingMs() else -1L
            fading = playing && r != null && r.fading()
            live = if (playing && r != null) r.live else null
            delay(100)
        }
    }
    var touching by remember { mutableStateOf(-1) }

    fun changed() {
        mix.changed()
        player.update()
    }

    val status = when {
        !playing -> stringResource(Res.string.state_ready)
        remaining >= 0 -> stringResource(if (fading) Res.string.state_fading else Res.string.state_left, fmtTime(remaining))
        else -> stringResource(Res.string.state_playing)
    }
    val sum = describe(mix)
    LaunchedEffect(sum) { player.describe(sum) }
    val sub = if (mix.timer > 0 && !playing) stringResource(Res.string.with_timer, sum, mix.timer) else sum

    val bar: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(status, 17.sp, c.fg, bold = true, align = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Txt(sub, 13.sp, c.fgMuted, align = TextAlign.Center, maxLines = 2)
            Spacer(Modifier.height(14.dp))
            val ready = !playing
            AappsPillButton(
                text = stringResource(if (ready) Res.string.start else Res.string.stop),
                onClick = {
                    onPress()
                    if (player.playing) player.stop() else player.play()
                    playing = player.playing
                },
                modifier = Modifier.widthIn(min = 176.dp),
                icon = vectorResource(if (ready) Res.drawable.ic_play else Res.drawable.ic_stop),
                active = ready,
            )
        }
    }

    val content: @Composable (Boolean) -> Unit = { land ->
        val scroll = rememberScrollState()
        LaunchedEffect(scrollToEnd, scroll.maxValue) { if (scrollToEnd) scroll.scrollTo(scroll.maxValue) }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(start = 18.dp, end = if (land) 24.dp else 18.dp, bottom = 24.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(stringResource(Res.string.spectrum), Modifier.weight(1f))
                    Txt(stringResource(Res.string.spectrum_unit), 11.sp, c.fgMuted)
                }
                Spacer(Modifier.height(8.dp))
                Spectrum(IntArray(5) { mix.levels[it] }, mix.tone, stringResource(Res.string.all_zero), sum, Modifier.fillMaxWidth().height(150.dp))
            }
            if (display.showMixes) {
                Spacer(Modifier.height(22.dp))
                SectionLabel(stringResource(Res.string.group_mixes))
                Spacer(Modifier.height(8.dp))
                ChipRow {
                    Mix.PRESETS.forEach { p ->
                        Chip(stringResource(presetName(p)), false, { onPress(); mix.applyPreset(p); player.update() })
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Card {
                SectionLabel(stringResource(Res.string.colors))
                COLORS.forEachIndexed { i, col ->
                    val name = stringResource(col.name)
                    val liveLevel = live?.let { if (mix.waves[i] && mix.levels[i] > 0 && touching != i) it[i].toInt() else null }
                    Spacer(Modifier.height(if (i == 0) 10.dp else 14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(col.color, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Txt(name, 16.sp, c.fg, bold = true)
                                Txt("  " + stringResource(col.slope), 11.sp, c.fgMuted, mono = true)
                            }
                            Txt(stringResource(col.hint), 12.sp, c.fgMuted)
                        }
                        if (display.showWaves) {
                            val on = mix.waves[i]
                            WaveButton(on, col.tint, stringResource(if (on) Res.string.waves_off else Res.string.waves_on, name)) {
                                onPress()
                                mix.waves[i] = !mix.waves[i]
                                changed()
                            }
                        }
                        Txt(
                            if (liveLevel != null) "~$liveLevel" else "${mix.levels[i]}",
                            13.sp, c.fg, Modifier.width(48.dp), mono = true, align = TextAlign.End,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    NoiseSlider(
                        value = liveLevel ?: mix.levels[i],
                        color = col.tint,
                        description = name,
                        onChange = { mix.levels[i] = it; changed() },
                        onTouch = { touching = if (it) i else -1 },
                    )
                    if (Display.waveBoxVisible(mix.waves[i], display.showWaves)) {
                        Column(Modifier.padding(start = 22.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Txt(stringResource(Res.string.wave_length), 12.sp, c.fgMuted, Modifier.weight(1f))
                                Txt("~${mix.waveSec[i]} s", 12.sp, c.fgMuted, mono = true, align = TextAlign.End)
                            }
                            NoiseSlider(
                                value = mix.waveSec[i] - Mix.WAVE_MIN,
                                color = col.tint,
                                description = stringResource(Res.string.wave_length),
                                onChange = { mix.waveSec[i] = Mix.WAVE_MIN + it; changed() },
                                modifier = Modifier.alpha(0.85f),
                                max = Mix.WAVE_MAX - Mix.WAVE_MIN,
                                valueText = "${mix.waveSec[i]} s",
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Card {
                val vol = stringResource(Res.string.volume)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(vol, 16.sp, c.fg, Modifier.weight(1f), bold = true)
                    Txt("${mix.master} %", 13.sp, c.fg, mono = true)
                }
                Spacer(Modifier.height(2.dp))
                NoiseSlider(mix.master, c.accent, vol, { mix.master = it; changed() }, valueText = "${mix.master} %")
                Spacer(Modifier.height(14.dp))
                val treble = stringResource(Res.string.treble)
                val hz = fmtHz(mix.toneHz())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(treble, 16.sp, c.fg, Modifier.weight(1f), bold = true)
                    Txt(hz, 13.sp, c.fg, mono = true)
                }
                Spacer(Modifier.height(2.dp))
                NoiseSlider(mix.tone, c.accent, treble, { mix.tone = it; changed() }, valueText = hz)
            }
            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(Res.string.group_timer))
            Spacer(Modifier.height(8.dp))
            ChipRow {
                Mix.TIMERS.forEach { t ->
                    val label = if (t == 0) stringResource(Res.string.timer_off) else stringResource(Res.string.timer_chip, t)
                    val desc = if (t > 0) pluralStringResource(Res.plurals.timer_minutes, t, t) else null
                    Chip(label, t == mix.timer, {
                        onPress()
                        mix.timer = t
                        changed()
                        player.renderer?.restartTimer()
                    }, desc)
                }
            }
        }
    }

    AappsScaffold {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val land = maxWidth > maxHeight && maxHeight < 600.dp
            Column(Modifier.fillMaxSize()) {
                // Kopfzeile wie in den anderen aapps: 64 dp, Name links, Zahnrad rechts
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        APP_NAME,
                        Modifier.weight(1f).semantics { heading() },
                        style = Aapps.type.title.copy(color = c.fg),
                    )
                    AappsIconButton(AappsIcons.Gear, stringResource(CoreRes.string.aapps_settings), { onPress(); onSettings() })
                }
                if (land) {
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        Column(
                            Modifier.weight(0.42f).fillMaxHeight().verticalScroll(rememberScrollState())
                                .padding(start = 24.dp, end = 12.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Spacer(Modifier.height(18.dp))
                            bar()
                        }
                        Box(Modifier.weight(0.58f).fillMaxHeight()) { content(true) }
                    }
                } else {
                    // Inhalt scrollt ueber der festen Leiste und blendet weich aus; auf breiten Geraeten mittig begrenzt
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.widthIn(max = 640.dp)) { content(false) }
                        Box(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(24.dp)
                                .background(Brush.verticalGradient(listOf(c.bg.copy(alpha = 0f), c.bg))),
                        )
                    }
                    Box(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 16.dp)) { bar() }
                }
            }
        }
    }
}
