package com.github.adriianh.melo.di

import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.FavoritesRepository
import com.github.adriianh.core.domain.repository.HistoryRepository
import com.github.adriianh.core.domain.repository.LoginRepository
import com.github.adriianh.core.domain.repository.MusicRepository
import com.github.adriianh.core.domain.repository.PlaylistRepository
import com.github.adriianh.core.domain.repository.RemoteLibraryRepository
import com.github.adriianh.core.domain.repository.SearchHistoryRepository
import com.github.adriianh.core.domain.repository.SessionRepository
import com.github.adriianh.core.domain.repository.SettingsRepository
import com.github.adriianh.core.domain.usecase.library.GetAccountProfileUseCase
import com.github.adriianh.core.domain.usecase.library.GetLikedSongsUseCase
import com.github.adriianh.core.domain.usecase.library.GetRemoteHistoryUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserAlbumsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserArtistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.SubscribeChannelUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeAlbumUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikePlaylistUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeTrackUseCase
import com.github.adriianh.core.domain.usecase.login.SetSessionCookiesUseCase
import com.github.adriianh.core.domain.usecase.login.VerifySessionUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.GetChartsUseCase
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import com.github.adriianh.core.domain.usecase.search.GetExploreUseCase
import com.github.adriianh.core.domain.usecase.search.GetHomeUseCase
import com.github.adriianh.core.domain.usecase.search.GetTrendingUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.data.player.PlaybackManagerImpl
import com.github.adriianh.data.provider.artwork.CompositeArtworkProvider
import com.github.adriianh.data.provider.artwork.DeezerArtworkProvider
import com.github.adriianh.data.provider.artwork.ItunesArtworkProvider
import com.github.adriianh.data.provider.audio.PipedAudioProvider
import com.github.adriianh.data.provider.discovery.InnerTubeDiscoveryProvider
import com.github.adriianh.data.provider.music.InnerTubeMusicProvider
import com.github.adriianh.data.remote.itunes.ItunesApiClient
import com.github.adriianh.data.remote.piped.PipedApiClient
import com.github.adriianh.data.repository.FavoritesRepositoryImpl
import com.github.adriianh.data.repository.HistoryRepositoryImpl
import com.github.adriianh.data.repository.InnerTubeLoginRepository
import com.github.adriianh.data.repository.MusicRepositoryImpl
import com.github.adriianh.data.repository.PlaylistRepositoryImpl
import com.github.adriianh.data.repository.RemoteLibraryRepositoryImpl
import com.github.adriianh.data.repository.SearchHistoryRepositoryImpl
import com.github.adriianh.data.repository.SessionRepositoryImpl
import com.github.adriianh.data.repository.SettingsRepositoryImpl
import com.github.adriianh.melo.ui.SidebarViewModel
import com.github.adriianh.melo.ui.detail.EntityDetailViewModel
import com.github.adriianh.melo.ui.home.HomeViewModel
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.login.LoginViewModel
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Common module for shared logic that doesn't depend on platform-specific APIs.
 */
val commonModule = module {
    single<HttpClient> {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                    isLenient = true
                })
            }
        }
    }
}

/**
 * Data module for repositories and providers.
 */
val dataModule = module {
    single<MusicProvider> { InnerTubeMusicProvider() }
    single<DiscoveryProvider> { InnerTubeDiscoveryProvider() }
    single<LoginRepository> { InnerTubeLoginRepository() }

    singleOf(::ItunesApiClient)
    singleOf(::PipedApiClient)
    singleOf(::PipedAudioProvider)

    single<MetadataProvider> {
        CompositeArtworkProvider(
            DeezerArtworkProvider(get()),
            ItunesArtworkProvider(get())
        )
    }

    single<MusicRepository> {
        MusicRepositoryImpl(
            musicProvider = get(),
            audioProvider = getOrNull(),
            discoveryProvider = getOrNull(),
            metadataProvider = getOrNull()
        )
    }

    singleOf(::PlaylistRepositoryImpl) { bind<PlaylistRepository>() }
    singleOf(::HistoryRepositoryImpl) { bind<HistoryRepository>() }
    singleOf(::FavoritesRepositoryImpl) { bind<FavoritesRepository>() }
    singleOf(::SearchHistoryRepositoryImpl) { bind<SearchHistoryRepository>() }
    singleOf(::SessionRepositoryImpl) { bind<SessionRepository>() }
    single<RemoteLibraryRepository> { RemoteLibraryRepositoryImpl() }
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            configDirPath = get(named("configDirPath")),
            dispatcher = MeloDispatchers.IO
        )
    }

    single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single<PlaybackManager> {
        PlaybackManagerImpl(
            meloPlayer = get(),
            getStreamUseCase = get(),
            scope = get(),
        )
    }
}

val useCaseModule = module {
    singleOf(::GetStreamUseCase)
    singleOf(::SearchTracksUseCase)
    singleOf(::GetHomeUseCase)
    singleOf(::GetExploreUseCase)
    singleOf(::GetChartsUseCase)
    singleOf(::GetTrendingUseCase)
    singleOf(::GetSettingsUseCase)
    singleOf(::UpdateSettingsUseCase)
    singleOf(::SetSessionCookiesUseCase)
    singleOf(::VerifySessionUseCase)
    singleOf(::GetAccountProfileUseCase)
    singleOf(::GetUserPlaylistsUseCase)
    singleOf(::GetLikedSongsUseCase)
    singleOf(::GetUserArtistsUseCase)
    singleOf(::GetUserAlbumsUseCase)
    singleOf(::GetRemoteHistoryUseCase)
    singleOf(::ToggleLikeTrackUseCase)
    singleOf(::ToggleLikeAlbumUseCase)
    singleOf(::ToggleLikePlaylistUseCase)
    singleOf(::SubscribeChannelUseCase)
    singleOf(::GetEntityDetailsUseCase)
}

/**
 * ViewModel module for all shared ViewModels.
 */
val viewModelModule = module {
    viewModelOf(::PlayerViewModel)
    viewModelOf(::QueueViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::LibraryViewModel)
    viewModelOf(::SidebarViewModel)
    viewModelOf(::EntityDetailViewModel)
    viewModelOf(::SearchViewModel)
}

expect val platformModule: Module
