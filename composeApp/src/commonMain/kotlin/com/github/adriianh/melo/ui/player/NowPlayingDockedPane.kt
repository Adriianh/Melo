@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_close_panel
import com.github.adriianh.melo.resources.a11y_expand_player
import com.github.adriianh.melo.resources.a11y_favorite
import com.github.adriianh.melo.resources.common_action_undo
import com.github.adriianh.melo.resources.player_header_now_playing
import com.github.adriianh.melo.resources.player_queue_cleared_snackbar
import com.github.adriianh.melo.resources.player_tab_artist
import com.github.adriianh.melo.resources.player_tab_lyrics
import com.github.adriianh.melo.resources.player_tab_queue
import com.github.adriianh.melo.ui.components.controls.SegmentedControl
import com.github.adriianh.melo.ui.components.track.TrackInteractionContextMenu
import com.github.adriianh.melo.ui.components.track.rememberTrackInteraction
import com.github.adriianh.melo.ui.i18n.UiText
import com.github.adriianh.melo.ui.library.LibraryUiState
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.components.DockedArtistContent
import com.github.adriianh.melo.ui.player.components.DockedLyricsContent
import com.github.adriianh.melo.ui.player.components.DockedQueueContent
import com.github.adriianh.melo.ui.player.components.DockedTooltipIconButton
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val DockCornerRadius = 24.dp
private val DockDefaultWidth = 310.dp
private val DockSecondaryText = Color.White.copy(alpha = 0.70f)
private val DockBorderColor = Color.White.copy(alpha = 0.12f)
private val DockDarkSurfaceBase = Color(0xFF101218)

@Composable
fun DesktopNowPlayingDockedPane(
    modifier: Modifier = Modifier,
    onExpandToFullscreen: () -> Unit,
    onClose: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    selectedSection: PanelSection = PanelSection.QUEUE,
    onSectionChange: (PanelSection) -> Unit = {},
    viewModel: PlayerViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel(),
    getSettingsUseCase: GetSettingsUseCase = koinInject(),
) {
    val state by viewModel.stableUiState.collectAsState()
    val artistDetails by viewModel.artistDetails.collectAsState()
    val settings by getSettingsUseCase().collectAsState()
    val systemReducedMotion = rememberPrefersReducedMotion()
    val reducedMotion = systemReducedMotion || settings.reduceMotion
    val interaction = rememberTrackInteraction(libraryViewModel, queueViewModel)
    val activeAccent = interaction.resolveActiveAccent(MaterialTheme.colorScheme.primary)
    var contextMenuInQueue by remember { mutableStateOf(false) }

    val isLibraryNeeded =
        interaction.contextMenuTrack != null || !interaction.addToPlaylistTracks.isNullOrEmpty()
    val libraryState by remember(isLibraryNeeded) {
        if (isLibraryNeeded) libraryViewModel.uiState else flowOf(LibraryUiState())
    }.collectAsState(initial = LibraryUiState())

    TrackInteractionContextMenu(
        interaction = interaction,
        libraryState = libraryState,
        activeAccent = activeAccent,
        onArtistClick = onArtistClick,
        onAlbumWithArtistClick = { album, _ -> onAlbumClick(album) },
        inQueue = contextMenuInQueue,
    )

    DesktopNowPlayingDockedPaneContent(
        state = state,
        artistDetails = artistDetails,
        modifier = modifier,
        activeAccent = activeAccent,
        selectedSection = selectedSection,
        reducedMotion = reducedMotion,
        onSectionChange = onSectionChange,
        onExpandToFullscreen = onExpandToFullscreen,
        onClose = onClose,
        onArtistClick = onArtistClick,
        onToggleFavorite = { viewModel.toggleFavorite() },
        queueContent = { accent ->
            DockedQueueContent(
                state = state,
                activeAccent = accent,
                queueViewModel = queueViewModel,
                onRemoveFromQueue = { track ->
                    interaction.showRemovedFromQueueSnackbar(track, accent)
                },
                onClearQueue = {
                    val removed = queueViewModel.clearUpcomingQueue()
                    if (removed.isNotEmpty()) {
                        interaction.snackbar.show(
                            msg = UiText.Resource(Res.string.player_queue_cleared_snackbar),
                            vector = Icons.Default.Delete,
                            action = UiText.Resource(Res.string.common_action_undo),
                            actionColor = accent,
                            onAction = { removed.forEach(queueViewModel::addToQueue) },
                        )
                    }
                },
                onSaveQueueAsPlaylist = { tracks ->
                    interaction.openAddToPlaylist(tracks)
                },
                onOpenContextMenu = { track, inQueue, anchor ->
                    contextMenuInQueue = inQueue
                    interaction.openContextMenu(track, anchor)
                },
            )
        },
        lyricsContent = { accent ->
            DockedLyricsContent(
                state = state,
                activeAccent = accent,
                viewModel = viewModel,
                reducedMotion = reducedMotion,
            )
        },
        artistContent = { accent ->
            DockedArtistContent(
                state = state,
                artistDetails = artistDetails,
                activeAccent = accent,
                onArtistClick = onArtistClick,
                onAlbumClick = onAlbumClick,
                queueViewModel = queueViewModel,
                onOpenTrackContextMenu = { track, anchor ->
                    contextMenuInQueue = false
                    interaction.openContextMenu(track, anchor)
                },
            )
        },
    )
}

@Composable
fun DesktopNowPlayingDockedPaneContent(
    modifier: Modifier = Modifier,
    state: PlayerUiState,
    artistDetails: SearchResult.Artist?,
    activeAccent: Color =
        state.accentColor.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.primary,
    selectedSection: PanelSection = PanelSection.QUEUE,
    reducedMotion: Boolean = false,
    dockWidth: Dp = DockDefaultWidth,
    onSectionChange: (PanelSection) -> Unit = {},
    onExpandToFullscreen: () -> Unit = {},
    onClose: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    queueContent: @Composable (Color) -> Unit = {},
    lyricsContent: @Composable (Color) -> Unit = {},
    artistContent: @Composable (Color) -> Unit = {},
) {
    if (!state.hasTrack) return

    val dockShape = RoundedCornerShape(DockCornerRadius)
    val surfaceBackground =
        if (MeloColors.reduceTransparency) {
            MeloColors.surface1
        } else {
            DockDarkSurfaceBase.copy(alpha = 0.88f)
        }
    val borderColor = if (MeloColors.reduceTransparency) MeloColors.border else DockBorderColor

    Box(
        modifier =
            modifier
                .width(dockWidth)
                .fillMaxHeight()
                .padding(top = 12.dp, bottom = 12.dp, end = 12.dp)
                .clip(dockShape)
                .background(surfaceBackground)
                .background(activeAccent.copy(alpha = 0.05f))
                .border(1.dp, borderColor, dockShape),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DockedPaneHeader(
                onExpandToFullscreen = onExpandToFullscreen,
                onClose = onClose,
            )

            DockedPaneNowPlayingBlock(
                state = state,
                artistId = artistDetails?.id,
                activeAccent = activeAccent,
                isExpandedArtwork = selectedSection == PanelSection.QUEUE,
                reducedMotion = reducedMotion,
                onExpandToFullscreen = onExpandToFullscreen,
                onArtistClick = onArtistClick,
                onToggleFavorite = onToggleFavorite,
            )

            val queueLabel = stringResource(Res.string.player_tab_queue)
            val lyricsLabel = stringResource(Res.string.player_tab_lyrics)
            val artistLabel = stringResource(Res.string.player_tab_artist)

            SegmentedControl(
                options = PanelSection.entries,
                selected = selectedSection,
                onSelect = onSectionChange,
                label = {
                    when (it) {
                        PanelSection.QUEUE -> queueLabel
                        PanelSection.LYRICS -> lyricsLabel
                        PanelSection.ARTIST -> artistLabel
                    }
                },
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                when (selectedSection) {
                    PanelSection.QUEUE -> queueContent(activeAccent)
                    PanelSection.LYRICS -> lyricsContent(activeAccent)
                    PanelSection.ARTIST -> artistContent(activeAccent)
                }
            }
        }
    }
}

@Composable
private fun DockedPaneHeader(
    onExpandToFullscreen: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.player_header_now_playing),
            style = MeloType.labelSmall,
            letterSpacing = 1.2.sp,
            color = DockSecondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            DockedTooltipIconButton(
                icon = Icons.Default.OpenInFull,
                contentDescription = stringResource(Res.string.a11y_expand_player),
                tint = Color.White,
                iconSize = 17.dp,
                onClick = onExpandToFullscreen,
            )
            DockedTooltipIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(Res.string.a11y_close_panel),
                tint = DockSecondaryText,
                iconSize = 18.dp,
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun DockedPaneNowPlayingBlock(
    state: PlayerUiState,
    artistId: String?,
    activeAccent: Color,
    isExpandedArtwork: Boolean,
    reducedMotion: Boolean,
    onExpandToFullscreen: () -> Unit,
    onArtistClick: (String) -> Unit,
    onToggleFavorite: () -> Unit,
) {
    AnimatedContent(
        targetState = isExpandedArtwork,
        transitionSpec = {
            if (reducedMotion) {
                EnterTransition.None togetherWith ExitTransition.None using SizeTransform(clip = false)
            } else {
                fadeIn(tween(220, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(tween(160, easing = FastOutSlowInEasing)) using
                    SizeTransform(clip = false)
            }
        },
        label = "DockedArtworkCollapse",
    ) { expanded ->
        if (expanded) {
            DockedExpandedTrackHeader(
                state = state,
                artistId = artistId,
                activeAccent = activeAccent,
                onExpandToFullscreen = onExpandToFullscreen,
                onArtistClick = onArtistClick,
                onToggleFavorite = onToggleFavorite,
            )
        } else {
            DockedCompactTrackHeader(
                state = state,
                artistId = artistId,
                activeAccent = activeAccent,
                onExpandToFullscreen = onExpandToFullscreen,
                onArtistClick = onArtistClick,
                onToggleFavorite = onToggleFavorite,
            )
        }
    }
}

@Composable
private fun DockedExpandedTrackHeader(
    state: PlayerUiState,
    artistId: String?,
    activeAccent: Color,
    onExpandToFullscreen: () -> Unit,
    onArtistClick: (String) -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val artworkShape = RoundedCornerShape(16.dp)
        MeloAsyncImage(
            url = state.albumArt,
            contentDescription = stringResource(Res.string.a11y_expand_player),
            modifier =
                Modifier
                    .size(200.dp)
                    .shadow(
                        elevation = 14.dp,
                        shape = artworkShape,
                        ambientColor = Color.Black.copy(alpha = 0.45f),
                        spotColor = Color.Black.copy(alpha = 0.55f),
                    ).clip(artworkShape)
                    .clickable(onClick = onExpandToFullscreen),
            size = 200.dp,
            shape = artworkShape,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MeloType.titleMedium.copy(fontSize = 20.sp, lineHeight = 24.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.artist,
                    style = MeloType.body.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = DockSecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.clickable(enabled = artistId != null) {
                            artistId?.let(onArtistClick)
                        },
                )
            }

            DockedTooltipIconButton(
                icon = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(Res.string.a11y_favorite),
                tint = if (state.isFavorite) activeAccent else DockSecondaryText,
                iconSize = 22.dp,
                onClick = onToggleFavorite,
            )
        }
    }
}

@Composable
private fun DockedCompactTrackHeader(
    state: PlayerUiState,
    artistId: String?,
    activeAccent: Color,
    onExpandToFullscreen: () -> Unit,
    onArtistClick: (String) -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val thumbShape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MeloAsyncImage(
            url = state.albumArt,
            contentDescription = stringResource(Res.string.a11y_expand_player),
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(thumbShape)
                    .clickable(onClick = onExpandToFullscreen),
            size = 48.dp,
            shape = thumbShape,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                style = MeloType.titleMedium.copy(fontSize = 17.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.artist,
                style = MeloType.body.copy(fontSize = 13.sp, lineHeight = 17.sp),
                color = DockSecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.clickable(enabled = artistId != null) {
                        artistId?.let(onArtistClick)
                    },
            )
        }

        DockedTooltipIconButton(
            icon = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(Res.string.a11y_favorite),
            tint = if (state.isFavorite) activeAccent else DockSecondaryText,
            iconSize = 20.dp,
            onClick = onToggleFavorite,
        )
    }
}
