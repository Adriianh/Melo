@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.library.ToggleLikeAlbumUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikePlaylistUseCase
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.search_type_artist
import com.github.adriianh.melo.resources.track_snackbar_added_to_queue
import com.github.adriianh.melo.resources.track_snackbar_link_copied
import com.github.adriianh.melo.resources.track_snackbar_play_next
import com.github.adriianh.melo.ui.LocalSelectionMode
import com.github.adriianh.melo.ui.components.controls.MeloSearchBar
import com.github.adriianh.melo.ui.components.feedback.MeloErrorState
import com.github.adriianh.melo.ui.components.track.BatchSelectionBottomBar
import com.github.adriianh.melo.ui.components.track.EntityContextMenu
import com.github.adriianh.melo.ui.components.track.EntityMenuContext
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.ui.components.track.TrackInteractionContextMenu
import com.github.adriianh.melo.ui.components.track.buildAlbumOrPlaylistMenuEntries
import com.github.adriianh.melo.ui.components.track.buildArtistMenuEntries
import com.github.adriianh.melo.ui.components.track.rememberTrackInteraction
import com.github.adriianh.melo.ui.home.components.HomeContent
import com.github.adriianh.melo.ui.home.components.HomeSearchResultsView
import com.github.adriianh.melo.ui.home.components.HomeSkeletonLoading
import com.github.adriianh.melo.ui.i18n.UiText
import com.github.adriianh.melo.ui.library.LibraryUiState
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.buildEntityShareUrl
import com.github.adriianh.melo.util.getPlatform
import com.github.adriianh.melo.util.rememberPlatformShareHandler
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Represents the four mutually exclusive display states of the home feed. */
private enum class HomeFeedState { Loading, Content, Error, Search }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onAlbumClick: (id: String, title: String, artwork: String?, author: String) -> Unit = { _, _, _, _ -> },
    onPlaylistClick: (id: String, title: String, artwork: String?, author: String) -> Unit = { _, _, _, _ -> },
    onArtistClick: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: HomeViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel(),
) {
    val platform = remember { getPlatform() }
    val uiState by viewModel.uiState.collectAsState()
    val currentAccent = MaterialTheme.colorScheme.primary
    val interaction = rememberTrackInteraction(libraryViewModel, queueViewModel)
    val activeAccent = interaction.resolveActiveAccent(currentAccent)
    val entityActionScope = rememberCoroutineScope()
    var contextMenuEntity by remember { mutableStateOf<Pair<SearchResult, SongMenuAnchor?>?>(null) }

    val isLibraryNeeded = interaction.contextMenuTrack != null ||
            contextMenuEntity != null ||
            !interaction.addToPlaylistTracks.isNullOrEmpty() ||
            uiState.searchQuery.isNotBlank()
    val libraryState by remember(isLibraryNeeded) {
        if (isLibraryNeeded) {
            libraryViewModel.uiState
        } else {
            flowOf(LibraryUiState())
        }
    }.collectAsState(initial = LibraryUiState())

    var selectedTrackIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedTrackIds.isNotEmpty()

    val selectionModeState = LocalSelectionMode.current
    LaunchedEffect(isSelectionMode) {
        selectionModeState.value = isSelectionMode
    }
    DisposableEffect(Unit) {
        onDispose {
            selectionModeState.value = false
        }
    }

    val visibleSongs =
        remember(isSelectionMode, uiState.searchQuery, uiState.searchResults, uiState.sections) {
            if (!isSelectionMode) {
                emptyList()
            } else if (uiState.searchQuery.isNotBlank()) {
                uiState.searchResults
            } else {
                uiState.sections
                    .filter { it.type == HomeSectionType.SONGS }
                    .flatMap { it.items }
                    .filterIsInstance<SearchResult.Song>()
                    .map { it.track }
            }
        }

    LaunchedEffect(uiState.searchQuery, uiState.selectedChip) {
        selectedTrackIds = emptySet()
    }

    TrackInteractionContextMenu(
        interaction = interaction,
        libraryState = libraryState,
        activeAccent = activeAccent,
        onArtistClick = onArtistClick,
        onAlbumWithArtistClick = { album, artist -> onAlbumClick(album, album, null, artist) },
    )

    HomeEntityContextMenuHost(
        entityPair = contextMenuEntity,
        libraryState = libraryState,
        activeAccent = activeAccent,
        queueViewModel = queueViewModel,
        libraryViewModel = libraryViewModel,
        interaction = interaction,
        scope = entityActionScope,
        onArtistClick = onArtistClick,
        onDismiss = { contextMenuEntity = null },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        MeloSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onClear = viewModel::clearSearch,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            val feedState = when {
                uiState.searchQuery.isNotBlank() -> HomeFeedState.Search
                uiState.isLoading -> HomeFeedState.Loading
                uiState.error != null -> HomeFeedState.Error
                else -> HomeFeedState.Content
            }

            val feedContent: @Composable () -> Unit = {
                AnimatedContent(
                    targetState = feedState,
                    transitionSpec = {
                        val isLoadingToContent = initialState == HomeFeedState.Loading &&
                            targetState == HomeFeedState.Content
                        if (isLoadingToContent) {
                            (fadeIn(animationSpec = tween(220)) togetherWith fadeOut(
                                animationSpec = tween(
                                    220
                                )
                            ))
                        } else {
                            (fadeIn(animationSpec = tween(150)) togetherWith fadeOut(
                                animationSpec = tween(
                                    150
                                )
                            ))
                        }
                    },
                    label = "HomeFeedState"
                ) { state ->
                    when (state) {
                        HomeFeedState.Search -> HomeSearchResultsView(
                            activeAccent = activeAccent,
                            isSearching = uiState.isSearching,
                            results = uiState.searchResults,
                            likedSongs = libraryState.likedSongs,
                            queueViewModel = queueViewModel,
                            onMoreClick = { interaction.openContextMenu(it) },
                            interaction = interaction,
                            paddingValues = paddingValues,
                            isSelectionMode = isSelectionMode,
                            selectedTrackIds = selectedTrackIds,
                            onToggleSelectTrack = { track ->
                                selectedTrackIds =
                                    if (track.id in selectedTrackIds) selectedTrackIds - track.id else selectedTrackIds + track.id
                            },
                            onTrackLongClick = { track ->
                                if (!isSelectionMode) selectedTrackIds = setOf(track.id)
                            }
                        )

                        HomeFeedState.Loading -> HomeSkeletonLoading(
                            chips = uiState.chips,
                            onChipClick = viewModel::toggleChip,
                            selectedChip = uiState.selectedChip
                        )

                        HomeFeedState.Error -> MeloErrorState(
                            uiState.error,
                            onRetry = viewModel::loadFeed
                        )

                        HomeFeedState.Content -> HomeContent(
                            uiState = uiState,
                            onChipClick = viewModel::toggleChip,
                            onLoadMore = viewModel::loadMore,
                            onAlbumClick = onAlbumClick,
                            onPlaylistClick = onPlaylistClick,
                            onArtistClick = onArtistClick,
                            queueViewModel = queueViewModel,
                            paddingValues = paddingValues,
                            onMoreClick = { interaction.openContextMenu(it) },
                            onOpenTrackContextMenu = { track, anchor -> interaction.openContextMenu(track, anchor) },
                            onOpenEntityContextMenu = { entity, anchor ->
                                if (entity is SearchResult.Song) {
                                    interaction.openContextMenu(entity.track, anchor)
                                } else {
                                    contextMenuEntity = entity to anchor
                                }
                            },
                            isSelectionMode = isSelectionMode,
                            selectedTrackIds = selectedTrackIds,
                            onToggleSelectTrack = { track ->
                                selectedTrackIds =
                                    if (track.id in selectedTrackIds) selectedTrackIds - track.id else selectedTrackIds + track.id
                            },
                            onTrackLongClick = { track ->
                                if (!isSelectionMode) selectedTrackIds = setOf(track.id)
                            }
                        )
                    }
                }
            }

            if (platform.type == PlatformType.DESKTOP) {
                Box(modifier = Modifier.fillMaxSize()) {
                    feedContent()
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = {
                        if (uiState.searchQuery.isBlank()) {
                            viewModel.refresh()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    feedContent()
                }
            }

            BatchSelectionBottomBar(
                isVisible = isSelectionMode,
                selectedCount = selectedTrackIds.size,
                totalCount = visibleSongs.size,
                onClearSelection = { selectedTrackIds = emptySet() },
                onSelectAllToggle = {
                    selectedTrackIds =
                        if (selectedTrackIds.size == visibleSongs.size) emptySet() else visibleSongs.map { it.id }
                            .toSet()
                },
                onAddToPlaylist = {
                    val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                    interaction.openAddToPlaylist(selTracks)
                    selectedTrackIds = emptySet()
                },
                onAddToQueue = {
                    val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                    queueViewModel.addAllToQueue(selTracks)
                    interaction.snackbar.show(
                        msg = if (selTracks.size == 1) "1 canción añadida a la cola" else "${selTracks.size} canciones añadidas a la cola",
                        actionColor = activeAccent
                    )
                    selectedTrackIds = emptySet()
                },
                onDownload = {
                    val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                    selTracks.forEach { libraryViewModel.downloadTrack(it) }
                    interaction.snackbar.show(
                        msg = "Descargando ${selTracks.size} canciones...",
                        actionColor = activeAccent
                    )
                    selectedTrackIds = emptySet()
                },
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun HomeEntityContextMenuHost(
    entityPair: Pair<SearchResult, SongMenuAnchor?>?,
    libraryState: LibraryUiState,
    activeAccent: androidx.compose.ui.graphics.Color,
    queueViewModel: QueueViewModel,
    libraryViewModel: LibraryViewModel,
    interaction: com.github.adriianh.melo.ui.components.track.TrackInteractionState,
    scope: kotlinx.coroutines.CoroutineScope,
    onArtistClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val (entity, anchor) = entityPair ?: return
    if (entity is SearchResult.Song) return
    val getEntityDetailsUseCase: GetEntityDetailsUseCase = koinInject()
    val toggleLikeAlbumUseCase: ToggleLikeAlbumUseCase = koinInject()
    val toggleLikePlaylistUseCase: ToggleLikePlaylistUseCase = koinInject()
    val shareHandler =
        rememberPlatformShareHandler {
            interaction.snackbar.show(
                msg = UiText.Resource(Res.string.track_snackbar_link_copied),
                actionColor = activeAccent,
            )
        }
    val onShareAction =
        remember(entity, shareHandler) {
            buildEntityShareUrl(entity)?.let { url -> { shareHandler(url) } }
        }

    val (title, subtitle, artworkUrl) =
        when (entity) {
            is SearchResult.Album -> Triple(entity.title, entity.author, entity.artworkUrl)
            is SearchResult.Playlist -> Triple(entity.title, entity.author, entity.artworkUrl)
            is SearchResult.Artist ->
                Triple(entity.name, stringResource(Res.string.search_type_artist), entity.artworkUrl)
        }
    val entries =
        rememberHomeEntityMenuEntries(
            entity = entity,
            libraryState = libraryState,
            activeAccent = activeAccent,
            queueViewModel = queueViewModel,
            libraryViewModel = libraryViewModel,
            interaction = interaction,
            getEntityDetailsUseCase = getEntityDetailsUseCase,
            toggleLikeAlbumUseCase = toggleLikeAlbumUseCase,
            toggleLikePlaylistUseCase = toggleLikePlaylistUseCase,
            scope = scope,
            onShare = onShareAction,
            onArtistClick = onArtistClick,
        )

    EntityContextMenu(
        title = title,
        subtitle = subtitle,
        artworkUrl = artworkUrl,
        entries = entries,
        anchor = anchor,
        onDismissRequest = onDismiss,
    )
}

private fun rememberHomeEntityMenuEntries(
    entity: SearchResult,
    libraryState: LibraryUiState,
    activeAccent: androidx.compose.ui.graphics.Color,
    queueViewModel: QueueViewModel,
    libraryViewModel: LibraryViewModel,
    interaction: com.github.adriianh.melo.ui.components.track.TrackInteractionState,
    getEntityDetailsUseCase: GetEntityDetailsUseCase,
    toggleLikeAlbumUseCase: ToggleLikeAlbumUseCase,
    toggleLikePlaylistUseCase: ToggleLikePlaylistUseCase,
    scope: kotlinx.coroutines.CoroutineScope,
    onShare: (() -> Unit)?,
    onArtistClick: (String) -> Unit,
): List<com.github.adriianh.melo.ui.components.track.SongMenuEntry> {
    fun launchTracks(action: (List<com.github.adriianh.core.domain.model.Track>) -> Unit) {
        scope.launch {
            val tracks = fetchHomeEntityTracks(entity, getEntityDetailsUseCase)
            if (tracks.isNotEmpty()) action(tracks)
        }
    }

    if (entity is SearchResult.Artist) {
        return buildArtistMenuEntries(
            EntityMenuContext(
                onPlay = { launchTracks { queueViewModel.playTracks(it) } },
                onStartRadio = { queueViewModel.startArtistRadio(entity.name) },
                onGoToArtist = { onArtistClick(entity.id) },
                onShare = onShare,
            ),
        )
    }

    val isAlbum = entity is SearchResult.Album
    val isSaved =
        if (entity is SearchResult.Album) libraryState.albums.any { it.id == entity.id }
        else libraryState.playlists.any { it.id == (entity as SearchResult.Playlist).id }
    val author = if (entity is SearchResult.Album) entity.author else ""
    val hasArtist = isAlbum && author.isNotBlank() && !author.equals("Unknown", ignoreCase = true)

    return buildAlbumOrPlaylistMenuEntries(
        isAlbum = isAlbum,
        author = author,
        context =
            EntityMenuContext(
                isSaved = isSaved,
                onPlay = { launchTracks { queueViewModel.playTracks(it) } },
                onShuffle = { launchTracks { queueViewModel.playShuffled(it) } },
                onPlayNext = {
                    launchTracks { tracks ->
                        tracks.asReversed().forEach { queueViewModel.insertTrackNext(it) }
                        interaction.snackbar.show(
                            UiText.Resource(Res.string.track_snackbar_play_next),
                            actionColor = activeAccent,
                        )
                    }
                },
                onAddToQueue = {
                    launchTracks { tracks ->
                        queueViewModel.addAllToQueue(tracks)
                        interaction.snackbar.show(
                            UiText.Resource(Res.string.track_snackbar_added_to_queue),
                            actionColor = activeAccent,
                        )
                    }
                },
                onToggleSave = {
                    scope.launch {
                        toggleHomeEntitySave(
                            entity = entity,
                            isSaved = isSaved,
                            toggleLikeAlbumUseCase = toggleLikeAlbumUseCase,
                            toggleLikePlaylistUseCase = toggleLikePlaylistUseCase,
                            libraryViewModel = libraryViewModel,
                        )
                    }
                },
                onGoToArtist = if (hasArtist) ({ onArtistClick(author) }) else null,
                onShare = onShare,
            ),
    )
}

private suspend fun toggleHomeEntitySave(
    entity: SearchResult,
    isSaved: Boolean,
    toggleLikeAlbumUseCase: ToggleLikeAlbumUseCase,
    toggleLikePlaylistUseCase: ToggleLikePlaylistUseCase,
    libraryViewModel: LibraryViewModel,
) {
    if (entity is SearchResult.Album) {
        toggleLikeAlbumUseCase(entity.id, !isSaved)
        libraryViewModel.refreshAlbums()
    } else if (entity is SearchResult.Playlist) {
        toggleLikePlaylistUseCase(entity.id, !isSaved)
        libraryViewModel.refreshPlaylists()
    }
}

private suspend fun fetchHomeEntityTracks(
    entity: SearchResult,
    getEntityDetailsUseCase: GetEntityDetailsUseCase,
): List<com.github.adriianh.core.domain.model.Track> =
    when (val details = runCatching { getEntityDetailsUseCase(entity) }.getOrNull()) {
        is SearchResult.Album -> details.songs.orEmpty()
        is SearchResult.Playlist -> details.songs.orEmpty()
        is SearchResult.Artist -> details.topSongs.orEmpty()
        else -> emptyList()
    }
