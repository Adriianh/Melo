package com.github.adriianh.cli.di

import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.config.shareDir
import com.github.adriianh.cli.service.YouTubeAuthService
import com.github.adriianh.cli.tui.service.DiscordRpcManager
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.core.domain.interactor.DiscoveryInteractors
import com.github.adriianh.core.domain.interactor.LibraryInteractors
import com.github.adriianh.core.domain.interactor.OfflineInteractors
import com.github.adriianh.core.domain.interactor.PlaybackInteractors
import com.github.adriianh.core.domain.interactor.SearchInteractors
import com.github.adriianh.core.domain.interactor.SessionInteractors
import com.github.adriianh.core.domain.interactor.SettingsInteractors
import com.github.adriianh.core.domain.interactor.StatsInteractors
import com.github.adriianh.core.domain.player.JvmMediaSessionManager
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.HistoryRepository
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.repository.ScrobblingRepository
import com.github.adriianh.core.domain.repository.StatsRepository
import com.github.adriianh.core.domain.usecase.library.AddFavoriteEntityUseCase
import com.github.adriianh.core.domain.usecase.library.GetFavoriteEntitiesUseCase
import com.github.adriianh.core.domain.usecase.library.GetLikedSongsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserAlbumsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserArtistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.IsFavoriteEntityUseCase
import com.github.adriianh.core.domain.usecase.library.RemoveFavoriteEntityUseCase
import com.github.adriianh.core.domain.usecase.library.SubscribeChannelUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleFavoriteEntityUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeAlbumUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeTrackUseCase
import com.github.adriianh.data.local.DatabaseFactory
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.provider.audio.InnerTubeAudioProvider
import com.github.adriianh.data.provider.audio.PipedAudioProvider
import com.github.adriianh.data.provider.audio.YtDlpAudioProvider
import com.github.adriianh.data.provider.discovery.CompositeDiscoveryProvider
import com.github.adriianh.data.provider.discovery.DeezerDiscoveryProvider
import com.github.adriianh.data.provider.discovery.InnerTubeDiscoveryProvider
import com.github.adriianh.data.provider.discovery.LastFmDiscoveryProvider
import com.github.adriianh.data.provider.music.InnerTubeMusicProvider
import com.github.adriianh.data.provider.music.ItunesMusicProvider
import com.github.adriianh.data.provider.music.MergedMusicProvider
import com.github.adriianh.data.provider.music.PipedMusicProvider
import com.github.adriianh.data.provider.music.SpotifyMusicProvider
import com.github.adriianh.data.remote.lastfm.LastFmApiClient
import com.github.adriianh.data.remote.spotify.SpotifyApiClient
import com.github.adriianh.data.remote.spotify.SpotifyAuthClient
import com.github.adriianh.data.repository.HistoryRepositoryImpl
import com.github.adriianh.data.repository.OfflineRepositoryImpl
import com.github.adriianh.data.repository.ScrobblingRepositoryImpl
import com.github.adriianh.data.repository.StatsRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

private fun hasSpotifyKeys() =
    resolveEnv("SPOTIFY_CLIENT_ID") != null &&
            resolveEnv("SPOTIFY_CLIENT_SECRET") != null

val appModule = module {
    single<CoroutineDispatcher> { Dispatchers.IO.limitedParallelism(8) }

    single {
        HttpClient(CIO) {
            engine {
                dispatcher = get<CoroutineDispatcher>()
                endpoint {
                    maxConnectionsCount = 20
                    connectTimeout = 5_000
                    connectTimeout = 5_000
                }
            }
            install(ContentNegotiation) {
                val jsonConfig = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                json(jsonConfig)
                json(jsonConfig, contentType = ContentType.Text.JavaScript)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 10_000
            }
            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = 2)
                exponentialDelay()
            }
        }
    }

    single(named("configDirPath")) { configDir }

    single { ArtworkRenderer(get()) }
    single {
        SpotifyAuthClient(
            httpClient = get(),
            clientId = resolveEnv("SPOTIFY_CLIENT_ID") ?: "",
            clientSecret = resolveEnv("SPOTIFY_CLIENT_SECRET") ?: "",
        )
    }
    single { SpotifyApiClient(get(), get()) }
    single {
        LastFmApiClient(
            httpClient = get(),
            apiKey = resolveEnv("LASTFM_API_KEY") ?: "",
            sharedSecret = resolveEnv("LASTFM_SHARED_SECRET") ?: "",
        )
    }

    single<MusicProvider> {
        val itunes = ItunesMusicProvider(get())
        val providers = mutableListOf(
            itunes,
            InnerTubeMusicProvider(
                fallback = PipedMusicProvider(get())
            )
        )
        if (hasSpotifyKeys()) providers.add(SpotifyMusicProvider(get()))
        MergedMusicProvider(providers)
    }
    single<DiscoveryProvider> {
        CompositeDiscoveryProvider(
            listOf(
                InnerTubeDiscoveryProvider(),
                LastFmDiscoveryProvider(get()),
                DeezerDiscoveryProvider(get()),
            )
        )
    }
    single<AudioProvider> {
        val dataDir = File(System.getProperty("user.home"), ".melo")
        if (!dataDir.exists()) dataDir.mkdirs()
        val ytDlp = YtDlpAudioProvider(pipedApiClient = get())
        val piped = PipedAudioProvider(apiClient = get(), fallback = ytDlp)
        InnerTubeAudioProvider(
            configDirPath = dataDir.absolutePath,
            fallback = piped,
            settingsRepository = get(),
            ageGateProvider = ytDlp
        )
    }
    single { JvmMediaSessionManager(httpClient = get()) }
    single { DiscordRpcManager() }

    single<MeloDatabase> { DatabaseFactory.create() }
    single<HistoryRepository> { HistoryRepositoryImpl(get()) }
    single<ScrobblingRepository> { ScrobblingRepositoryImpl(get(), configDir) }
    single<StatsRepository> { StatsRepositoryImpl(get()) }
    single<OfflineRepository> { OfflineRepositoryImpl(File(shareDir), get(), get()) }

    single { YouTubeAuthService(get(), get(), get(), get()) }

    factory { DiscoveryInteractors(get(), get(), get(), get(), get()) }
    factory {
        SearchInteractors(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }
    factory {
        LibraryInteractors(
            getFavorites = get(),
            addFavorite = get(),
            removeFavorite = get(),
            isFavorite = get(),
            getPlaylists = get(),
            getPlaylistTracks = get(),
            createPlaylist = get(),
            renamePlaylist = get(),
            deletePlaylist = get(),
            addTrackToPlaylist = get(),
            removeTrackFromPlaylist = get(),
            getLikedSongs = getOrNull<GetLikedSongsUseCase>(),
            getUserPlaylists = getOrNull<GetUserPlaylistsUseCase>(),
            toggleLikeTrack = getOrNull<ToggleLikeTrackUseCase>(),
            getUserAlbums = getOrNull<GetUserAlbumsUseCase>(),
            getUserArtists = getOrNull<GetUserArtistsUseCase>(),
            toggleLikeAlbum = getOrNull<ToggleLikeAlbumUseCase>(),
            toggleLikePlaylist = getOrNull<ToggleLikePlaylistUseCase>(),
            subscribeChannel = getOrNull<SubscribeChannelUseCase>(),
            getFavoriteEntities = getOrNull<GetFavoriteEntitiesUseCase>(),
            addFavoriteEntity = getOrNull<AddFavoriteEntityUseCase>(),
            removeFavoriteEntity = getOrNull<RemoveFavoriteEntityUseCase>(),
            isFavoriteEntity = getOrNull<IsFavoriteEntityUseCase>(),
            toggleFavoriteEntity = getOrNull<ToggleFavoriteEntityUseCase>(),
        )
    }
    factory { PlaybackInteractors(get(), get(), get(), get(), get()) }
    factory { OfflineInteractors(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { StatsInteractors(get(), get(), get()) }
    factory { SessionInteractors(get(), get(), get()) }
    factory { SettingsInteractors(get(), get()) }
}