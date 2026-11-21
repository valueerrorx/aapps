package com.valueerrorx.aapps.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp

// Weicher radialer Lichthof; Abfall quadratisch, damit kein harter Rand entsteht
fun DrawScope.drawGlow(color: Color, center: Offset, radius: Float, alpha: Float = 0.35f) {
    if (radius <= 0f || alpha <= 0f) return
    val brush = Brush.radialGradient(
        0f to color.copy(alpha = alpha),
        0.35f to color.copy(alpha = alpha * 0.45f),
        0.7f to color.copy(alpha = alpha * 0.12f),
        1f to Color.Transparent,
        center = center,
        radius = radius,
    )
    drawCircle(brush, radius, center)
}

fun Modifier.glow(color: Color, radius: Dp, alpha: Float = 0.35f): Modifier = drawBehind {
    drawGlow(color, center, radius.toPx(), alpha)
}
