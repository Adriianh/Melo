package com.github.adriianh.melo.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MEDIUM_WIDTH_MIN = 600.dp
private val EXPANDED_WIDTH_MIN = 840.dp

enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded,
}

data class MeloWindowSizeClass(
    val widthClass: WindowWidthClass,
    val isLandscape: Boolean,
) {
    val isCompact: Boolean get() = widthClass == WindowWidthClass.Compact
    val isMediumPortrait: Boolean get() = widthClass == WindowWidthClass.Medium && !isLandscape
    val isExpandedOrMediumLandscape: Boolean
        get() = widthClass == WindowWidthClass.Expanded || (widthClass == WindowWidthClass.Medium && isLandscape)
}

fun windowSizeClassForDimensions(
    width: Dp,
    height: Dp,
): MeloWindowSizeClass {
    val widthClass =
        when {
            width >= EXPANDED_WIDTH_MIN -> WindowWidthClass.Expanded
            width >= MEDIUM_WIDTH_MIN -> WindowWidthClass.Medium
            else -> WindowWidthClass.Compact
        }
    return MeloWindowSizeClass(
        widthClass = widthClass,
        isLandscape = width >= height,
    )
}

@Composable
fun rememberMeloWindowSizeClass(): MeloWindowSizeClass {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val heightDp = with(density) { windowInfo.containerSize.height.toDp() }
    return windowSizeClassForDimensions(width = widthDp, height = heightDp)
}
