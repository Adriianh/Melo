package com.github.adriianh.melo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.LocalMeloColors
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform
import kotlinx.coroutines.launch

@Composable
fun CarouselScrollContainer(
    state: LazyListState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val platform = remember { getPlatform() }
    if (platform.type != PlatformType.DESKTOP) {
        Box(modifier = modifier, content = content)
        return
    }

    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val canScrollBackward by remember { derivedStateOf { state.canScrollBackward } }
    val canScrollForward by remember { derivedStateOf { state.canScrollForward } }

    Box(
        modifier = modifier.hoverable(interactionSource)
    ) {
        content()

        AnimatedVisibility(
            visible = isHovered && canScrollBackward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
        ) {
            ScrollButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                onClick = {
                    scope.launch {
                        val scrollAmount = state.layoutInfo.viewportSize.width * 0.7f
                        state.animateScrollBy(
                            value = -scrollAmount,
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = isHovered && canScrollForward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
        ) {
            ScrollButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                onClick = {
                    scope.launch {
                        val scrollAmount = state.layoutInfo.viewportSize.width * 0.7f
                        state.animateScrollBy(
                            value = scrollAmount,
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ScrollButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    val meloColors = LocalMeloColors.current
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonHovered by buttonInteractionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                clip = false
            )
            .clip(CircleShape)
            .background(meloColors.playerBarFill)
            .border(0.5.dp, meloColors.glassBorder, CircleShape)
            .hoverable(buttonInteractionSource)
            .clickable(
                interactionSource = buttonInteractionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isButtonHovered) meloColors.brandAccent else meloColors.textPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}