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
import com.github.adriianh.core.domain.usecase.library.RemoveTrackFromPlaylistUseCase
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class LibraryTab(val label: String) {
    PLAYLISTS("Playlists"),
    LIKED("Me Gusta"),
    DOWNLOADS("Descargas"),
    LOCAL("Archivos locales"),
    ARTISTS("Artistas"),
    ALBUMS("Álbumes"),
    HISTORY("Historial")
}

enum class LibraryCategory(
    val label: String,
    val tabs: List<LibraryTab>
) {
    COLLECTION(
        "Tu Colección",
        listOf(LibraryTab.PLAYLISTS, LibraryTab.LIKED, LibraryTab.ALBUMS, LibraryTab.ARTISTS)
    ),
    DEVICE("En tu Dispositivo", listOf(LibraryTab.DOWNLOADS, LibraryTab.LOCAL)),
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
    private val removeTrackFromPlaylistUseCase: RemoveTrackFromPlaylistUseCase,
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
        _uiState.value = _uiState.value.copy(history = merged)
    }

    init {
        viewModelScope.launch {
            getPlaylistsUseCase().collectLatest { playlists ->
                _uiState.value = _uiState.value.copy(customPlaylists = playlists)
            }
        }
        viewModelScope.launch {
            getRecentTracksUseCase(limit = 100).collectLatest { recent ->
                localHistory = recent
                updateMergedHistory()
            }
        }
        viewModelScope.launch {
            syncOfflineTracksUseCase()
            getOfflineTracksUseCase().collectLatest { tracks ->
                _uiState.value = _uiState.value.copy(
                    downloadedTracks = tracks.filter { it.downloadStatus == DownloadStatus.COMPLETED }
                )
            }
        }

        viewModelScope.launch {
            downloadManager.activeDownloads.collectLatest { downloads ->
                _uiState.value = _uiState.value.copy(activeDownloads = downloads)
            }
        }

        viewModelScope.launch {
            getSettingsUseCase().collectLatest { settings ->
                _uiState.value = _uiState.value.copy(
                    localLibraryPaths = settings.localLibraryPaths
                )
            }
        }
        scanLocalTracks()

        // Account / Online library sync
        viewModelScope.launch {
            val initialCookies = getSettingsUseCase.getSnapshot().sessionCookies
                ?.takeIf { it.isNotBlank() }

            val initiallyLoggedIn = initialCookies != null
            _uiState.value = _uiState.value.copy(isLoggedIn = initiallyLoggedIn)
            if (initiallyLoggedIn) {
                refreshAll()
            }

            getSettingsUseCase()
                .map { it.sessionCookies?.takeIf { cookies -> cookies.isNotBlank() } }
                .dropWhile { it == initialCookies }
                .distinctUntilChanged()
                .collectLatest { cookies ->
                    val loggedIn = cookies != null
                    _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
                    if (loggedIn) {
                        refreshAll()
                    }
                }
        }
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        if (tab == LibraryTab.LOCAL && _uiState.value.localTracks.isEmpty()) {
            scanLocalTracks()
        }
        if (tab == LibraryTab.HISTORY && _uiState.value.isLoggedIn) {
            refreshRemoteHistory()
        }
    }

    fun refreshRemoteHistory() {
        if (!_uiState.value.isLoggedIn) return
        viewModelScope.launch {
            val result = getRemoteHistoryUseCase()
            if (result.isSuccess) {
                remoteHistory = result.getOrDefault(emptyList())
                updateMergedHistory()
            }
        }
    }

    fun scanLocalTracks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanningLocal = true)
            val settings = getSettingsUseCase.getSnapshot()
            val paths = settings.localLibraryPaths
            val scanned = scanLocalTracksUseCase(paths)
            _uiState.value = _uiState.value.copy(
                localTracks = scanned,
                localLibraryPaths = paths,
                isScanningLocal = false
            )

            if (scanned.isNotEmpty()) {
                val enriched = enrichLocalTracksUseCase(scanned)
                _uiState.value = _uiState.value.copy(localTracks = enriched)
            }
        }
    }

    fun addLocalLibraryPath(path: String) {
        if (path.isBlank()) return
        viewModelScope.launch {
            val currentSettings = getSettingsUseCase.getSnapshot()
            val cleanPath = path.trim()
            if (cleanPath !in currentSettings.localLibraryPaths) {
                val updatedPaths = currentSettings.localLibraryPaths + cleanPath
                updateSettingsUseCase(currentSettings.copy(localLibraryPaths = updatedPaths))
                _uiState.value = _uiState.value.copy(localLibraryPaths = updatedPaths)
                scanLocalTracks()
            }
        }
    }

    fun removeLocalLibraryPath(path: String) {
        viewModelScope.launch {
            val currentSettings = getSettingsUseCase.getSnapshot()
            val updatedPaths = currentSettings.localLibraryPaths.filter { it != path }
            updateSettingsUseCase(currentSettings.copy(localLibraryPaths = updatedPaths))
            _uiState.value = _uiState.value.copy(localLibraryPaths = updatedPaths)
            scanLocalTracks()
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            downloadManager.downloadTrack(track)
        }
    }

    fun deleteDownloadedTrack(trackId: String) {
        viewModelScope.launch {
            deleteDownloadedTrackUseCase(trackId)
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            syncOfflineTracksUseCase()
            scanLocalTracks()
            if (!_uiState.value.isLoggedIn) return@launch

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val profileResult = getAccountProfileUseCase()
            val playlistsResult = getUserPlaylistsUseCase()
            val likedSongsResult = getLikedSongsUseCase()
            val artistsResult = getUserArtistsUseCase()
            val albumsResult = getUserAlbumsUseCase()
            val historyResult = getRemoteHistoryUseCase()
            remoteHistory = historyResult.getOrDefault(emptyList())

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                profile = profileResult.getOrNull() ?: _uiState.value.profile,
                playlists = playlistsResult.getOrDefault(emptyList()),
                likedSongs = likedSongsResult.getOrDefault(emptyList()),
                artists = artistsResult.getOrDefault(emptyList()),
                albums = albumsResult.getOrDefault(emptyList()),
                error = if (playlistsResult.isFailure && likedSongsResult.isFailure) {
                    "No se pudo cargar la biblioteca. Verifica tu conexión o sesión."
                } else null
            )
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
        viewModelScope.launch {
            toggleLikeTrackUseCase(trackId, isLiked)
            val updatedLiked = getLikedSongsUseCase().getOrNull()
            if (updatedLiked != null) {
                _uiState.value = _uiState.value.copy(likedSongs = updatedLiked)
            }
        }
    }

    fun createPlaylist(name: String, onCreated: ((Long) -> Unit)? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = createPlaylistUseCase(name)
            onCreated?.invoke(id)
        }
    }

    fun renamePlaylist(id: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            renamePlaylistUseCase(id, newName)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            deletePlaylistUseCase(id)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: Track, onAdded: (() -> Unit)? = null) {
        viewModelScope.launch {
            addTrackToPlaylistUseCase(playlistId, track)
            onAdded?.invoke()
        }
    }

    fun addTracksToPlaylist(
        playlistId: Long,
        tracks: List<Track>,
        onAdded: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val count = addTracksToPlaylistUseCase(playlistId, tracks)
            onAdded?.invoke(count)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            removeTrackFromPlaylistUseCase(playlistId, trackId)
        }
    }

    fun getPlaylistIdsForTrack(trackId: String): Flow<Set<Long>> =
        getPlaylistIdsForTrackUseCase(trackId)
}