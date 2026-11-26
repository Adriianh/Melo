package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_instrumental_break
import com.github.adriianh.melo.util.LocalWindowActivity
import com.github.adriianh.melo.util.rememberSharedAnimationTime
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

private const val DOTS_TWO_PI = (2.0 * PI).toFloat()
private const val DOT_CYCLE_SECONDS = 1.2f
private const val DOT_PHASE_STEP_SECONDS = 0.22f

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
fun DancingMelodyIndicator(
    isActive: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    baseFontSize: TextUnit = 17.sp,
    noteCount: Int = 3,
) {
    val dotCount = noteCount.coerceIn(1, 5)
    val dotSize = (baseFontSize.value * 0.36f).coerceIn(6f, 12f).dp
    val dotSpacing = (dotSize.value * 0.9f).coerceIn(6f, 10f).dp
    val a11yLabel = stringResource(Res.string.a11y_instrumental_break)
    val shouldAnimate = isActive && LocalWindowActivity.current.isVisible

    if (shouldAnimate) {
        val elapsedSeconds = rememberSharedAnimationTime(enabled = true)
        AnimatedDotsRow(
            elapsedSeconds = elapsedSeconds,
            tint = tint,
            dotSize = dotSize,
            dotSpacing = dotSpacing,
            dotCount = dotCount,
            a11yLabel = a11yLabel,
            modifier = modifier,
        )
    } else {
        StaticDotsRow(
            tint = tint,
            dotSize = dotSize,
            dotSpacing = dotSpacing,
            dotCount = dotCount,
            isActive = isActive,
            a11yLabel = a11yLabel,
            modifier = modifier,
        )
    }
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
private fun AnimatedDotsRow(
    elapsedSeconds: FloatState,
    tint: Color,
    dotSize: Dp,
    dotSpacing: Dp,
    dotCount: Int,
    a11yLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.semantics { contentDescription = a11yLabel },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
    ) {
        for (i in 0 until dotCount) {
            val phaseOffset = i * DOT_PHASE_STEP_SECONDS
            Box(
                modifier =
                    Modifier
                        .size(dotSize)
                        .graphicsLayer {
                            val t = (elapsedSeconds.floatValue - phaseOffset).coerceAtLeast(0f)
                            val wave = 0.5f + 0.5f * sin(t * (DOTS_TWO_PI / DOT_CYCLE_SECONDS))
                            val scale = 0.78f + 0.34f * wave
                            scaleX = scale
                            scaleY = scale
                            alpha = 0.42f + 0.58f * wave
                            translationY = (-3.0f * wave).dp.toPx()
                        }.background(color = tint, shape = CircleShape),
            )
        }
    }
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
private fun StaticDotsRow(
    tint: Color,
    dotSize: Dp,
    dotSpacing: Dp,
    dotCount: Int,
    isActive: Boolean,
    a11yLabel: String,
    modifier: Modifier = Modifier,
) {
    val dotAlpha = if (isActive) 0.92f else 0.55f
    Row(
        modifier = modifier.semantics { contentDescription = a11yLabel },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
    ) {
        for (i in 0 until dotCount) {
            Box(
                modifier =
                    Modifier
                        .size(dotSize)
                        .background(color = tint.copy(alpha = dotAlpha), shape = CircleShape),
            )
        }
    }
}
