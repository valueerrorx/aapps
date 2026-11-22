package com.valueerrorx.levlr.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.valueerrorx.aapps.core.ui.Aapps
import com.valueerrorx.aapps.core.ui.AappsIcons
import com.valueerrorx.aapps.core.ui.AappsPillButton
import com.valueerrorx.aapps.core.ui.AappsTextButton
import com.valueerrorx.aapps.core.ui.Haptics
import com.valueerrorx.aapps.core.ui.Springs
import com.valueerrorx.levlr.R
import com.valueerrorx.levlr.domain.Bias
import com.valueerrorx.levlr.domain.CalibrationSession
import com.valueerrorx.levlr.domain.CalibrationSession.Problem
import com.valueerrorx.levlr.domain.CalibrationSession.Step
import com.valueerrorx.levlr.domain.LevelMode

data class CalibrationResult(val mode: LevelMode, val bias: Bias)

private val Warn = Color(0xFFF2B84E)

@Composable
fun CalibrationCard(
    state: LevelState,
    session: CalibrationSession,
    onDone: (CalibrationResult) -> Unit,
    haptics: Haptics?,
) {
    val c = Aapps.colors
    state.calibrationTick // bei jedem Messwert neu lesen
    val step = session.step
    val mode = session.mode ?: state.reading.mode
    val progress by animateFloatAsState(session.progress, Springs.gentle(), label = "calProgress")

    // Ergebnis genau einmal melden
    LaunchedEffect(step) {
        if (step == Step.DONE) {
            session.result?.let { onDone(CalibrationResult(session.mode ?: mode, it)) }
            haptics?.confirm()
        } else if (step == Step.SECOND) {
            haptics?.tick()
        }
    }

    val title = if (step == Step.DONE) stringResource(R.string.cal_title_done)
    else stringResource(R.string.cal_title, if (step == Step.FIRST) 1 else 2)
    val body = when (step) {
        Step.FIRST -> stringResource(R.string.cal_step1)
        Step.SECOND -> stringResource(if (mode == LevelMode.EDGE) R.string.cal_step2_edge else R.string.cal_step2_flat)
        Step.DONE -> stringResource(R.string.cal_done)
    }
    val problem = when (session.problem) {
        Problem.NONE -> null
        Problem.WRONG_MODE -> stringResource(if (mode == LevelMode.EDGE) R.string.cal_wrong_mode_edge else R.string.cal_wrong_mode_flat)
        Problem.NOT_ROTATED -> stringResource(if (mode == LevelMode.EDGE) R.string.cal_not_rotated else R.string.cal_not_turned)
        Problem.IMPLAUSIBLE -> stringResource(R.string.cal_implausible)
    }

    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(c.surface.copy(alpha = 0.96f))
            .border(1.dp, c.line, RoundedCornerShape(28.dp))
            .padding(start = 22.dp, end = 14.dp, top = 20.dp, bottom = 12.dp),
    ) {
        BasicText(title.uppercase(), style = Aapps.type.label.copy(color = c.accent))
        Spacer(Modifier.height(8.dp))
        BasicText(body, Modifier.padding(end = 8.dp), style = Aapps.type.body.copy(color = c.fg))
        if (problem != null) {
            Spacer(Modifier.height(8.dp))
            BasicText(problem, Modifier.padding(end = 8.dp), style = Aapps.type.body.copy(color = Warn))
        }
        Spacer(Modifier.height(16.dp))
        // Fortschritt der ruhigen Messphase
        Box(Modifier.padding(end = 8.dp).fillMaxWidth().height(4.dp).background(c.bg, CircleShape)) {
            val p = if (step == Step.DONE) 1f else progress
            if (p > 0f) {
                Box(Modifier.fillMaxWidth(p).height(4.dp).background(c.accent, CircleShape))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (step != Step.DONE) {
                AappsTextButton(stringResource(R.string.cal_cancel), { state.endCalibration() })
                Spacer(Modifier.weight(1f))
                AappsPillButton(
                    text = stringResource(if (session.measuring) R.string.cal_measuring else R.string.cal_measure),
                    onClick = { state.measureCalibration() },
                    icon = if (step == Step.SECOND) AappsIcons.Rotate else AappsIcons.Target,
                    active = !session.measuring,
                    enabled = !session.measuring,
                )
            } else {
                AappsPillButton(
                    text = stringResource(R.string.cal_finish),
                    onClick = { state.endCalibration() },
                    icon = AappsIcons.Check,
                    active = true,
                )
            }
        }
    }
}
