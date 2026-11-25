package solutions.xapient.thinkr.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsIcon
import solutions.xapient.aapps.core.ui.AappsIcons
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.aapps.core.ui.AappsScaffold
import solutions.xapient.aapps.core.ui.AappsTextButton
import solutions.xapient.aapps.core.ui.Haptics
import solutions.xapient.aapps.core.ui.glow
import solutions.xapient.thinkr.R

data class ResultView(
    @param:StringRes val name: Int,
    val icon: ImageVector,
    val score: Int,
    val level: Int,
    val prevLevel: Int,
    val accuracy: Int,
    val best: Int,
    val newBest: Boolean,
    val dailyComplete: Boolean,
    val hasNext: Boolean,
    // „im Ziel / gesamt" statt Prozent
    val tally: Boolean = false,
    val hits: Int = 0,
    val total: Int = 0,
)

// Ergebnis: Punkte als Hero, darunter Stufe, Treffer, Bestwert; Weiter bzw. Nochmal
@Composable
fun ResultScreen(
    r: ResultView,
    haptics: Haptics?,
    onNext: () -> Unit,
    onAgain: () -> Unit,
    onDone: () -> Unit,
    animate: Boolean = true,
    shineAt: Float = -1f,
) {
    val c = Aapps.colors
    // Glanz einmal ueber „Neuer Bestwert"
    val shine = remember { Animatable(-1f) }
    LaunchedEffect(Unit) {
        if (!animate || !r.newBest) return@LaunchedEffect
        delay(350)
        haptics?.tick()
        shine.snapTo(0f)
        shine.animateTo(1f, spring(dampingRatio = 1f, stiffness = 40f))
        shine.snapTo(-1f)
    }
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val hero: @Composable (Modifier) -> Unit = { m ->
        Column(m, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AappsIcon(r.icon, null, c.fgMuted, size = 18.dp)
                Spacer(Modifier.width(8.dp))
                BasicText(stringResource(r.name).uppercase(),style = Aapps.type.label.copy(color = c.fgMuted))
            }
            Spacer(Modifier.height(8.dp))
            val points = formatPoints(r.score)
            val desc = stringResource(R.string.points_n, points)
            BasicText(
                points,
                Modifier
                    .fillMaxWidth()
                    .glow(c.accent, 120.dp, if (r.newBest) 0.22f else 0.12f)
                    .semantics { contentDescription = desc },
                style = Aapps.type.display.copy(color = if (r.newBest) c.accent else c.fg, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 36.sp, maxFontSize = Aapps.type.display.fontSize, stepSize = 2.sp),
            )
            BasicText(stringResource(R.string.points).uppercase(), style = Aapps.type.label.copy(color = c.fgMuted))
            if (r.newBest) {
                Spacer(Modifier.height(18.dp))
                BestChip(if (shineAt >= 0f) shineAt else shine.value)
            }
        }
    }
    val stats: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val arrow = when {
                r.level > r.prevLevel -> ThinkrIcons.Up
                r.level < r.prevLevel -> ThinkrIcons.Down
                else -> null
            }
            val change = when {
                r.level > r.prevLevel -> stringResource(R.string.level_up)
                r.level < r.prevLevel -> stringResource(R.string.level_down)
                else -> stringResource(R.string.level_same)
            }
            Stat(stringResource(R.string.level), r.level.toString(), arrow, "${stringResource(R.string.level)} ${r.level}, $change")
            if (r.tally) {
                Stat(stringResource(R.string.accuracy), "${r.hits}/${r.total}", null, stringResource(R.string.tally_desc, r.hits, r.total))
            } else {
                Stat(stringResource(R.string.accuracy), stringResource(R.string.percent, r.accuracy), null, null)
            }
            Stat(stringResource(R.string.best), formatPoints(r.best), null, null)
        }
    }
    val daily: @Composable () -> Unit = {
        if (r.dailyComplete) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AappsIcon(ThinkrIcons.Spark, null, c.accent, size = 18.dp)
                Spacer(Modifier.width(8.dp))
                BasicText(stringResource(R.string.daily_complete), style = Aapps.type.body.copy(color = c.accent))
            }
        }
    }
    val actions: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (r.hasNext) {
                AappsPillButton(stringResource(R.string.go_on), { haptics?.press(); onNext() }, Modifier.widthIn(min = 196.dp), icon = ThinkrIcons.Play, active = true)
            } else {
                AappsPillButton(stringResource(R.string.again), { haptics?.press(); onAgain() }, Modifier.widthIn(min = 196.dp), icon = AappsIcons.Rotate, active = true)
            }
            Spacer(Modifier.height(4.dp))
            AappsTextButton(stringResource(R.string.finish), onDone)
        }
    }
    AappsScaffold {
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                hero(Modifier.weight(0.55f).fillMaxHeight())
                Column(
                    Modifier.weight(0.45f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    stats()
                    Spacer(Modifier.height(20.dp))
                    daily()
                    Spacer(Modifier.height(20.dp))
                    actions()
                }
            }
        } else {
            // Hochformat: Punkte und Werte als ein Block in der Mitte, Aktionen unten
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    hero(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(44.dp))
                    stats()
                }
                Spacer(Modifier.height(16.dp))
                daily()
                Spacer(Modifier.height(20.dp))
                Box(Modifier.padding(bottom = 16.dp)) { actions() }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, arrow: ImageVector?, desc: String?) {
    val c = Aapps.colors
    Column(
        Modifier.semantics(mergeDescendants = true) { if (desc != null) contentDescription = desc }.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(value, style = Aapps.type.number.copy(color = c.fg, fontSize = 26.sp), maxLines = 1)
            if (arrow != null) {
                Spacer(Modifier.width(4.dp))
                AappsIcon(arrow, null, if (arrow == ThinkrIcons.Up) c.accent else c.fgMuted, size = 20.dp)
            }
        }
        Spacer(Modifier.height(2.dp))
        BasicText(label.uppercase(), style = Aapps.type.label.copy(color = c.fgMuted), maxLines = 1)
    }
}

// Plakette mit einmaligem Glanzstreifen
@Composable
private fun BestChip(shine: Float) {
    val c = Aapps.colors
    Row(
        Modifier
            .clip(CircleShape)
            .background(c.accent.copy(alpha = 0.14f), CircleShape)
            .border(1.dp, c.accent.copy(alpha = 0.7f), CircleShape)
            .drawWithContent {
                drawContent()
                if (shine in 0f..1f) {
                    val x = -size.width * 0.3f + size.width * 1.6f * shine
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                            start = Offset(x - 40f, 0f), end = Offset(x + 40f, size.height),
                        ),
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AappsIcon(ThinkrIcons.Spark, null, c.accent, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        BasicText(stringResource(R.string.new_best).uppercase(), style = Aapps.type.label.copy(color = c.accent))
    }
}
