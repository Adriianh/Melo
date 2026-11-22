package com.github.adriianh.melo.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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

@Composable
fun <T> MultiRowHorizontalCarousel(
    items: List<T>,
    rows: Int,
    minCardWidth: Dp,
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    verticalSpacing: Dp = 4.dp,
    horizontalPadding: Dp = 24.dp,
    containerWidth: Dp? = null,
    listState: LazyListState = rememberLazyListState(),
    key: ((List<T>) -> Any)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    val columns = remember(items, rows) { items.chunked(rows) }

    CarouselScrollContainer(state = listState) {
        val content: @Composable (Dp) -> Unit = { columnWidth ->
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier =
                    modifier
                        .fillMaxWidth()
                        .desktopScroll(listState),
            ) {
                items(
                    items = columns,
                    key = key ?: { col -> col.firstOrNull()?.hashCode() ?: col.hashCode() },
                    contentType = { "multi_row_column" },
                ) { columnItems ->
                    Column(
                        modifier = Modifier.width(columnWidth),
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                    ) {
                        columnItems.forEach { item ->
                            itemContent(item)
                        }
                    }
                }
            }
        }

        if (containerWidth != null) {
            val columnWidth =
                remember(containerWidth, minCardWidth, spacing, horizontalPadding) {
                    calculateAdaptiveCardWidth(
                        containerWidth = containerWidth,
                        minCardWidth = minCardWidth,
                        spacing = spacing,
                        horizontalPadding = horizontalPadding,
                    )
                }
            content(columnWidth)
        } else {
            BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
                val columnWidth =
                    calculateAdaptiveCardWidth(
                        containerWidth = maxWidth,
                        minCardWidth = minCardWidth,
                        spacing = spacing,
                        horizontalPadding = horizontalPadding,
                    )
                content(columnWidth)
            }
        }
    }
}
