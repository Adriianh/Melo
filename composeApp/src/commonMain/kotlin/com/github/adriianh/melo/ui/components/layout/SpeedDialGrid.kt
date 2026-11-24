@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.search.itemKey
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.search_type_artist
import com.github.adriianh.melo.ui.components.track.RowContextMenuButton
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.ui.components.track.songMenuRightClick
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.desktopScroll
import com.github.adriianh.melo.util.getPlatform
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedDialGrid(
    items: List<SearchResult>,
    onItemClick: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
    containerWidth: Dp? = null,
    listState: LazyListState = rememberLazyListState(),
    onItemMoreClick: ((SearchResult, SongMenuAnchor?) -> Unit)? = null,
) {
    val columns = remember(items) { items.chunked(3) }

    CarouselScrollContainer(state = listState) {
        if (containerWidth != null) {
            val columnWidth =
                remember(containerWidth) {
                    calculateAdaptiveCardWidth(
                        containerWidth = containerWidth,
                        minCardWidth = 280.dp,
                        spacing = 10.dp,
                        horizontalPadding = 24.dp,
                    )
                }
            SpeedDialRowContent(
                columns = columns,
                columnWidth = columnWidth,
                listState = listState,
                modifier = modifier,
                onItemClick = onItemClick,
                onItemMoreClick = onItemMoreClick,
            )
        } else {
            BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
                val columnWidth =
                    calculateAdaptiveCardWidth(
                        containerWidth = maxWidth,
                        minCardWidth = 280.dp,
                        spacing = 10.dp,
                        horizontalPadding = 24.dp,
                    )
                SpeedDialRowContent(
                    columns = columns,
                    columnWidth = columnWidth,
                    listState = listState,
                    modifier = Modifier,
                    onItemClick = onItemClick,
                    onItemMoreClick = onItemMoreClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialRowContent(
    columns: List<List<SearchResult>>,
    columnWidth: Dp,
    listState: LazyListState,
    modifier: Modifier,
    onItemClick: (SearchResult) -> Unit,
    onItemMoreClick: ((SearchResult, SongMenuAnchor?) -> Unit)? = null,
) {
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .desktopScroll(listState),
    ) {
        items(
            items = columns,
            key = { col -> col.firstOrNull()?.itemKey() ?: col.hashCode() },
            contentType = { "speed_dial_column" },
        ) { columnItems ->
            Column(
                modifier = Modifier.width(columnWidth),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                columnItems.forEach { item ->
                    SpeedDialRowItem(
                        item = item,
                        onItemClick = onItemClick,
                        onItemMoreClick = onItemMoreClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialRowItem(
    item: SearchResult,
    onItemClick: (SearchResult) -> Unit,
    onItemMoreClick: ((SearchResult, SongMenuAnchor?) -> Unit)?,
) {
    val (title, subtitle, artworkUrl) = speedDialDisplayData(item)
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    val isDesktop = remember { getPlatform().type == PlatformType.DESKTOP }
    var rowBounds by remember { mutableStateOf<Rect?>(null) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MeloColors.glassSurface.copy(alpha = 0.35f))
                .hoverable(hoverSource)
                .onGloballyPositioned { rowBounds = it.boundsInWindow() }
                .songMenuRightClick(
                    enabled = onItemMoreClick != null,
                    onOpenMenu = { anchor -> onItemMoreClick?.invoke(item, anchor) },
                ).combinedClickable(
                    onClick = { onItemClick(item) },
                    onLongClick =
                        onItemMoreClick?.let { open ->
                            { open(item, rowBounds?.let { SongMenuAnchor.Bounds(it) }) }
                        },
                ).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MeloAsyncImage(
            url = artworkUrl,
            contentDescription = title,
            size = 48.dp,
            shape = RoundedCornerShape(6.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MeloType.labelMedium,
                color = MeloColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MeloType.labelSmall,
                color = MeloColors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onItemMoreClick != null) {
            RowContextMenuButton(
                isVisible = !isDesktop || isHovered,
                onOpenMenu = { anchor -> onItemMoreClick(item, anchor) },
                size = 28.dp,
                iconSize = 18.dp,
            )
        }
    }
}

@Composable
private fun speedDialDisplayData(item: SearchResult): Triple<String, String, String?> =
    when (item) {
        is SearchResult.Song -> Triple(item.track.title, item.track.artist, item.track.artworkUrl)
        is SearchResult.Album -> Triple(item.title, item.author, item.artworkUrl)
        is SearchResult.Playlist -> Triple(item.title, item.author, item.artworkUrl)
        is SearchResult.Artist -> Triple(item.name, stringResource(Res.string.search_type_artist), item.artworkUrl)
    }
