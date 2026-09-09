package com.github.adriianh.melo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.desktopScroll

fun calculateAdaptiveCardWidth(
    containerWidth: Dp,
    minCardWidth: Dp,
    spacing: Dp,
    horizontalPadding: Dp,
): Dp {
    val available = (containerWidth - horizontalPadding * 2).coerceAtLeast(minCardWidth)
    val columns = ((available + spacing) / (minCardWidth + spacing)).toInt().coerceAtLeast(1)
    return (available - spacing * (columns - 1)) / columns
}

@Composable
fun <T> AdaptiveLazyRow(
    items: List<T>,
    minCardWidth: Dp,
    spacing: Dp,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 24.dp,
    containerWidth: Dp? = null,
    key: ((T) -> Any)? = null,
    listState: LazyListState = rememberLazyListState(),
    itemContent: @Composable (T, Dp) -> Unit,
) {
    CarouselScrollContainer(state = listState) {
        if (containerWidth != null) {
            val cardWidth = remember(containerWidth, minCardWidth, spacing, horizontalPadding) {
                calculateAdaptiveCardWidth(containerWidth, minCardWidth, spacing, horizontalPadding)
            }
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier = modifier
                    .fillMaxWidth()
                    .desktopScroll(listState)
            ) {
                items(
                    items = items,
                    key = key,
                    contentType = { "adaptive_card" }
                ) { item ->
                    itemContent(item, cardWidth)
                }
            }
        } else {
            BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
                val cardWidth = calculateAdaptiveCardWidth(
                    maxWidth,
                    minCardWidth,
                    spacing,
                    horizontalPadding
                )
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    modifier = Modifier
                        .fillMaxWidth()
                        .desktopScroll(listState)
                ) {
                    items(
                        items = items,
                        key = key,
                        contentType = { "adaptive_card" }
                    ) { item ->
                        itemContent(item, cardWidth)
                    }
                }
            }
        }
    }
}