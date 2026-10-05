package com.github.adriianh.melo.di

import com.github.adriianh.core.domain.manager.DownloadManager
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.repository.HistoryRepository
import com.github.adriianh.core.domain.usecase.library.GetLikedSongsUseCase
import com.github.adriianh.core.domain.usecase.library.ObserveLibraryUpdatesUseCase
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.data.manager.DownloadManagerImpl
import com.github.adriianh.data.player.PlaybackManagerImpl
import com.github.adriianh.data.provider.audio.PipedAudioProvider
import com.github.adriianh.data.provider.discovery.InnerTubeDiscoveryProvider
import com.github.adriianh.data.provider.music.InnerTubeMusicProvider
import com.github.adriianh.data.repository.HistoryRepositoryImpl
import com.github.adriianh.melo.ui.SidebarViewModel
import com.github.adriianh.melo.ui.detail.EntityDetailViewModel
import com.github.adriianh.melo.ui.home.HomeViewModel
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.login.LoginViewModel
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchViewModel
import com.github.adriianh.melo.ui.settings.UpdateViewModel
import com.github.adriianh.melo.util.PlatformUpdateInstaller
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Common module for shared logic that doesn't depend on platform-specific APIs.
 */
val commonModule =
    module {
        single<HttpClient> {
            HttpClient(CIO) {
                engine {
                    requestTimeout = 0L
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000L
                    connectTimeoutMillis = 15_000L
                    socketTimeoutMillis = 30_000L
                }
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            explicitNulls = false
                            encodeDefaults = true
                            isLenient = true
                        },
                    )
                }
            }
        }
    }

/**
 * Data module for repositories and providers.
 */
val dataModule =
    module {
        single<MusicProvider> { InnerTubeMusicProvider() }
        single<DiscoveryProvider> { InnerTubeDiscoveryProvider() }

        singleOf(::PipedAudioProvider)
        single<HistoryRepository> {
            HistoryRepositoryImpl(
                database = get(),
                settingsRepository = get(),
            )
        }

        single<DownloadManager> {
            DownloadManagerImpl(
                httpClient = get(),
                getStreamUseCase = get(),
                getSettingsUseCase = get(),
                offlineRepository = get(),
                configDirPath = get<String>(named("configDirPath")),
                dispatcher = MeloDispatchers.IO,
            )
        }

        single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
        single<PlaybackManager> {
            PlaybackManagerImpl(
                meloPlayer = get(),
                getStreamUseCase = get(),
                scope = get(),
                getRadioUseCase = getOrNull(),
                getSettingsUseCase = getOrNull(),
                downloadManager = getOrNull(),
                offlineRepository = getOrNull(),
                recordPlayUseCase = getOrNull(),
                updateSettingsUseCase = getOrNull(),
                saveSessionUseCase = getOrNull(),
                restoreSessionUseCase = getOrNull(),
                clearSessionUseCase = getOrNull(),
                streamCacheRepository = getOrNull(),
                networkMonitor = getOrNull(),
                ioDispatcher = MeloDispatchers.IO,
            )
        }

        singleOf(::PlatformUpdateInstaller)
    }

/**
 * ViewModel module for all shared ViewModels.
 */
val viewModelModule =
    module {
        viewModel {
            PlayerViewModel(
                manager = get(),
                httpClient = get(),
                toggleLikeTrackUseCase = get(),
                recordPlayUseCase = get(),
                musicProvider = get(),
                getTrackLyricsUseCase = get(),
                translateLyricsUseCase = get(),
                getLikedSongsUseCase = getOrNull<GetLikedSongsUseCase>(),
                observeLibraryUpdatesUseCase = getOrNull<ObserveLibraryUpdatesUseCase>(),
                updateSettingsUseCase = getOrNull(),
                getSettingsUseCase = getOrNull(),
            )
        }
        viewModelOf(::QueueViewModel)
        viewModel {
            HomeViewModel(
                getHomeUseCase = get(),
                getExploreUseCase = get(),
                getChartsUseCase = get(),
                getTrendingUseCase = get(),
                searchTracksUseCase = get(),
                getSettingsUseCase = get(),
                getOfflineTracksUseCase = get(),
                scanLocalTracksUseCase = get(),
                getRecentTracksUseCase = get(),
                homeFeedCache = get(),
                networkMonitor = getOrNull(),
            )
        }
        viewModelOf(::LoginViewModel)
        viewModel {
            LibraryViewModel(
                getSettingsUseCase = get(),
                updateSettingsUseCase = get(),
                getAccountProfileUseCase = get(),
                getUserPlaylistsUseCase = get(),
                getPlaylistsUseCase = get(),
                createPlaylistUseCase = get(),
                renamePlaylistUseCase = get(),
                deletePlaylistUseCase = get(),
                addTrackToPlaylistUseCase = get(),
                addTracksToPlaylistUseCase = get(),
                getPlaylistIdsForTrackUseCase = get(),
                getLikedSongsUseCase = get(),
                getUserArtistsUseCase = get(),
                getUserAlbumsUseCase = get(),
                getRemoteHistoryUseCase = get(),
                getRecentTracksUseCase = get(),
                toggleLikeTrackUseCase = get(),
                getOfflineTracksUseCase = get(),
                scanLocalTracksUseCase = get(),
                enrichLocalTracksUseCase = get(),
                deleteDownloadedTrackUseCase = get(),
                syncOfflineTracksUseCase = get(),
                downloadManager = get(),
                playbackManager = get(),
                libraryCache = get(),
                observeLibraryUpdatesUseCase = getOrNull(),
            )
        }
        viewModel {
            SidebarViewModel(
                getSettingsUseCase = get(),
                getAccountProfileUseCase = get(),
                getUserPlaylistsUseCase = get(),
                libraryCache = get(),
                networkMonitor = getOrNull(),
            )
        }
        viewModel {
            EntityDetailViewModel(
                getEntityDetailsUseCase = get(),
                toggleLikeAlbumUseCase = get(),
                toggleLikePlaylistUseCase = get(),
                subscribeChannelUseCase = getOrNull(),
                getUserAlbumsUseCase = getOrNull(),
                getUserPlaylistsUseCase = getOrNull(),
                getUserArtistsUseCase = getOrNull(),
                downloadManager = get(),
                offlineRepository = get(),
                getSettingsUseCase = get(),
                networkMonitor = getOrNull(),
                entityCache = getOrNull(),
                getPlaylistTracksUseCase = getOrNull(),
                removeTrackFromPlaylistUseCase = getOrNull(),
                deletePlaylistUseCase = getOrNull(),
                renamePlaylistUseCase = getOrNull(),
                reorderPlaylistTracksUseCase = getOrNull(),
            )
        }
        viewModel {
            SearchViewModel(
                searchTracksUseCase = get(),
                searchAlbumsUseCase = get(),
                searchArtistsUseCase = get(),
                searchPlaylistsUseCase = get(),
                searchVideosUseCase = get(),
                searchSummaryUseCase = get(),
                getExploreUseCase = get(),
                getChartsUseCase = get(),
                getTrendingUseCase = get(),
                getMoodAndGenresUseCase = get(),
                getSearchHistoryUseCase = get(),
                saveSearchQueryUseCase = get(),
                deleteSearchQueryUseCase = getOrNull(),
                getSearchSuggestionsUseCase = get(),
                getRecentTracksUseCase = get(),
                getRemoteHistoryUseCase = get(),
                browseCategoryUseCase = get(),
                getSettingsUseCase = get(),
                getOfflineTracksUseCase = get(),
                scanLocalTracksUseCase = get(),
                networkMonitor = getOrNull(),
            )
        }
        viewModelOf(::UpdateViewModel)
    }

expect val platformModule: Module
