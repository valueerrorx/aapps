package com.valueerrorx.scorr

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.valueerrorx.scorr.domain.Board
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

// Gesten auf der echten Tafel: Tippen, Ziehen, lange Druecken
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class GestureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun tile() = rule.onNodeWithContentDescription("Player 1")

    private fun points(n: Int) =
        tile().assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "${Board.format(n)} points"))

    @Test fun tapTopAddsTapBottomSubtracts() {
        points(0)
        tile().performTouchInput { click(Offset(centerX, height * 0.25f)) }
        points(1)
        tile().performTouchInput { click(Offset(centerX, height * 0.75f)) }
        points(0)
        tile().performTouchInput { click(Offset(centerX, height * 0.75f)) }
        points(-1)
    }

    // Finger in kleinen Schritten senkrecht bewegen
    private fun TouchInjectionScope.slide(dp: Int) {
        val step = if (dp < 0) -4 else 4
        repeat(abs(dp) / 4) { moveBy(Offset(0f, step.dp.toPx())) }
    }

    @Test fun dragUpThreeStepsAddsThree() {
        tile().performTouchInput {
            down(center)
            slide(-(24 * 3 + 4))
            up()
        }
        points(3)
    }

    @Test fun dragBackToStartBooksNothing() {
        tile().performTouchInput {
            down(center)
            slide(-52)
            slide(52)
            up()
        }
        points(0)
        rule.onNodeWithContentDescription("Undo").assertDoesNotExist()
    }

    @Test fun longPressOpensEntryWithoutPoint() {
        tile().performTouchInput { longClick(Offset(centerX, height * 0.25f)) }
        rule.onNodeWithContentDescription("Points").assertExists()
        points(0)
    }
}
