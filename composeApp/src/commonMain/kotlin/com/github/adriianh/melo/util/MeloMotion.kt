package com.github.adriianh.melo.util

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween

object MeloMotion {
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    fun <T> instant() = tween<T>(100, easing = LinearEasing)
    fun <T> fast() = tween<T>(150, easing = FastOutSlowInEasing)
    fun <T> medium() = tween<T>(250, easing = FastOutSlowInEasing)
    fun <T> slow() = tween<T>(400, easing = EmphasizedDecelerateEasing)
}
