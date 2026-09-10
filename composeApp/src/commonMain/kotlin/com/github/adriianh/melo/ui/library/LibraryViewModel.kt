package com.github.adriianh.melo.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.manager.DownloadManager
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.repository.LibraryUpdateEvent
import com.github.adriianh.core.domain.usecase.library.AddTrackToPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.AddTracksToPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.CreatePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.DeletePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.GetAccountProfileUseCase
import com.github.adriianh.core.domain.usecase.library.GetLikedSongsUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistIdsForTrackUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetRemoteHistoryUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserAlbumsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserArtistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.ObserveLibraryUpdatesUseCase
import com.github.adriianh.core.domain.usecase.library.RenamePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeTrackUseCase
import com.github.adriianh.core.domain.usecase.offline.DeleteDownloadedTrackUseCase
import com.github.adriianh.core.domain.usecase.offline.EnrichLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.GetOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.ScanLocalTracksUseCase
import com.github.adriianh.core.domain.usecase.offline.SyncOfflineTracksUseCase
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.core.util.MeloDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

enum class LibraryTab(val label: String) {
    PLAYLISTS("Playlists"),
    LIKED("Me Gusta"),
    DOWNLOADS("Descargas"),
    LOCAL("Archivos locales"),
    ARTISTS("Artistas"),
    ALBUMS("Álbumes"),
    HISTORY("Historial")
}

enum class LibrarySortOrder(val label: String) {
    RECENTLY_ADDED("Añadido recientemente"),
    TITLE_A_Z("Título (A-Z)"),
    ARTIST_A_Z("Artista (A-Z)"),
    DURATION("Duración")
}

enum class LibraryViewMode {
    GRID,
    COMPACT_LIST
}

enum class LibraryCategory(
    val label: String,
    val tabs: List<LibraryTab>
) {
    COLLECTION(
        "Colección",
        listOf(LibraryTab.PLAYLISTS, LibraryTab.LIKED, LibraryTab.ALBUMS, LibraryTab.ARTISTS)
    ),
    DEVICE("Dispositivo", listOf(LibraryTab.DOWNLOADS, LibraryTab.LOCAL)),
    HISTORY("Historial", listOf(LibraryTab.HISTORY));

    companion object {
        fun fromTab(tab: LibraryTab): LibraryCategory =
            entries.firstOrNull { tab in it.tabs } ?: COLLECTION
    }
}

data class LibraryUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val isScanningLocal: Boolean = false,
    val profile: AccountProfile? = null,
    val selectedTab: LibraryTab = LibraryTab.PLAYLISTS,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_ADDED,
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val playlists: List<SearchResult.Playlist> = emptyList(),
    val customPlaylists: List<Playlist> = emptyList(),
    val likedSongs: List<Track> = emptyList(),
    val downloadedTracks: List<OfflineTrack> = emptyList(),
    val localTracks: List<Track> = emptyList(),
    val localLibraryPaths: List<String> = emptyList(),
    val activeDownloads: Map<String, Float> = emptyMap(),
    val artists: List<SearchResult.Artist> = emptyList(),
    val albums: List<SearchResult.Album> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val error: String? = null,
)

class LibraryViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val getAccountProfileUseCase: GetAccountProfileUseCase,
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val getPlaylistsUseCase: GetPlaylistsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    private val addTracksToPlaylistUseCase: AddTracksToPlaylistUseCase,
    private val getPlaylistIdsForTrackUseCase: GetPlaylistIdsForTrackUseCase,
    private val getLikedSongsUseCase: GetLikedSongsUseCase,
    private val getUserArtistsUseCase: GetUserArtistsUseCase,
    private val getUserAlbumsUseCase: GetUserAlbumsUseCase,
    private val getRemoteHistoryUseCase: GetRemoteHistoryUseCase,
    private val getRecentTracksUseCase: GetRecentTracksUseCase,
    private val toggleLikeTrackUseCase: ToggleLikeTrackUseCase,
    private val getOfflineTracksUseCase: GetOfflineTracksUseCase,
    private val scanLocalTracksUseCase: ScanLocalTracksUseCase,
    private val enrichLocalTracksUseCase: EnrichLocalTracksUseCase,
    private val deleteDownloadedTrackUseCase: DeleteDownloadedTrackUseCase,
    private val syncOfflineTracksUseCase: SyncOfflineTracksUseCase,
    private val downloadManager: DownloadManager,
    private val playbackManager: PlaybackManager,
    observeLibraryUpdatesUseCase: ObserveLibraryUpdatesUseCase? = null,
    private val ioDispatcher: CoroutineDispatcher = MeloDispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var localHistory: List<HistoryEntry> = emptyList()
    private var remoteHistory: List<HistoryEntry> = emptyList()

    private fun updateMergedHistory() {
        val merged = if (localHistory.isEmpty()) {
            remoteHistory
        } else if (remoteHistory.isEmpty()) {
            localHistory
        } else {
            val localIds = localHistory.map { it.track.id }.toSet()
            val localSourceIds =
                localHistory.mapNotNull { it.track.sourceId }.filter { it.isNotBlank() }.toSet()
            val filteredRemote = remoteHistory.filterNot { remote ->
                val remoteSourceId = remote.track.sourceId
                remote.track.id in localIds ||
                        (!remoteSourceId.isNullOrBlank() && remoteSourceId in localSourceIds) ||
                        (remote.track.id.removePrefix("piped:") in localSourceIds)
            }
            localHistory + filteredRemote
        }
        _uiState.update { it.copy(history = merged) }
    }

    init {
        viewModelScope.launch(ioDispatcher) {
            getPlaylistsUseCase().collectLatest { playlists ->
                _uiState.update { it.copy(customPlaylists = playlists) }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            getRecentTracksUseCase(limit = 100).collectLatest { recent ->
                localHistory = recent
                updateMergedHistory()
            }
        }
        viewModelScope.launch(ioDispatcher) {
            getOfflineTracksUseCase().collectLatest { tracks ->
                _uiState.update {
                    it.copy(
                        downloadedTracks = tracks.filter { it.downloadStatus == DownloadStatus.COMPLETED }
                    )
                }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            delay(3500.milliseconds)
            syncOfflineTracksUseCase()
        }

        viewModelScope.launch(ioDispatcher) {
            downloadManager.activeDownloads.collectLatest { downloads ->
                _uiState.update { it.copy(activeDownloads = downloads) }
            }
        }

        viewModelScope.launch(ioDispatcher) {
            getSettingsUseCase().collectLatest { settings ->
                _uiState.update {
                    it.copy(
                        localLibraryPaths = settings.localLibraryPaths
                    )
                }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            val initialCookies = getSettingsUseCase.getSnapshot().sessionCookies
                ?.takeIf { it.isNotBlank() }

            val initiallyLoggedIn = initialCookies != null
            _uiState.update { it.copy(isLoggedIn = initiallyLoggedIn) }

            getSettingsUseCase()
                .map { it.sessionCookies?.takeIf { cookies -> cookies.isNotBlank() } }
                .dropWhile { it == initialCookies }
                .distinctUntilChanged()
                .collectLatest { cookies ->
                    val loggedIn = cookies != null
                    _uiState.update { it.copy(isLoggedIn = loggedIn) }
                    if (loggedIn) {
                        refreshAll()
                    }
                }
        }

        observeLibraryUpdatesUseCase?.let { observeUseCase ->
            viewModelScope.launch(ioDispatcher) {
                observeUseCase().collectLatest { event ->
                    when (event) {
                        is LibraryUpdateEvent.TrackLiked -> {
                            if (!event.isLiked) {
                                _uiState.update { current ->
                                    current.copy(likedSongs = current.likedSongs.filterNot { it.id == event.videoId || it.sourceId == event.videoId })
                                }
                            }
                            refreshLikedSongs()
                        }

                        is LibraryUpdateEvent.AlbumSaved -> {
                            refreshAlbums()
                        }

                        is LibraryUpdateEvent.PlaylistSaved -> {
                            refreshPlaylists()
                        }

                        is LibraryUpdateEvent.ArtistSubscribed -> {
                            refreshArtists()
                        }
                    }
                }
            }
        }
    }

    fun onScreenVisible() {
        if (_uiState.value.isLoggedIn) {
            refreshAll(silent = true)
        }
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        when (tab) {
            LibraryTab.LOCAL -> {
                if (_uiState.value.localTracks.isEmpty()) {
                    scanLocalTracks()
                }
            }

            LibraryTab.HISTORY -> {
                if (_uiState.value.isLoggedIn) {
                    refreshRemoteHistory()
                }
            }

            LibraryTab.LIKED -> {
                if (_uiState.value.isLoggedIn && _uiState.value.likedSongs.isEmpty()) {
                    refreshLikedSongs()
                }
            }

            LibraryTab.PLAYLISTS -> {
                if (_uiState.value.isLoggedIn && _uiState.value.playlists.isEmpty()) {
                    refreshPlaylists()
                }
            }

            LibraryTab.ALBUMS -> {
                if (_uiState.value.isLoggedIn && _uiState.value.albums.isEmpty()) {
                    refreshAlbums()
                }
            }

            LibraryTab.ARTISTS -> {
                if (_uiState.value.isLoggedIn && _uiState.value.artists.isEmpty()) {
                    refreshArtists()
                }
            }

            else -> {}
        }
    }

    fun refreshRemoteHistory() {
        if (!_uiState.value.isLoggedIn) return
        viewModelScope.launch(ioDispatcher) {
            val result = getRemoteHistoryUseCase()
            if (result.isSuccess) {
                remoteHistory = result.getOrDefault(emptyList())
                updateMergedHistory()
            }
        }
    }

    fun refreshLikedSongs() {
        if (!_uiState.value.isLoggedIn) return
        viewModelScope.launch(ioDispatcher) {
            val result = getLikedSongsUseCase()
            if (result.isSuccess) {
                _uiState.update { it.copy(likedSongs = result.getOrDefault(it.likedSongs)) }
            }
        }
    }

    fun refreshPlaylists() {
        if (!_uiState.value.isLoggedIn) return
        viewModelScope.launch(ioDispatcher) {
            val result = getUserPlaylistsUseCase()
            if (result.isSuccess) {
                _uiState.update { it.copy(playlists = result.getOrDefault(it.playlists)) }
            }
        }
    }

    fun refreshAlbums() {
        if (!_uiState.value.isLoggedIn) return
        viewModelScope.launch(ioDispatcher) {
            val result = getUserAlbumsUseCase()
            if (result.isSuccess) {
                _uiState.update { it.copy(albums = result.getOrDefault(it.albums)) }
            }
        }
    }

    fun refreshArtists() {
        if (!_uiState.value.isLoggedIn) return
        viewModelScope.launch(ioDispatcher) {
            val result = getUserArtistsUseCase()
            if (result.isSuccess) {
                _uiState.update { it.copy(artists = result.getOrDefault(it.artists)) }
            }
        }
    }

    fun scanLocalTracks() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isScanningLocal = true) }
            val settings = getSettingsUseCase.getSnapshot()
            val paths = settings.localLibraryPaths
            val scanned = scanLocalTracksUseCase(paths)
            _uiState.update {
                it.copy(
                    localTracks = scanned,
                    localLibraryPaths = paths,
                    isScanningLocal = false
                )
            }

            if (scanned.isNotEmpty()) {
                val enriched = enrichLocalTracksUseCase(scanned)
                _uiState.update { it.copy(localTracks = enriched) }
            }
        }
    }

    fun addLocalLibraryPath(path: String) {
        if (path.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            val currentSettings = getSettingsUseCase.getSnapshot()
            val cleanPath = path.trim()
            if (cleanPath !in currentSettings.localLibraryPaths) {
                val updatedPaths = currentSettings.localLibraryPaths + cleanPath
                updateSettingsUseCase(currentSettings.copy(localLibraryPaths = updatedPaths))
                _uiState.update { it.copy(localLibraryPaths = updatedPaths) }
                scanLocalTracks()
            }
        }
    }

    fun removeLocalLibraryPath(path: String) {
        viewModelScope.launch(ioDispatcher) {
            val currentSettings = getSettingsUseCase.getSnapshot()
            val updatedPaths = currentSettings.localLibraryPaths.filter { it != path }
            updateSettingsUseCase(currentSettings.copy(localLibraryPaths = updatedPaths))
            _uiState.update { it.copy(localLibraryPaths = updatedPaths) }
            scanLocalTracks()
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch(ioDispatcher) {
            downloadManager.downloadTrack(track)
        }
    }

    fun deleteDownloadedTrack(trackId: String) {
        viewModelScope.launch(ioDispatcher) {
            deleteDownloadedTrackUseCase(trackId)
        }
    }

    fun refreshAll(silent: Boolean = false) {
        viewModelScope.launch(ioDispatcher) {
            syncOfflineTracksUseCase()
            scanLocalTracks()
            if (!_uiState.value.isLoggedIn) return@launch

            if (!silent) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val profileDeferred = async { getAccountProfileUseCase() }
            val playlistsDeferred = async { getUserPlaylistsUseCase() }
            val likedSongsDeferred = async { getLikedSongsUseCase() }
            val artistsDeferred = async { getUserArtistsUseCase() }
            val albumsDeferred = async { getUserAlbumsUseCase() }
            val historyDeferred = async { getRemoteHistoryUseCase() }

            val profileResult = profileDeferred.await()
            val playlistsResult = playlistsDeferred.await()
            val likedSongsResult = likedSongsDeferred.await()
            val artistsResult = artistsDeferred.await()
            val albumsResult = albumsDeferred.await()
            val historyResult = historyDeferred.await()

            remoteHistory = historyResult.getOrDefault(remoteHistory)

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    profile = profileResult.getOrNull() ?: current.profile,
                    playlists = playlistsResult.getOrDefault(current.playlists),
                    likedSongs = likedSongsResult.getOrDefault(current.likedSongs),
                    artists = artistsResult.getOrDefault(current.artists),
                    albums = albumsResult.getOrDefault(current.albums),
                    error = if (!silent && playlistsResult.isFailure && likedSongsResult.isFailure) {
                        "No se pudo cargar la biblioteca. Verifica tu conexión o sesión."
                    } else current.error
                )
            }
            updateMergedHistory()
        }
    }

    fun playTrack(track: Track, queue: List<Track> = emptyList()) {
        if (queue.isNotEmpty()) {
            val index = queue.indexOf(track).coerceAtLeast(0)
            playbackManager.setQueue(queue, index)
        } else {
            playbackManager.playTrack(track)
        }
    }

    fun toggleLike(trackId: String, isLiked: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            if (!isLiked) {
                _uiState.update { current ->
                    current.copy(likedSongs = current.likedSongs.filterNot { it.id == trackId })
                }
            }
            toggleLikeTrackUseCase(trackId, isLiked)
            val updatedLiked = getLikedSongsUseCase().getOrNull()
            if (updatedLiked != null) {
                _uiState.update { it.copy(likedSongs = updatedLiked) }
            }
        }
    }

    fun createPlaylist(name: String, onCreated: ((Long) -> Unit)? = null) {
        if (name.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            val id = createPlaylistUseCase(name)
            onCreated?.invoke(id)
        }
    }

    fun renamePlaylist(id: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            renamePlaylistUseCase(id, newName)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch(ioDispatcher) {
            deletePlaylistUseCase(id)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track, onAdded: (() -> Unit)? = null) {
        viewModelScope.launch(ioDispatcher) {
            addTrackToPlaylistUseCase(playlistId, track)
            onAdded?.invoke()
        }
    }

    fun addTracksToPlaylist(
        playlistId: Long,
        tracks: List<Track>,
        onAdded: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch(ioDispatcher) {
            val count = addTracksToPlaylistUseCase(playlistId, tracks)
            onAdded?.invoke(count)
        }
    }

    fun getPlaylistIdsForTrack(trackId: String): Flow<Set<Long>> =
        getPlaylistIdsForTrackUseCase(trackId)

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleSearchActive(active: Boolean? = null) {
        val newActive = active ?: !_uiState.value.isSearchActive
        _uiState.value = _uiState.value.copy(
            isSearchActive = newActive,
            searchQuery = if (!newActive) "" else _uiState.value.searchQuery
        )
    }

    fun setSortOrder(order: LibrarySortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
    }

    fun setViewMode(mode: LibraryViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }
}