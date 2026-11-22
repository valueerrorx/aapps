package com.valueerrorx.aapps.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

// Einheitliche Federn fuer Compose-Animationen
object Springs {
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
    fun <T> gentle(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)
}
