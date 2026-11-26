@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_add_to_queue
import com.github.adriianh.melo.resources.a11y_queue_options
import com.github.adriianh.melo.resources.a11y_refresh_suggestions
import com.github.adriianh.melo.resources.a11y_remove_from_queue
import com.github.adriianh.melo.resources.a11y_reorder
import com.github.adriianh.melo.resources.player_queue_clear
import com.github.adriianh.melo.resources.player_queue_collapse
import com.github.adriianh.melo.resources.player_queue_more_count
import com.github.adriianh.melo.resources.player_queue_save_playlist
import com.github.adriianh.melo.resources.player_section_suggestions
import com.github.adriianh.melo.resources.player_section_up_next
import com.github.adriianh.melo.resources.player_suggestions_empty
import com.github.adriianh.melo.ui.components.cards.SuggestionSkeletonCard
import com.github.adriianh.melo.ui.components.controls.AnimatedEqualizerBars
import com.github.adriianh.melo.ui.components.controls.LocalSwipeRevealGroup
import com.github.adriianh.melo.ui.components.controls.MeloSwipeActions
import com.github.adriianh.melo.ui.components.controls.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.controls.rememberSwipeRevealGroup
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.ui.components.track.songMenuRightClick
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.PlayerUiState
import com.github.adriianh.melo.util.getPlatform
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val DockedSecondaryTextColor = Color.White.copy(alpha = 0.70f)
private val DockedTouchTargetSize = 44.dp
private val DockedRowMinHeight = 60.dp
private val DockedRowCoverSize = 44.dp

@Composable
internal fun DockedQueueContent(
    state: PlayerUiState,
    activeAccent: Color,
    queueViewModel: QueueViewModel = koinViewModel(),
    onRemoveFromQueue: ((Track) -> Unit)? = null,
    onClearQueue: (() -> Unit)? = null,
    onSaveQueueAsPlaylist: ((List<Track>) -> Unit)? = null,
    onOpenContextMenu: ((Track, Boolean, SongMenuAnchor?) -> Unit)? = null,
) {
    val queueState by queueViewModel.queueState.collectAsState()
    val suggestions by queueViewModel.suggestions.collectAsState()
    val isLoadingSuggestions by queueViewModel.isLoadingSuggestions.collectAsState()

    val upNextTracks =
        remember(queueState.tracks, queueState.currentIndex) {
            if (queueState.currentIndex >= 0 && queueState.currentIndex < queueState.tracks.size) {
                queueState.tracks.drop(queueState.currentIndex + 1)
            } else {
                emptyList()
            }
        }

    DockedQueueSectionContent(
        currentTrackId = state.currentTrack?.id,
        isPlaying = state.isPlaying,
        upNextTracks = upNextTracks,
        suggestions = suggestions,
        isLoadingSuggestions = isLoadingSuggestions,
        activeAccent = activeAccent,
        onPlayQueueTrack = queueViewModel::playTrackInQueue,
        onRemoveQueueTrack = { track ->
            onRemoveFromQueue?.invoke(track) ?: queueViewModel.removeTrackFromQueue(track)
        },
        onMoveQueueTrack = { fromRel, toRel ->
            val fromAbs = queueState.currentIndex + 1 + fromRel
            val toAbs = queueState.currentIndex + 1 + toRel
            queueViewModel.moveTrack(fromAbs, toAbs)
        },
        onClearQueue = {
            onClearQueue?.invoke() ?: queueViewModel.clearUpcomingQueue()
        },
        onSaveAsPlaylist = {
            val allQueueTracks = listOfNotNull(state.currentTrack) + upNextTracks
            onSaveQueueAsPlaylist?.invoke(allQueueTracks)
        },
        onRefreshSuggestions = queueViewModel::refreshSuggestions,
        onPlaySuggestion = queueViewModel::playTrack,
        onAddSuggestionToQueue = queueViewModel::insertTrackNext,
        onOpenContextMenu = onOpenContextMenu,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DockedQueueSectionContent(
    currentTrackId: String?,
    isPlaying: Boolean,
    upNextTracks: List<Track>,
    suggestions: List<Track>,
    isLoadingSuggestions: Boolean,
    activeAccent: Color,
    onPlayQueueTrack: (Track) -> Unit,
    onRemoveQueueTrack: (Track) -> Unit,
    onMoveQueueTrack: (Int, Int) -> Unit,
    onClearQueue: () -> Unit,
    onSaveAsPlaylist: () -> Unit,
    onRefreshSuggestions: () -> Unit,
    onPlaySuggestion: (Track) -> Unit,
    onAddSuggestionToQueue: (Track) -> Unit,
    onOpenContextMenu: ((Track, Boolean, SongMenuAnchor?) -> Unit)? = null,
    initialExpanded: Boolean = false,
) {
    var isQueueExpanded by remember { mutableStateOf(initialExpanded) }
    val visibleQueueTracks = if (isQueueExpanded) upNextTracks else upNextTracks.take(1)
    val upNextKeys =
        remember(visibleQueueTracks) {
            val counts = mutableMapOf<String, Int>()
            visibleQueueTracks.map { track ->
                val count = counts.getOrPut(track.id) { 0 }
                counts[track.id] = count + 1
                "docked_q_${track.id}_$count"
            }
        }

    val listState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            val fromRel = upNextKeys.indexOf(from.key)
            val toRel = upNextKeys.indexOf(to.key)
            if (fromRel != -1 && toRel != -1 && fromRel != toRel) {
                onMoveQueueTrack(fromRel, toRel)
            }
        }
    val revealGroup = rememberSwipeRevealGroup()

    CompositionLocalProvider(LocalSwipeRevealGroup provides revealGroup) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            if (upNextTracks.isNotEmpty()) {
                item(key = "docked_up_next_header") {
                    DockedUpNextHeader(
                        upNextCount = upNextTracks.size,
                        isQueueExpanded = isQueueExpanded,
                        activeAccent = activeAccent,
                        onToggleExpand = { isQueueExpanded = !isQueueExpanded },
                        onClearQueue = onClearQueue,
                        onSaveAsPlaylist = onSaveAsPlaylist,
                    )
                }

                itemsIndexed(
                    visibleQueueTracks,
                    key = { index, _ -> upNextKeys.getOrElse(index) { "docked_q_$index" } },
                ) { index, track ->
                    val itemKey = upNextKeys.getOrElse(index) { "docked_q_$index" }
                    ReorderableItem(state = reorderState, key = itemKey) { isDragging ->
                        MeloSwipeableItem(
                            onSwipeRight = {},
                            trailingActions =
                                listOf(
                                    MeloSwipeActions.remove() to { onRemoveQueueTrack(track) },
                                ),
                            cornerRadius = 10,
                        ) {
                            DockedUpNextRow(
                                track = track,
                                isDragging = isDragging,
                                showDragHandle = isQueueExpanded && visibleQueueTracks.size > 1,
                                activeAccent = activeAccent,
                                onClick = { onPlayQueueTrack(track) },
                                onRemove = { onRemoveQueueTrack(track) },
                                onOpenContextMenu =
                                    onOpenContextMenu?.let { open -> { anchor -> open(track, true, anchor) } },
                            )
                        }
                    }
                }
            }

            item(key = "docked_suggestions_header") {
                Spacer(modifier = Modifier.height(4.dp))
                DockedSuggestionsHeader(
                    isLoading = isLoadingSuggestions,
                    activeAccent = activeAccent,
                    onRefresh = onRefreshSuggestions,
                )
            }

            if (isLoadingSuggestions && suggestions.isEmpty()) {
                items(4) { SuggestionSkeletonCard() }
            } else if (suggestions.isEmpty()) {
                item(key = "docked_suggestions_empty") {
                    Text(
                        stringResource(Res.string.player_suggestions_empty),
                        style = MeloType.labelSmall,
                        color = DockedSecondaryTextColor,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                itemsIndexed(
                    suggestions,
                    key = { index, track -> "docked_sugg_${index}_${track.id}" },
                ) { _, track ->
                    val isCurrent = currentTrackId == track.id
                    DockedSuggestionRow(
                        track = track,
                        activeAccent = activeAccent,
                        isCurrent = isCurrent,
                        isPlaying = isPlaying && isCurrent,
                        onClick = { onPlaySuggestion(track) },
                        onAddClick = { onAddSuggestionToQueue(track) },
                        onOpenContextMenu =
                            onOpenContextMenu?.let { open -> { anchor -> open(track, false, anchor) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun DockedUpNextHeader(
    upNextCount: Int,
    isQueueExpanded: Boolean,
    activeAccent: Color,
    onToggleExpand: () -> Unit,
    onClearQueue: () -> Unit,
    onSaveAsPlaylist: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(Res.string.player_section_up_next),
                style = MeloType.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = DockedSecondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (upNextCount > 1) {
                Text(
                    text =
                        if (isQueueExpanded) {
                            stringResource(Res.string.player_queue_collapse)
                        } else {
                            stringResource(Res.string.player_queue_more_count, upNextCount - 1)
                        },
                    style = MeloType.labelSmall.copy(fontSize = 11.sp),
                    color = activeAccent,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onToggleExpand)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }

        Box {
            DockedTooltipIconButton(
                icon = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.a11y_queue_options),
                tint = DockedSecondaryTextColor,
                onClick = { menuExpanded = true },
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(MeloColors.surface1),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(Res.string.player_queue_save_playlist),
                            style = MeloType.body,
                            color = MeloColors.textPrimary,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onSaveAsPlaylist()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(Res.string.player_queue_clear),
                            style = MeloType.body,
                            color = MeloColors.textPrimary,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onClearQueue()
                    },
                )
            }
        }
    }
}

@Composable
private fun DockedSuggestionsHeader(
    isLoading: Boolean,
    activeAccent: Color,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(Res.string.player_section_suggestions),
                style = MeloType.labelSmall,
                fontWeight = FontWeight.Bold,
                color = DockedSecondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = activeAccent,
                )
            }
        }
        DockedTooltipIconButton(
            icon = Icons.Default.Refresh,
            contentDescription = stringResource(Res.string.a11y_refresh_suggestions),
            tint = DockedSecondaryTextColor,
            onClick = onRefresh,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderableCollectionItemScope.DockedUpNextRow(
    track: Track,
    isDragging: Boolean,
    showDragHandle: Boolean,
    activeAccent: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onOpenContextMenu: ((SongMenuAnchor?) -> Unit)?,
) {
    val haptic = LocalHapticFeedback.current
    val isDesktop = remember { getPlatform().type == PlatformType.DESKTOP }
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    val showSecondaryActions = !isDesktop || isHovered || isDragging
    var rowBounds by remember { mutableStateOf<Rect?>(null) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = DockedRowMinHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isDragging) activeAccent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                ).hoverable(hoverSource)
                .onGloballyPositioned { rowBounds = it.boundsInWindow() }
                .songMenuRightClick(
                    enabled = onOpenContextMenu != null,
                    onOpenMenu = { anchor -> onOpenContextMenu?.invoke(anchor) },
                ).combinedClickable(
                    onClick = onClick,
                    onLongClick =
                        onOpenContextMenu?.let { open ->
                            { open(rowBounds?.let { SongMenuAnchor.Bounds(it) }) }
                        },
                ).padding(
                    start = if (showDragHandle) 4.dp else 8.dp,
                    end = 4.dp,
                    top = 6.dp,
                    bottom = 6.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showDragHandle) {
            Box(
                modifier =
                    Modifier
                        .size(DockedTouchTargetSize)
                        .draggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = stringResource(Res.string.a11y_reorder),
                    tint =
                        when {
                            isDragging -> activeAccent
                            isHovered -> Color.White
                            else -> DockedSecondaryTextColor
                        },
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        MeloAsyncImage(
            url = track.artworkUrl,
            contentDescription = track.title,
            size = DockedRowCoverSize,
            shape = RoundedCornerShape(8.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MeloType.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (isDragging) activeAccent else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MeloType.labelSmall.copy(fontSize = 11.sp),
                color = DockedSecondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showSecondaryActions) {
            DockedTooltipIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(Res.string.a11y_remove_from_queue),
                tint = DockedSecondaryTextColor,
                iconSize = 16.dp,
                onClick = onRemove,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockedSuggestionRow(
    track: Track,
    activeAccent: Color,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    onOpenContextMenu: ((SongMenuAnchor?) -> Unit)?,
) {
    var rowBounds by remember { mutableStateOf<Rect?>(null) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = DockedRowMinHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isCurrent) activeAccent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                ).onGloballyPositioned { rowBounds = it.boundsInWindow() }
                .songMenuRightClick(
                    enabled = onOpenContextMenu != null,
                    onOpenMenu = { anchor -> onOpenContextMenu?.invoke(anchor) },
                ).combinedClickable(
                    onClick = onClick,
                    onLongClick =
                        onOpenContextMenu?.let { open ->
                            { open(rowBounds?.let { SongMenuAnchor.Bounds(it) }) }
                        },
                ).padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(DockedRowCoverSize),
            contentAlignment = Alignment.Center,
        ) {
            MeloAsyncImage(
                url = track.artworkUrl,
                contentDescription = track.title,
                size = DockedRowCoverSize,
                shape = RoundedCornerShape(8.dp),
            )
            if (isCurrent) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isPlaying) {
                        AnimatedEqualizerBars(accentColor = Color.White)
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MeloType.labelMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) activeAccent else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MeloType.labelSmall.copy(fontSize = 11.sp),
                color = DockedSecondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DockedTooltipIconButton(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            contentDescription = stringResource(Res.string.a11y_add_to_queue),
            tint = activeAccent,
            iconSize = 20.dp,
            onClick = onAddClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DockedTooltipIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip {
                Text(text = contentDescription, style = MeloType.labelSmall)
            }
        },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(DockedTouchTargetSize),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
