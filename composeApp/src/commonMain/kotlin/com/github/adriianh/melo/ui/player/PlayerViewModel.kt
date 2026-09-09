package com.github.adriianh.melo.ui.player

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.LibraryUpdateEvent
import com.github.adriianh.core.domain.usecase.library.GetLikedSongsUseCase
import com.github.adriianh.core.domain.usecase.library.ObserveLibraryUpdatesUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeTrackUseCase
import com.github.adriianh.core.domain.usecase.lyrics.GetTrackLyricsUseCase
import com.github.adriianh.core.domain.usecase.lyrics.TranslateLyricsUseCase
import com.github.adriianh.core.domain.usecase.playback.RecordPlayUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.core.util.LrcParser
import com.github.adriianh.melo.util.AccentColorExtractor
import com.github.adriianh.melo.util.AccentPalette
import com.github.adriianh.melo.util.PlayerUiState
import io.ktor.client.HttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel(
    private val manager: PlaybackManager,
    private val httpClient: HttpClient,
    private val toggleLikeTrackUseCase: ToggleLikeTrackUseCase,
    private val recordPlayUseCase: RecordPlayUseCase,
    private val musicProvider: MusicProvider,
    private val getTrackLyricsUseCase: GetTrackLyricsUseCase,
    private val translateLyricsUseCase: TranslateLyricsUseCase,
    private val getLikedSongsUseCase: GetLikedSongsUseCase? = null,
    observeLibraryUpdatesUseCase: ObserveLibraryUpdatesUseCase? = null,
    private val updateSettingsUseCase: UpdateSettingsUseCase? = null,
    getSettingsUseCase: GetSettingsUseCase? = null,
) : ViewModel() {
    val playbackState = manager.playbackState

    private val initialAccent =
        getSettingsUseCase?.getInitial()?.lastAccentColor?.let { Color(it.toULong()) }
    private val _accentPalette = MutableStateFlow(
        initialAccent?.let {
            AccentPalette(
                dominant = it,
                onDominant = Color.White,
                rawDominant = it
            )
        }
            ?: AccentColorExtractor.fallback
    )
    val accentPalette: StateFlow<AccentPalette> = _accentPalette.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    private val _likedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    private var hasFetchedLikedSongs = false

    private val _artistDetails = MutableStateFlow<SearchResult.Artist?>(null)
    val artistDetails: StateFlow<SearchResult.Artist?> = _artistDetails.asStateFlow()

    private val _trackLyrics = MutableStateFlow<TrackLyrics?>(null)
    private val _isLyricsLoading = MutableStateFlow(false)
    private val _showTranslation = MutableStateFlow(false)
    private val _isTranslating = MutableStateFlow(false)
    private val _targetLanguage = MutableStateFlow("es")

    val uiState: StateFlow<PlayerUiState> = combine(
        manager.playbackState,
        manager.queueState,
        _accentPalette,
        _isFavorite,
        _trackLyrics,
        _isLyricsLoading,
        _showTranslation,
        _isTranslating,
        _targetLanguage
    ) { args: Array<Any?> ->
        val playback = args[0] as com.github.adriianh.core.domain.player.PlaybackState
        val queue = args[1] as com.github.adriianh.core.domain.player.QueueState
        val accent = args[2] as AccentPalette
        val isFavorite = args[3] as Boolean
        val trackLyrics = args[4] as TrackLyrics?
        val isLyricsLoading = args[5] as Boolean
        val showTranslation = args[6] as Boolean
        val isTranslating = args[7] as Boolean
        val targetLanguage = args[8] as String

        val activeIndex = if (trackLyrics != null && trackLyrics.hasSync) {
            LrcParser.currentLineIndex(trackLyrics.syncedLyrics, playback.progressMs)
        } else {
            -1
        }

        PlayerUiState.from(playback, queue, accent.dominant).copy(
            isFavorite = isFavorite,
            lyrics = trackLyrics?.plainLyrics,
            trackLyrics = trackLyrics,
            activeLyricIndex = activeIndex,
            isLyricsLoading = isLyricsLoading,
            showTranslation = showTranslation,
            isTranslating = isTranslating,
            targetLanguage = targetLanguage
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerUiState())

    init {
        observeLibraryUpdatesUseCase?.let { observeUseCase ->
            viewModelScope.launch {
                observeUseCase().collectLatest { event ->
                    if (event is LibraryUpdateEvent.TrackLiked) {
                        val rawId = event.videoId.removePrefix("piped:")
                        _likedTrackIds.update { current ->
                            if (event.isLiked) {
                                current + setOf(rawId, event.videoId, "piped:$rawId")
                            } else {
                                current - setOf(rawId, event.videoId, "piped:$rawId")
                            }
                        }
                        updateFavoriteForCurrentTrack()
                    }
                }
            }
        }

        viewModelScope.launch {
            manager.playbackState
                .map { it.currentTrack?.artworkUrl }
                .distinctUntilChanged()
                .flatMapLatest { artworkUrl ->
                    if (artworkUrl.isNullOrBlank()) {
                        flowOf(AccentColorExtractor.fallback)
                    } else {
                        flow {
                            emit(AccentColorExtractor.fromImageUrl(artworkUrl, httpClient))
                        }
                    }
                }
                .collectLatest { palette ->
                    _accentPalette.value = palette
                    if (manager.playbackState.value.currentTrack != null) {
                        updateSettingsUseCase?.invoke { current ->
                            val colorLong = palette.dominant.value.toLong()
                            if (current.lastAccentColor != colorLong) {
                                current.copy(lastAccentColor = colorLong)
                            } else current
                        }
                    }
                }
        }
        viewModelScope.launch {
            manager.playbackState
                .map { it.currentTrack }
                .distinctUntilChanged()
                .collectLatest { track ->
                    if (track != null) {
                        updateFavoriteForCurrentTrack()
                        recordPlayUseCase(track)
                        loadLyrics(track.artist, track.title, track.id)
                    } else {
                        _isFavorite.value = false
                        _trackLyrics.value = null
                        _isLyricsLoading.value = false
                        updateSettingsUseCase?.invoke { current ->
                            if (current.lastAccentColor != null) {
                                current.copy(lastAccentColor = null)
                            } else current
                        }
                    }
                }
        }
        viewModelScope.launch {
            manager.playbackState
                .map { it.currentTrack?.artist?.takeIf { a -> a.isNotBlank() } }
                .distinctUntilChanged()
                .collectLatest { artistName ->
                    if (artistName != null) {
                        loadArtistDetails(artistName)
                    } else {
                        _artistDetails.value = null
                    }
                }
        }
    }

    private suspend fun loadLyrics(artist: String, title: String, trackId: String) {
        if (artist.isBlank() || title.isBlank()) {
            _trackLyrics.value = null
            return
        }
        _isLyricsLoading.value = true
        runCatching {
            var lyrics = getTrackLyricsUseCase(artist, title)
            if (_showTranslation.value && (lyrics.hasSync || !lyrics.plainLyrics.isNullOrBlank())) {
                _isTranslating.value = true
                lyrics = translateLyricsUseCase(trackId, lyrics, _targetLanguage.value)
                _isTranslating.value = false
            }
            _trackLyrics.value = lyrics
        }.onFailure {
            _trackLyrics.value = TrackLyrics(error = "No se pudieron cargar las letras")
            _isTranslating.value = false
        }
        _isLyricsLoading.value = false
    }

    fun toggleTranslation() {
        val currentShow = !_showTranslation.value
        _showTranslation.value = currentShow
        val current = _trackLyrics.value ?: return
        val track = manager.playbackState.value.currentTrack ?: return

        if (currentShow) {
            val hasTranslations = current.syncedLyrics.any { it.translation != null } ||
                    (current.plainLyrics != null && current.targetLanguage == _targetLanguage.value)
            if (!hasTranslations) {
                viewModelScope.launch {
                    _isTranslating.value = true
                    runCatching {
                        val translated =
                            translateLyricsUseCase(track.id, current, _targetLanguage.value)
                        _trackLyrics.value = translated
                    }
                    _isTranslating.value = false
                }
            }
        }
    }

    fun setTargetLanguage(languageCode: String) {
        if (_targetLanguage.value == languageCode) return
        _targetLanguage.value = languageCode
        val current = _trackLyrics.value ?: return
        val track = manager.playbackState.value.currentTrack ?: return

        if (_showTranslation.value) {
            viewModelScope.launch {
                _isTranslating.value = true
                runCatching {
                    val translated = translateLyricsUseCase(track.id, current, languageCode)
                    _trackLyrics.value = translated
                }
                _isTranslating.value = false
            }
        }
    }

    private suspend fun loadArtistDetails(artistName: String) {
        runCatching {
            val results = musicProvider.searchArtists(artistName)
            val first = results.firstOrNull() ?: return@runCatching null
            musicProvider.getArtistDetails(first.id)
        }.onSuccess { details ->
            _artistDetails.value = details
        }.onFailure {
            _artistDetails.value = null
        }
    }

    fun togglePlayPause() = manager.togglePlayPause()

    fun seekTo(positionMs: Long) = manager.seekTo(positionMs)

    fun playNext() = manager.playNext()

    fun playPrevious() = manager.playPrevious()

    fun toggleShuffle() = manager.toggleShuffle()

    fun toggleRepeat() = manager.toggleRepeat()

    val volume: StateFlow<Float> = manager.volume

    fun setVolume(volume: Float) = manager.setVolume(volume)

    fun toggleMute() = manager.toggleMute()

    private fun fetchLikedSongs() {
        if (getLikedSongsUseCase == null) return
        viewModelScope.launch {
            val result = getLikedSongsUseCase().getOrNull()
            if (result != null) {
                hasFetchedLikedSongs = true
                val ids = result.flatMap { track ->
                    listOfNotNull(
                        track.id,
                        track.sourceId,
                        track.id.removePrefix("piped:").takeIf { it.isNotBlank() }
                    )
                }.toSet()
                _likedTrackIds.value = ids
                updateFavoriteForCurrentTrack()
            }
        }
    }

    private fun updateFavoriteForCurrentTrack() {
        val track = manager.playbackState.value.currentTrack
        if (track == null) {
            _isFavorite.value = false
            return
        }
        val rawId = track.sourceId ?: track.id.removePrefix("piped:")
        val ids = _likedTrackIds.value
        _isFavorite.value =
            track.id in ids || (rawId.isNotBlank() && rawId in ids) || ("piped:$rawId" in ids)
        if (!hasFetchedLikedSongs && getLikedSongsUseCase != null) {
            fetchLikedSongs()
        }
    }

    fun toggleFavorite() {
        val track = manager.playbackState.value.currentTrack ?: return
        val videoId = track.sourceId ?: track.id.removePrefix("piped:")
        if (videoId.isBlank()) return
        val newFavorite = !_isFavorite.value
        _isFavorite.value = newFavorite
        val rawId = videoId.removePrefix("piped:")
        _likedTrackIds.update { current ->
            if (newFavorite) {
                current + setOf(rawId, videoId, "piped:$rawId", track.id)
            } else {
                current - setOf(rawId, videoId, "piped:$rawId", track.id)
            }
        }
        viewModelScope.launch {
            val result = toggleLikeTrackUseCase(videoId, newFavorite)
            if (result.isFailure) {
                _isFavorite.value = !newFavorite
                _likedTrackIds.update { current ->
                    if (!newFavorite) {
                        current + setOf(rawId, videoId, "piped:$rawId", track.id)
                    } else {
                        current - setOf(rawId, videoId, "piped:$rawId", track.id)
                    }
                }
            }
        }
    }
}