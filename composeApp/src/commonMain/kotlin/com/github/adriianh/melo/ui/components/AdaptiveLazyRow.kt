package com.github.adriianh.melo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.desktopScroll

fun BoxWithConstraintsScope.adaptiveCardWidth(
    minCardWidth: Dp,
    spacing: Dp,
    horizontalPadding: Dp,
): Dp {
    val available = (maxWidth - horizontalPadding * 2).coerceAtLeast(minCardWidth)
    val columns = ((available + spacing) / (minCardWidth + spacing)).toInt().coerceAtLeast(1)
    return (available - spacing * (columns - 1)) / columns
}

@Composable
fun <T> AdaptiveLazyRow(
    items: List<T>,
    minCardWidth: Dp,
    spacing: Dp,
    horizontalPadding: Dp = 24.dp,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T, Dp) -> Unit,
) {
    val listState = rememberLazyListState()
    CarouselScrollContainer(state = listState) {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val cardWidth = adaptiveCardWidth(
                minCardWidth = minCardWidth,
                spacing = spacing,
                horizontalPadding = horizontalPadding
            )
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .desktopScroll(listState)
            ) {
                items(items) { item ->
                    itemContent(item, cardWidth)
                }
            }
        }
    }
}