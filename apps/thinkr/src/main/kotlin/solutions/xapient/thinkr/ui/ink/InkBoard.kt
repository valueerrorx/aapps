package solutions.xapient.thinkr.ui.ink

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.SpaceGrotesk
import solutions.xapient.aapps.core.ui.drawGlow
import solutions.xapient.thinkr.R
import solutions.xapient.thinkr.domain.Spring
import solutions.xapient.thinkr.domain.ink.InkCard
import solutions.xapient.thinkr.domain.ink.InkColor
import solutions.xapient.thinkr.domain.ink.InkState
import solutions.xapient.thinkr.ui.Motion
import solutions.xapient.thinkr.ui.Amber
import solutions.xapient.thinkr.ui.AnswerButton
import solutions.xapient.thinkr.ui.CountOverlay
import solutions.xapient.thinkr.ui.DeadlineBar
import solutions.xapient.thinkr.ui.drawGlass
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

// Abgang der letzten Karte in ms Spielzeit
private const val OUT_MS = 260.0

// Einklappen der neuen Karte nach Beginn ihrer Frist
private const val FLIP_DELAY_MS = 60.0

fun inkColor(c: InkColor): Color = Color(c.argb)

@Composable
fun inkName(c: InkColor): String = stringResource(
    when (c) {
        InkColor.BLUE -> R.string.ink_blue
        InkColor.ORANGE -> R.string.ink_orange
        InkColor.YELLOW -> R.string.ink_yellow
        InkColor.PURPLE -> R.string.ink_purple
        InkColor.RED -> R.string.ink_red
        InkColor.GREEN -> R.string.ink_green
    },
)

// Tinte-Brett: zwei Karten, Nein links, Ja rechts; alle Bewegung haengt an der Spielzeit
@Composable
fun InkBoard(s: InkState, live: Boolean, count: Int, onAnswer: (Boolean) -> Unit) {
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }
    val now = s.round.now.toDouble()
    val last = s.last
    // kurzes Aufleuchten der gedrueckten Taste: Akzent richtig, Bernstein falsch
    val flash = if (last?.said != null) exp(-(now - last.at) / 220.0).toFloat() else 0f
    val flashColor = if (last?.correct == true) Aapps.colors.accent else Amber
    val flashYes = if (last?.said == true) flash else 0f
    val flashNo = if (last?.said == false) flash else 0f

    val stage: @Composable (Modifier) -> Unit = { m ->
        BoxWithConstraints(m, contentAlignment = Alignment.Center) {
            val cardW = min(min((maxWidth - 40.dp) / 2, (maxHeight - 120.dp) / 1.3f), 220.dp).coerceAtLeast(96.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Cards(s, cardW, count > 0)
                Spacer(Modifier.height(20.dp))
                val show = live && now >= s.shownAt && !s.round.isOver
                DeadlineBar(((s.dueAt - now) / s.deadlineMs).toFloat(), show, Modifier.width(cardW * 1.6f))
            }
            if (count > 0) CountOverlay(stringResource(R.string.ink_prompt), count)
        }
    }

    if (landscape) {
        Row(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            AnswerButton(false, live, flashNo, flashColor, { onAnswer(false) }, Modifier.width(148.dp).height(124.dp))
            stage(Modifier.weight(1f).fillMaxHeight())
            AnswerButton(true, live, flashYes, flashColor, { onAnswer(true) }, Modifier.width(148.dp).height(124.dp))
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            stage(Modifier.weight(1f).fillMaxWidth())
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnswerButton(false, live, flashNo, flashColor, { onAnswer(false) }, Modifier.weight(1f).heightIn(min = 88.dp))
                AnswerButton(true, live, flashYes, flashColor, { onAnswer(true) }, Modifier.weight(1f).heightIn(min = 88.dp))
            }
        }
    }
}

// Beschriftung und beide Karten; die alte Karte tritt ab, die neue klappt herein
@Composable
private fun Cards(s: InkState, w: Dp, counting: Boolean) {
    val c = Aapps.colors
    val now = s.round.now.toDouble()
    val h = w * 1.3f
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Label(stringResource(R.string.ink_meaning), w)
            Label(stringResource(R.string.ink_ink), w)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (right in listOf(false, true)) {
                Box(Modifier.size(w, h), contentAlignment = Alignment.Center) {
                    // leerer Platz, damit die Flaeche beim Wechsel nicht springt
                    Box(Modifier.size(w, h).drawBehind { drawGlass(Offset.Zero, size, size.width * 0.14f, c.line.copy(alpha = 0.5f), fill = c.surface.copy(alpha = 0.5f)) })
                    val last = s.last
                    if (!counting && last != null && now - last.at < OUT_MS) {
                        Face(s.cards[last.index], right, w, h, out = ((now - last.at) / OUT_MS).toFloat(), good = last.correct)
                    }
                    if (!counting) {
                        // erst wenn die alte Karte fast weg ist; rechts etwas spaeter
                        val f = Spring.step(now - s.shownAt - FLIP_DELAY_MS - if (right) 50.0 else 0.0, hz = 2.6, damping = 0.62).toFloat()
                        if (f > 0f) Face(s.card, right, w, h, flip = f)
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String, w: Dp) {
    BasicText(
        text.uppercase(),
        Modifier.width(w),
        style = Aapps.type.label.copy(color = Aapps.colors.fgMuted, textAlign = TextAlign.Center),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = Aapps.type.label.fontSize),
    )
}

// Eine Karte: flip = Einklappen (1 = steht), out = Abgang (0 = steht); good = Abgang nach richtiger Antwort
@Composable
private fun Face(card: InkCard, right: Boolean, w: Dp, h: Dp, flip: Float = 1f, out: Float = 0f, good: Boolean = true) {
    val c = Aapps.colors
    val px = with(LocalDensity.current) { w.toPx() }
    val color = when {
        right -> inkColor(card.ink)
        card.meaningInk != null -> inkColor(card.meaningInk)
        else -> c.fg
    }
    val word = inkName(if (right) card.word else card.meaning)
    val desc = if (right) stringResource(R.string.ink_word_desc, word, inkName(card.ink)) else word
    val bad = out > 0f && !good
    Box(
        Modifier
            .size(w, h)
            .semantics { contentDescription = desc }
            .graphicsLayer {
                cameraDistance = 12f * density
                rotationY = 90f * (1f - flip)
                if (out > 0f) {
                    alpha = 1f - out
                    if (good) {
                        translationY = -out * px * 0.25f
                        val sc = 1f - 0.08f * out
                        scaleX = sc; scaleY = sc
                    } else {
                        translationX = sin(out * 5f * PI.toFloat()) * (1f - out) * px * Motion.still(0.06f)
                        translationY = out * px * 0.18f
                    }
                }
            }
            .drawBehind {
                drawGlass(Offset.Zero, size, size.width * 0.14f, if (bad) Amber else c.accent.copy(alpha = 0.35f))
                if (right || card.meaningInk != null) drawGlow(color, center, size.width * 0.7f, 0.14f)
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            word.uppercase(),
            Modifier.padding(horizontal = 10.dp),
            style = Aapps.type.headline.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 40.sp),
        )
    }
}
