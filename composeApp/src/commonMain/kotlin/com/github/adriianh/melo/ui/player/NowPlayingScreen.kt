@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.github.adriianh.core.domain.model.ArtworkCornerStyle
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.FullscreenIdleHideDelay
import com.github.adriianh.core.domain.model.NowPlayingBackdropScrim
import com.github.adriianh.core.domain.model.NowPlayingBlurIntensity
import com.github.adriianh.core.domain.model.NowPlayingInitialPanel
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.common_action_undo
import com.github.adriianh.melo.resources.track_snackbar_added_to_likes
import com.github.adriianh.melo.resources.track_snackbar_added_to_playlist
import com.github.adriianh.melo.resources.track_snackbar_added_to_queue
import com.github.adriianh.melo.resources.track_snackbar_link_copied
import com.github.adriianh.melo.resources.track_snackbar_play_next
import com.github.adriianh.melo.resources.track_snackbar_removed_from_likes
import com.github.adriianh.melo.resources.track_snackbar_removed_from_queue
import com.github.adriianh.melo.ui.components.dialogs.AddToPlaylistSheet
import com.github.adriianh.melo.ui.components.feedback.MeloSnackbarState
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.ui.components.track.TrackContextMenu
import com.github.adriianh.melo.ui.components.track.rememberTrackInteraction
import com.github.adriianh.melo.ui.i18n.UiText
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.components.ExpandedSectionSheet
import com.github.adriianh.melo.ui.player.components.FullscreenPlayerActions
import com.github.adriianh.melo.ui.player.components.IdleBypassRegistry
import com.github.adriianh.melo.ui.player.components.LocalIdleBypassRegistry
import com.github.adriianh.melo.ui.player.components.NowPlayingFullscreenContent
import com.github.adriianh.melo.ui.player.components.NowPlayingLyricsConfig
import com.github.adriianh.melo.ui.player.components.NowPlayingQueueSection
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.buildTrackShareUrl
import com.github.adriianh.melo.util.getPlatform
import com.github.adriianh.melo.util.rememberMeloWindowSizeClass
import com.github.adriianh.melo.util.rememberPlatformShareHandler
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

private const val FULLSCREEN_DARK_SCRIM_ALPHA = 0.28f

enum class PanelSection {
    QUEUE,
    LYRICS,
    ARTIST,
}

@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier,
    onCollapse: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onAlbumWithArtistClick: (album: String, artist: String) -> Unit = { album, _ -> onAlbumClick(album) },
    onPlaylistClick: (String) -> Unit = {},
    titleBar: @Composable () -> Unit = {},
    blurEnabled: Boolean = true,
    blurIntensity: NowPlayingBlurIntensity = NowPlayingBlurIntensity.MEDIUM,
    backdropScrim: NowPlayingBackdropScrim = NowPlayingBackdropScrim.NORMAL,
    reduceMotion: Boolean = false,
    artworkCornerRadiusDp: Int = ArtworkCornerStyle.ROUNDED.radiusDp,
    initialPanel: NowPlayingInitialPanel = NowPlayingInitialPanel.AUTO,
    showRemainingTime: Boolean = false,
    onToggleShowRemainingTime: () -> Unit = {},
    lyricsConfig: NowPlayingLyricsConfig = NowPlayingLyricsConfig(),
    idleHideDelayMs: Long? = FullscreenIdleHideDelay.SECONDS_3.delayMs,
    keepScreenOn: Boolean = false,
    viewModel: PlayerViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.stableUiState.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val audioOutputs by viewModel.availableAudioOutputs.collectAsState()
    val selectedAudioOutputId by viewModel.selectedAudioOutputId.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val artistDetails by viewModel.artistDetails.collectAsState()
    val platform = remember { getPlatform() }
    val isDesktop = platform.type == PlatformType.DESKTOP
    val sizeClass = rememberMeloWindowSizeClass()
    val reducedMotion = reduceMotion || rememberPrefersReducedMotion()

    var showLyrics by remember(sizeClass.isExpandedOrMediumLandscape, initialPanel) {
        mutableStateOf(
            when (initialPanel) {
                NowPlayingInitialPanel.AUTO -> sizeClass.isExpandedOrMediumLandscape
                NowPlayingInitialPanel.ARTWORK -> false
                NowPlayingInitialPanel.LYRICS -> true
            },
        )
    }
    var showQueue by remember { mutableStateOf(false) }
    var predictiveBackProgress by remember { mutableStateOf<Float?>(null) }

    var lastActivityTick by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    val currentControlsVisible by rememberUpdatedState(controlsVisible)
    val idleBypassRegistry = remember { IdleBypassRegistry() }
    var isAudioOutputMenuOpen by remember { mutableStateOf(false) }

    val interaction = rememberTrackInteraction(libraryViewModel, queueViewModel)
    val activeAccent = MaterialTheme.colorScheme.primary
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val rootFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (isDesktop) {
            runCatching { rootFocusRequester.requestFocus() }
        }
    }

    KeepScreenOnEffect(enabled = keepScreenOn)

    val keepControlsVisible =
        idleHideDelayMs == null ||
            showQueue ||
            isAudioOutputMenuOpen ||
            interaction.contextMenuTrack != null ||
            interaction.addToPlaylistTrack != null

    LaunchedEffect(lastActivityTick, keepControlsVisible, idleHideDelayMs) {
        controlsVisible = true
        if (!keepControlsVisible) {
            delay(idleHideDelayMs.milliseconds)
            controlsVisible = false
        }
    }

    PlayerPredictiveBackHandler(
        enabled = true,
        onProgress = { progress -> predictiveBackProgress = progress },
        onBack = {
            if (showQueue) {
                showQueue = false
            } else {
                onCollapse()
            }
        },
    )

    val sheetSnackbarState = remember { MeloSnackbarState() }
    var contextMenuInQueue by remember { mutableStateOf(false) }

    val isTrackLiked: (Track) -> Boolean =
        remember(libraryState.likedSongs) {
            { track ->
                val rawId = track.sourceId ?: track.id.removePrefix("piped:")
                libraryState.likedSongs.any {
                    it.id == track.id ||
                        (!it.sourceId.isNullOrBlank() && it.sourceId == track.sourceId) ||
                        (rawId.isNotBlank() && (it.id.removePrefix("piped:") == rawId || it.sourceId == rawId))
                }
            }
        }

    val currentTrackIsLiked =
        remember(state.currentTrack, state.isFavorite, libraryState.likedSongs) {
            val track = state.currentTrack
            track != null && (state.isFavorite || isTrackLiked(track))
        }
    val effectiveState =
        remember(state, currentTrackIsLiked) {
            state.copy(isFavorite = currentTrackIsLiked)
        }

    if (interaction.contextMenuTrack != null) {
        val track = interaction.contextMenuTrack!!
        val isLiked = isTrackLiked(track)
        val isDownloaded =
            libraryState.downloadedTracks.any { it.track.id == track.id && it.downloadType == DownloadType.MANUAL }
        val isDownloading = libraryState.activeDownloads.containsKey(track.id)
        val downloadProgress = libraryState.activeDownloads[track.id]
        val isModalSheetOpen = showQueue && !sizeClass.isExpandedOrMediumLandscape
        val targetSnackbar =
            if (contextMenuInQueue && isModalSheetOpen) sheetSnackbarState else interaction.snackbar
        val shareHandler =
            rememberPlatformShareHandler {
                targetSnackbar.show(
                    msg = UiText.Resource(Res.string.track_snackbar_link_copied),
                    actionColor = activeAccent,
                )
            }
        val shareUrl = remember(track.id) { buildTrackShareUrl(track) }
        val containingPlaylistIds by remember(track.id) {
            libraryViewModel.getPlaylistIdsForTrack(track.id)
        }.collectAsState(initial = emptySet())

        TrackContextMenu(
            track = track,
            onDismissRequest = interaction::dismissContextMenu,
            onPlayNext = {
                val insertIndex = queueViewModel.queueState.value.currentIndex + 1
                queueViewModel.insertTrackNext(track)
                targetSnackbar.show(
                    msg = UiText.Resource(Res.string.track_snackbar_play_next),
                    vector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    action = UiText.Resource(Res.string.common_action_undo),
                    actionColor = activeAccent,
                    onAction = { queueViewModel.removeTrackAt(insertIndex) },
                )
                interaction.dismissContextMenu()
            },
            onAddToQueue = {
                queueViewModel.addToQueue(track)
                targetSnackbar.show(
                    msg = UiText.Resource(Res.string.track_snackbar_added_to_queue),
                    vector = Icons.AutoMirrored.Filled.QueueMusic,
                    action = UiText.Resource(Res.string.common_action_undo),
                    actionColor = activeAccent,
                    onAction = { queueViewModel.removeTrackFromQueue(track) },
                )
                interaction.dismissContextMenu()
            },
            onRemoveFromQueue = {
                val removedIndex = queueViewModel.removeTrackFromQueue(track)
                if (removedIndex != -1) {
                    targetSnackbar.show(
                        msg = UiText.Resource(Res.string.track_snackbar_removed_from_queue),
                        vector = Icons.Default.Delete,
                        action = UiText.Resource(Res.string.common_action_undo),
                        actionColor = activeAccent,
                        onAction = { queueViewModel.insertTrackAt(track, removedIndex) },
                    )
                }
                interaction.dismissContextMenu()
            },
            inQueue = contextMenuInQueue,
            onToggleLike = {
                libraryViewModel.toggleLike(track.id, !isLiked)
                targetSnackbar.show(
                    msg =
                        UiText.Resource(
                            if (isLiked) {
                                Res.string.track_snackbar_removed_from_likes
                            } else {
                                Res.string.track_snackbar_added_to_likes
                            },
                        ),
                    vector = if (isLiked) Icons.Default.HeartBroken else Icons.Default.Favorite,
                    action = UiText.Resource(Res.string.common_action_undo),
                    actionColor = activeAccent,
                    onAction = { libraryViewModel.toggleLike(track.id, isLiked) },
                )
                interaction.dismissContextMenu()
            },
            isLiked = isLiked,
            onAddToPlaylist = {
                interaction.openAddToPlaylist(track)
                interaction.dismissContextMenu()
            },
            onGoToArtist = {
                interaction.dismissContextMenu()
                onArtistClick(track.artist)
            },
            onGoToAlbum = {
                interaction.dismissContextMenu()
                if (track.album.isNotBlank()) onAlbumWithArtistClick(track.album, track.artist)
            },
            onShare =
                shareUrl?.let { url ->
                    {
                        shareHandler(url)
                        interaction.dismissContextMenu()
                    }
                },
            onDownload = {
                interaction.downloadTrack(track, activeAccent)
                interaction.dismissContextMenu()
            },
            onDeleteDownload = {
                interaction.deleteDownloadedTrack(track.id, activeAccent)
                interaction.dismissContextMenu()
            },
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            anchor = interaction.contextMenuAnchor,
            playlists = libraryState.customPlaylists,
            containingPlaylistIds = containingPlaylistIds,
            onSelectPlaylist = { playlist ->
                libraryViewModel.addTrackToPlaylist(playlist.id, track) {
                    targetSnackbar.show(
                        msg = UiText.Resource(Res.string.track_snackbar_added_to_playlist, listOf(playlist.name)),
                        vector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        actionColor = activeAccent,
                    )
                }
            },
            onCreatePlaylistAndAdd = { name ->
                libraryViewModel.createPlaylist(name) { id ->
                    libraryViewModel.addTrackToPlaylist(id, track) {
                        targetSnackbar.show(
                            msg = UiText.Resource(Res.string.track_snackbar_added_to_playlist, listOf(name)),
                            vector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            actionColor = activeAccent,
                        )
                    }
                }
            },
        )
    }

    val onMoreClick: (Track, Boolean) -> Unit = { track, inQueue ->
        interaction.openContextMenu(track)
        contextMenuInQueue = inQueue
    }
    val onOpenContextMenu: (Track, Boolean, SongMenuAnchor?) -> Unit = { track, inQueue, anchor ->
        interaction.openContextMenu(track, anchor)
        contextMenuInQueue = inQueue
    }
    val onSwipeQueueItem: (Int) -> Unit = { trackIndex ->
        val removedTrack = queueViewModel.removeTrackAt(trackIndex)
        if (removedTrack != null) {
            interaction.snackbar.show(
                msg = UiText.Resource(Res.string.track_snackbar_removed_from_queue),
                vector = Icons.Default.Delete,
                action = UiText.Resource(Res.string.common_action_undo),
                actionColor = activeAccent,
                onAction = { queueViewModel.insertTrackAt(removedTrack, trackIndex) },
            )
        }
    }
    val onSwipeSuggestionItem: (Track) -> Unit = { track ->
        interaction.showAddedToQueueSnackbar(track, activeAccent)
    }
    val onSwipeRight: (Track) -> Unit = { track ->
        val trackIsLiked = isTrackLiked(track)
        interaction.showToggledLikeSnackbar(track, trackIsLiked, activeAccent)
    }

    val actions =
        FullscreenPlayerActions(
            onCollapse = onCollapse,
            onMoreClick =
                effectiveState.currentTrack?.let { currentTrack ->
                    { anchor -> onOpenContextMenu(currentTrack, false, anchor) }
                },
            onArtistClick = {
                val target = artistDetails?.id ?: effectiveState.currentTrack?.artist
                if (!target.isNullOrBlank()) {
                    onCollapse()
                    onArtistClick(target)
                }
            },
            onAlbumClick = {
                val album = effectiveState.currentTrack?.album
                val artist = effectiveState.currentTrack?.artist.orEmpty()
                if (!album.isNullOrBlank()) {
                    onCollapse()
                    onAlbumWithArtistClick(album, artist)
                }
            },
            onToggleFavorite = viewModel::toggleFavorite,
            onSeekTo = viewModel::seekTo,
            onTogglePlayPause = viewModel::togglePlayPause,
            onPlayNext = viewModel::playNext,
            onPlayPrevious = viewModel::playPrevious,
            onToggleShuffle = viewModel::toggleShuffle,
            onToggleRepeat = viewModel::toggleRepeat,
            onVolumeChange = viewModel::setVolume,
            onToggleMute = viewModel::toggleMute,
            onToggleLyrics = {
                if (sizeClass.isExpandedOrMediumLandscape) {
                    if (showQueue) {
                        showQueue = false
                        showLyrics = true
                    } else {
                        showLyrics = !showLyrics
                    }
                } else {
                    showLyrics = !showLyrics
                }
            },
            onToggleQueue = { showQueue = !showQueue },
            onCloseQueue = { showQueue = false },
            onToggleTranslation = viewModel::toggleTranslation,
            onSelectLanguage = viewModel::setTargetLanguage,
            onToggleShowRemainingTime = onToggleShowRemainingTime,
            onRefreshAudioOutputs = viewModel::refreshAudioOutputs,
            onSelectAudioOutput = viewModel::selectAudioOutput,
            onAudioOutputMenuOpenChanged = { isAudioOutputMenuOpen = it },
        )

    val backProgress = if (!showQueue && !reducedMotion) (predictiveBackProgress ?: 0f) else 0f

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (backProgress > 0f) {
                        val s = 1f - 0.04f * backProgress
                        scaleX = s
                        scaleY = s
                        translationY = 48f * backProgress
                    }
                }.focusRequester(rootFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        lastActivityTick++
                        if (event.key == Key.Escape) {
                            if (showQueue) {
                                showQueue = false
                            } else {
                                onCollapse()
                            }
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }.idleCursorHidden(isDesktop && !controlsVisible)
                .onGloballyPositioned { idleBypassRegistry.rootCoordinates = it }
                .pointerInput(isDesktop) {
                    var lastPosition: Offset? = null
                    var consumingWakeGesture = false
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val position = event.changes.firstOrNull()?.position
                            if (!isDesktop) {
                                if (event.type == PointerEventType.Press) {
                                    val bypassed = !currentControlsVisible && idleBypassRegistry.contains(position)
                                    if (!bypassed) {
                                        if (!currentControlsVisible) {
                                            consumingWakeGesture = true
                                            event.changes.forEach { it.consume() }
                                            controlsVisible = true
                                        }
                                        lastActivityTick++
                                    }
                                } else if (consumingWakeGesture) {
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) {
                                        consumingWakeGesture = false
                                    }
                                }
                            } else {
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        if (lastPosition == null) {
                                            lastPosition = position
                                        }
                                    }

                                    PointerEventType.Press -> {
                                        lastPosition = position
                                        lastActivityTick++
                                    }

                                    PointerEventType.Move -> {
                                        val prev = lastPosition
                                        if (prev == null) {
                                            lastPosition = position
                                        } else if (position != null && (position - prev).getDistanceSquared() > 4f) {
                                            lastPosition = position
                                            lastActivityTick++
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
    ) {
        NowPlayingBackground(
            artworkUrl = effectiveState.albumArt,
            blurEnabled = blurEnabled,
            blurIntensity = blurIntensity,
            backdropScrim = backdropScrim,
            accentColor = activeAccent,
            extraDarkScrimAlpha = FULLSCREEN_DARK_SCRIM_ALPHA,
        )

        val titleBarVisible = !isDesktop || controlsVisible
        val titleBarAlpha by animateFloatAsState(
            targetValue = if (titleBarVisible) 1f else 0f,
            animationSpec =
                tween(
                    durationMillis =
                        when {
                            reducedMotion -> 0
                            titleBarVisible -> 220
                            else -> 650
                        },
                    easing = FastOutSlowInEasing,
                ),
            label = "FullscreenTitleBarAlpha",
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.Top,
        ) {
            Box(modifier = Modifier.alpha(titleBarAlpha)) {
                titleBar()
            }

            CompositionLocalProvider(LocalIdleBypassRegistry provides idleBypassRegistry) {
                NowPlayingFullscreenContent(
                    state = effectiveState,
                    positionMs = effectiveState.progressMs,
                    progressFlow = viewModel.progressState,
                    volume = volume,
                    activeAccent = activeAccent,
                    sizeClass = sizeClass,
                    isDesktop = isDesktop,
                    reducedMotion = reducedMotion,
                    showLyrics = showLyrics,
                    showQueue = showQueue,
                    controlsVisible = controlsVisible,
                    actions = actions,
                    artworkCornerRadiusDp = artworkCornerRadiusDp,
                    showRemainingTime = showRemainingTime,
                    audioOutputs = audioOutputs,
                    selectedAudioOutputId = selectedAudioOutputId,
                    lyricsConfig = lyricsConfig,
                    queueSidePanelContent = {
                        NowPlayingQueueSection(
                            state = effectiveState,
                            activeAccent = activeAccent,
                            onMoreClick = onMoreClick,
                            onOpenContextMenu = onOpenContextMenu,
                            onSwipeQueueItem = onSwipeQueueItem,
                            onSwipeSuggestionItem = onSwipeSuggestionItem,
                            onPlayNextSuggestionItem = { track ->
                                interaction.showPlayNextSnackbar(track, activeAccent)
                            },
                            onSwipeRight = onSwipeRight,
                            isLiked = isTrackLiked,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (!sizeClass.isExpandedOrMediumLandscape) {
            ExpandedSectionSheet(
                section = if (showQueue) PanelSection.QUEUE else null,
                state = effectiveState,
                artistDetails = artistDetails,
                activeAccent = activeAccent,
                snackbarState = sheetSnackbarState,
                onDismiss = { showQueue = false },
                onArtistClick = { id ->
                    showQueue = false
                    onCollapse()
                    onArtistClick(id)
                },
                onAlbumClick = { id ->
                    showQueue = false
                    onCollapse()
                    onAlbumClick(id)
                },
                onPlaylistClick = { id ->
                    showQueue = false
                    onCollapse()
                    onPlaylistClick(id)
                },
                onMoreClick = onMoreClick,
                onOpenContextMenu = onOpenContextMenu,
                isLiked = isTrackLiked,
            )
        }

        val playlistTrack = interaction.addToPlaylistTrack
        if (playlistTrack != null) {
            val containingPlaylistIds by remember(playlistTrack.id) {
                libraryViewModel.getPlaylistIdsForTrack(playlistTrack.id)
            }.collectAsState(initial = emptySet())

            AddToPlaylistSheet(
                track = playlistTrack,
                playlists = libraryState.customPlaylists,
                containingPlaylistIds = containingPlaylistIds,
                onDismissRequest = interaction::dismissAddToPlaylist,
                onSelectPlaylist = { playlist ->
                    libraryViewModel.addTrackToPlaylist(playlist.id, playlistTrack) {
                        interaction.showAddedToPlaylistSnackbar(playlist.name, activeAccent)
                    }
                },
                onCreatePlaylistAndAdd = { name ->
                    libraryViewModel.createPlaylist(name) { id ->
                        libraryViewModel.addTrackToPlaylist(id, playlistTrack) {
                            interaction.showAddedToPlaylistSnackbar(name, activeAccent)
                        }
                    }
                },
            )
        }
    }
}
