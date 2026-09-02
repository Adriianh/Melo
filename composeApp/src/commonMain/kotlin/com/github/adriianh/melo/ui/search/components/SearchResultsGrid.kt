package com.github.adriianh.melo.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun searchResultContentPadding(paddingValues: PaddingValues): PaddingValues =
    PaddingValues(
        start = 24.dp,
        end = 24.dp,
        top = 8.dp,
        bottom = paddingValues.calculateBottomPadding() + 16.dp
    )

@Composable
internal fun <T> SearchResultsGrid(
    items: List<T>,
    columns: GridCells,
    paddingValues: PaddingValues,
    horizontalSpacing: Dp = 14.dp,
    verticalSpacing: Dp = 14.dp,
    modifier: Modifier = Modifier,
    key: (Int, T) -> Any,
    itemContent: @Composable (T) -> Unit,
) {
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.fillMaxSize(),
        contentPadding = searchResultContentPadding(paddingValues),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        itemsIndexed(items, key = { index, item -> key(index, item) }) { _, item ->
            itemContent(item)
        }
    }
}