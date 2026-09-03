package com.github.adriianh.melo.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.manager.DownloadManager
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.usecase.library.DeletePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.GetPlaylistTracksUseCase
import com.github.adriianh.core.domain.usecase.library.RemoveTrackFromPlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.RenamePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.ReorderPlaylistTracksUseCase
import com.github.adriianh.core.domain.usecase.library.SubscribeChannelUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeAlbumUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikePlaylistUseCase
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntityDetailUiState(
    val isLoading: Boolean = true,
    val entity: SearchResult? = null,
    val error: String? = null,
    val isSaved: Boolean = false,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedTrackCount: Int = 0,
    val totalTrackCount: Int = 0,
    val currentDownloadingTrackTitle: String? = null,
    val downloadedTrackIds: Set<String> = emptySet(),
    val activeDownloadsMap: Map<String, Float> = emptyMap(),
    val downloadedArtistTracks: List<Track> = emptyList(),
)

class EntityDetailViewModel(
    private val getEntityDetailsUseCase: GetEntityDetailsUseCase,
    private val toggleLikeAlbumUseCase: ToggleLikeAlbumUseCase,
    private val toggleLikePlaylistUseCase: ToggleLikePlaylistUseCase,
    private val subscribeChannelUseCase: SubscribeChannelUseCase? = null,
    private val downloadManager: DownloadManager,
    private val offlineRepository: OfflineRepository,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val getPlaylistTracksUseCase: GetPlaylistTracksUseCase? = null,
    private val removeTrackFromPlaylistUseCase: RemoveTrackFromPlaylistUseCase? = null,
    private val deletePlaylistUseCase: DeletePlaylistUseCase? = null,
    private val renamePlaylistUseCase: RenamePlaylistUseCase? = null,
    private val reorderPlaylistTracksUseCase: ReorderPlaylistTracksUseCase? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntityDetailUiState())
    val uiState: StateFlow<EntityDetailUiState> = _uiState.asStateFlow()

    init {
        observeDownloads()
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            combine(
                offlineRepository.getOfflineTracksFlow(),
                downloadManager.activeDownloads
            ) { offlineTracks, activeDownloads ->
                updateDownloadState(offlineTracks, activeDownloads)
            }.collect { }
        }
    }

    private fun updateDownloadState(
        offlineTracks: List<OfflineTrack>,
        activeDownloads: Map<String, Float>
    ) {
        val songs = getEntitySongs()
        if (songs.isEmpty()) {
            _uiState.update {
                it.copy(
                    isDownloaded = false,
                    isDownloading = false,
                    downloadProgress = 0f,
                    downloadedTrackCount = 0,
                    totalTrackCount = 0,
                    currentDownloadingTrackTitle = null,
                    downloadedTrackIds = emptySet(),
                    activeDownloadsMap = emptyMap()
                )
            }
            return
        }

        val completedOffline = offlineTracks.filter {
            it.downloadStatus == DownloadStatus.COMPLETED && it.downloadType == DownloadType.MANUAL
        }

        val completedKeys = mutableSetOf<String>()
        val completedSignatures = mutableSetOf<String>()

        for (ot in completedOffline) {
            completedKeys.add(ot.track.id)
            completedKeys.add(ot.track.id.removePrefix("piped:"))
            completedKeys.add("piped:${ot.track.id.removePrefix("piped:")}")
            ot.track.sourceId?.let { completedKeys.add(it) }
            completedSignatures.add("${ot.track.title.trim().lowercase()}:::${ot.track.artist.trim().lowercase()}")
        }

        val completedTrackIds = mutableSetOf<String>()
        var completedCount = 0

        for (song in songs) {
            val songSig = "${song.title.trim().lowercase()}:::${song.artist.trim().lowercase()}"
            val isCompleted = song.id in completedKeys ||
                    song.id.removePrefix("piped:") in completedKeys ||
                    (song.sourceId != null && song.sourceId in completedKeys) ||
                    songSig in completedSignatures

            if (isCompleted) {
                completedTrackIds.add(song.id)
                completedCount++
            }
        }

        val isDownloading = songs.any { it.id in activeDownloads.keys || it.id.removePrefix("piped:") in activeDownloads.keys }
        val isDownloaded = completedCount == songs.size && songs.isNotEmpty()

        val currentDownloading = songs.firstOrNull { it.id in activeDownloads.keys || it.id.removePrefix("piped:") in activeDownloads.keys }?.title

        val progress = if (songs.isNotEmpty()) {
            val totalTrackProgress = songs.sumOf { track ->
                when {
                    track.id in completedTrackIds -> 1.0
                    track.id in activeDownloads -> activeDownloads[track.id]?.toDouble() ?: 0.0
                    track.id.removePrefix("piped:") in activeDownloads -> activeDownloads[track.id.removePrefix("piped:")]?.toDouble() ?: 0.0
                    else -> 0.0
                }
            }
            (totalTrackProgress / songs.size).toFloat()
        } else 0f

        _uiState.update {
            it.copy(
                isDownloaded = isDownloaded,
                isDownloading = isDownloading,
                downloadProgress = progress,
                downloadedTrackCount = completedCount,
                totalTrackCount = songs.size,
                currentDownloadingTrackTitle = currentDownloading,
                downloadedTrackIds = completedTrackIds,
                activeDownloadsMap = activeDownloads
            )
        }
    }

    private fun getEntitySongs(): List<Track> {
        return when (val e = _uiState.value.entity) {
            is SearchResult.Album -> e.songs.orEmpty()
            is SearchResult.Playlist -> e.songs.orEmpty()
            is SearchResult.Artist -> e.topSongs.orEmpty()
            else -> emptyList()
        }
    }

    fun loadAlbum(
        id: String,
        initialTitle: String = "",
        initialArtwork: String? = null,
        initialAuthor: String = ""
    ) {
        loadEntity(
            SearchResult.Album(
                id = id,
                title = initialTitle,
                author = initialAuthor,
                year = null,
                artworkUrl = initialArtwork
            )
        )
    }

    fun loadPlaylist(
        id: String,
        initialTitle: String = "",
        initialArtwork: String? = null,
        initialAuthor: String = ""
    ) {
        val isCustom = id.startsWith("local:")
                || id.startsWith("custom:")
                || (id.toLongOrNull() != null && !id.startsWith("VL") && !id.startsWith("PL") && !id.startsWith(
            "RD"
        ))
        val localPlaylistId = id.removePrefix("local:").removePrefix("custom:").toLongOrNull()
        if (isCustom && localPlaylistId != null) {
            loadLocalPlaylist(localPlaylistId, id, initialTitle)
            return
        }

        loadEntity(
            SearchResult.Playlist(
                id = id,
                title = initialTitle,
                author = initialAuthor,
                trackCount = null,
                artworkUrl = initialArtwork
            )
        )
    }

    private var currentLoadJob: Job? = null

    private fun loadLocalPlaylist(playlistId: Long, rawId: String, initialTitle: String) {
        currentLoadJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                entity = SearchResult.Playlist(
                    id = rawId,
                    title = initialTitle.ifBlank { "Mi playlist" },
                    author = "Tú",
                    trackCount = 0,
                    artworkUrl = null,
                    songs = emptyList()
                ),
                error = null
            )
        }
        currentLoadJob = viewModelScope.launch {
            if (getPlaylistTracksUseCase != null) {
                getPlaylistTracksUseCase(playlistId).collectLatest { tracks ->
                    _uiState.update { current ->
                        val currentPlaylist = current.entity as? SearchResult.Playlist
                        val title =
                            currentPlaylist?.title?.takeIf { t -> t.isNotBlank() && t != "Mi playlist" }
                                ?: initialTitle.ifBlank { "Mi playlist" }
                        val resolved = SearchResult.Playlist(
                            id = rawId,
                            title = title,
                            author = "Tú",
                            trackCount = tracks.size,
                            artworkUrl = tracks.firstOrNull()?.artworkUrl,
                            songs = tracks
                        )
                        current.copy(
                            isLoading = false,
                            entity = resolved,
                            error = null
                        )
                    }
                    val offlineTracks = offlineRepository.getOfflineTracks()
                    updateDownloadState(offlineTracks, downloadManager.activeDownloads.value)
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun removeTrackFromLocalPlaylist(trackId: String) {
        val entity = _uiState.value.entity as? SearchResult.Playlist ?: return
        val localPlaylistId =
            entity.id.removePrefix("local:").removePrefix("custom:").toLongOrNull() ?: return
        val currentSongs = (entity.songs ?: emptyList()).filter { it.id != trackId }
        _uiState.update {
            it.copy(entity = entity.copy(songs = currentSongs, trackCount = currentSongs.size))
        }
        viewModelScope.launch {
            removeTrackFromPlaylistUseCase?.invoke(localPlaylistId, trackId)
        }
    }

    fun moveLocalPlaylistTrack(fromIndex: Int, toIndex: Int) {
        val entity = _uiState.value.entity as? SearchResult.Playlist ?: return
        val localPlaylistId =
            entity.id.removePrefix("local:").removePrefix("custom:").toLongOrNull() ?: return
        val currentSongs = (entity.songs ?: emptyList()).toMutableList()
        if (fromIndex !in currentSongs.indices || toIndex !in currentSongs.indices || fromIndex == toIndex) return
        val movedTrack = currentSongs.removeAt(fromIndex)
        currentSongs.add(toIndex, movedTrack)
        _uiState.update {
            it.copy(entity = entity.copy(songs = currentSongs))
        }
        viewModelScope.launch {
            reorderPlaylistTracksUseCase?.invoke(localPlaylistId, currentSongs.map { it.id })
        }
    }

    fun renameLocalPlaylist(newName: String) {
        val entity = _uiState.value.entity as? SearchResult.Playlist ?: return
        val localPlaylistId =
            entity.id.removePrefix("local:").removePrefix("custom:").toLongOrNull() ?: return
        if (newName.isBlank()) return
        viewModelScope.launch {
            renamePlaylistUseCase?.invoke(localPlaylistId, newName)
            _uiState.update {
                val current = it.entity as? SearchResult.Playlist ?: return@update it
                it.copy(entity = current.copy(title = newName))
            }
        }
    }

    fun deleteLocalPlaylist(onDeleted: () -> Unit) {
        val entity = _uiState.value.entity as? SearchResult.Playlist ?: return
        val localPlaylistId =
            entity.id.removePrefix("local:").removePrefix("custom:").toLongOrNull() ?: return
        viewModelScope.launch {
            deletePlaylistUseCase?.invoke(localPlaylistId)
            onDeleted()
        }
    }

    fun loadArtist(id: String, initialName: String = "", initialArtwork: String? = null) {
        loadEntity(
            SearchResult.Artist(
                id = id,
                name = initialName,
                artworkUrl = initialArtwork
            )
        )
    }

    fun toggleSave() {
        val entity = _uiState.value.entity ?: return
        val newSaved = !_uiState.value.isSaved
        _uiState.update { it.copy(isSaved = newSaved) }
        viewModelScope.launch {
            try {
                when (entity) {
                    is SearchResult.Album -> toggleLikeAlbumUseCase(entity.id, newSaved)
                    is SearchResult.Playlist -> toggleLikePlaylistUseCase(entity.id, newSaved)
                    is SearchResult.Artist -> subscribeChannelUseCase?.invoke(entity.id, newSaved)
                    else -> {}
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaved = !newSaved) }
            }
        }
    }

    fun toggleDownload() {
        val songs = getEntitySongs()
        if (songs.isEmpty()) return
        val currentlyDownloaded = _uiState.value.isDownloaded

        viewModelScope.launch {
            if (currentlyDownloaded) {
                for (track in songs) {
                    downloadManager.deleteDownload(track.id)
                }
            } else {
                downloadManager.downloadTracks(songs)
            }
        }
    }

    private fun loadEntity(initial: SearchResult) {
        currentLoadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, entity = initial, error = null) }
        currentLoadJob = viewModelScope.launch {
            val loadedOffline = loadEntityOffline(initial)
            if (loadedOffline) {
                val offlineTracks = offlineRepository.getOfflineTracks()
                updateDownloadState(offlineTracks, downloadManager.activeDownloads.value)
            }

            val isOffline = getSettingsUseCase.getSnapshot().offlineMode
            if (isOffline) {
                if (!loadedOffline) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No hay contenido descargado para este elemento."
                        )
                    }
                }
                return@launch
            }

            try {
                val isOnlineAlbumId = when (initial) {
                    is SearchResult.Album -> initial.id.startsWith("MPRE") || initial.id.startsWith(
                        "OLAK"
                    ) || initial.id.startsWith("VL") || initial.id.startsWith("FE") || initial.id.startsWith(
                        "piped:"
                    )

                    else -> false
                }

                if (loadedOffline && initial is SearchResult.Album && !isOnlineAlbumId) {
                    return@launch
                }

                val fullDetails = getEntityDetailsUseCase(initial)
                val hasContent = when (fullDetails) {
                    is SearchResult.Album -> !fullDetails.songs.isNullOrEmpty()
                    is SearchResult.Playlist -> !fullDetails.songs.isNullOrEmpty()
                    is SearchResult.Artist -> !fullDetails.topSongs.isNullOrEmpty() || fullDetails.sections.isNotEmpty()
                    else -> true
                }

                if (hasContent) {
                    if (loadedOffline && fullDetails is SearchResult.Album && initial is SearchResult.Album) {
                        val initialAuthor =
                            initial.author.takeIf { it.isNotBlank() && it != "Unknown" }
                        val fullAuthor =
                            fullDetails.author.takeIf { it.isNotBlank() && it != "Unknown" }
                        if (initialAuthor != null && fullAuthor != null &&
                            !fullAuthor.contains(initialAuthor, ignoreCase = true) &&
                            !initialAuthor.contains(fullAuthor, ignoreCase = true)
                        ) {
                            return@launch
                        }
                    }

                    val initialArtistId = (initial as? SearchResult.Artist)?.id.orEmpty()
                    val matchingOfflineArtistTracks = if (fullDetails is SearchResult.Artist) {
                        offlineRepository.getOfflineTracks()
                            .filter { it.downloadStatus == DownloadStatus.COMPLETED && it.downloadType == DownloadType.MANUAL }
                            .map { it.track }
                            .filter {
                                it.artist.contains(fullDetails.name, ignoreCase = true) ||
                                        (initialArtistId.isNotBlank() && it.artist.contains(initialArtistId, ignoreCase = true))
                            }
                    } else emptyList()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entity = fullDetails,
                            downloadedArtistTracks = matchingOfflineArtistTracks.ifEmpty { it.downloadedArtistTracks },
                            error = null
                        )
                    }
                    val offlineTracks = offlineRepository.getOfflineTracks()
                    updateDownloadState(offlineTracks, downloadManager.activeDownloads.value)
                } else if (!loadedOffline) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No se pudieron obtener los detalles del elemento."
                        )
                    }
                }
            } catch (e: Exception) {
                if (!loadedOffline) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Error al cargar detalles"
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadEntityOffline(initial: SearchResult): Boolean {
        val allOffline = offlineRepository.getOfflineTracks()
        when (initial) {
            is SearchResult.Album -> {
                val albumQuery = initial.title.ifBlank { initial.id }
                val hasAuthor = initial.author.isNotBlank() && initial.author != "Unknown"
                val matchingTracks = allOffline
                    .filter { it.downloadStatus == DownloadStatus.COMPLETED }
                    .map { it.track }
                    .filter {
                        val matchesAlbum = albumQuery.isNotBlank() && it.album.equals(
                            albumQuery,
                            ignoreCase = true
                        )
                        val matchesAuthor =
                            hasAuthor && it.artist.equals(initial.author, ignoreCase = true)

                        if (albumQuery.isNotBlank() && hasAuthor) {
                            matchesAlbum && matchesAuthor
                        } else if (albumQuery.isNotBlank()) {
                            matchesAlbum
                        } else if (hasAuthor) {
                            matchesAuthor
                        } else {
                            false
                        }
                    }
                if (matchingTracks.isNotEmpty()) {
                    val resolved = initial.copy(
                        title = initial.title.ifBlank { matchingTracks.first().album },
                        songs = matchingTracks,
                        artworkUrl = initial.artworkUrl ?: matchingTracks.firstOrNull()?.artworkUrl,
                        author = initial.author.ifBlank { matchingTracks.first().artist }
                    )
                    _uiState.update { it.copy(isLoading = false, entity = resolved, error = null) }
                    return true
                }
            }
            is SearchResult.Playlist -> {
                val matchingTracks = allOffline
                    .filter { it.downloadStatus == DownloadStatus.COMPLETED }
                    .map { it.track }
                if (matchingTracks.isNotEmpty()) {
                    val resolved = initial.copy(
                        songs = matchingTracks,
                        artworkUrl = initial.artworkUrl ?: matchingTracks.firstOrNull()?.artworkUrl
                    )
                    _uiState.update { it.copy(isLoading = false, entity = resolved, error = null) }
                    return true
                }
            }
            is SearchResult.Artist -> {
                val artistQuery = initial.name.ifBlank { initial.id }
                val matchingTracks = allOffline
                    .filter { it.downloadStatus == DownloadStatus.COMPLETED }
                    .map { it.track }
                    .filter { it.artist.contains(artistQuery, ignoreCase = true) }
                if (matchingTracks.isNotEmpty()) {
                    val resolved = initial.copy(
                        name = initial.name.ifBlank { matchingTracks.first().artist },
                        topSongs = matchingTracks,
                        artworkUrl = initial.artworkUrl ?: matchingTracks.firstOrNull()?.artworkUrl
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entity = resolved,
                            downloadedArtistTracks = matchingTracks,
                            error = null
                        )
                    }
                    return true
                }
            }
            else -> {}
        }
        return false
    }
}