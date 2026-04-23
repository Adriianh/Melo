package com.github.adriianh.melo.di

import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.FavoritesRepository
import com.github.adriianh.core.domain.repository.HistoryRepository
import com.github.adriianh.core.domain.repository.MusicRepository
import com.github.adriianh.core.domain.repository.PlaylistRepository
import com.github.adriianh.core.domain.repository.SearchHistoryRepository
import com.github.adriianh.core.domain.repository.SessionRepository
import com.github.adriianh.core.domain.repository.SettingsRepository
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.util.MeloDispatchers
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
import com.github.adriianh.data.repository.MusicRepositoryImpl
import com.github.adriianh.data.repository.PlaylistRepositoryImpl
import com.github.adriianh.data.repository.SearchHistoryRepositoryImpl
import com.github.adriianh.data.repository.SessionRepositoryImpl
import com.github.adriianh.data.repository.SettingsRepositoryImpl
import com.github.adriianh.innertube.createPlatformHttpClient
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.search.SearchViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
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
        val baseClient = createPlatformHttpClient()
        HttpClient(baseClient.engine) {
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
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            configDirPath = get(named("configDirPath")),
            dispatcher = MeloDispatchers.IO
        )
    }
}

val useCaseModule = module {
    singleOf(::GetStreamUseCase)
}

/**
 * ViewModel module for all shared ViewModels.
 */
val viewModelModule = module {
    viewModelOf(::SearchViewModel)
    viewModelOf(::PlayerViewModel)
}

expect val platformModule: Module
