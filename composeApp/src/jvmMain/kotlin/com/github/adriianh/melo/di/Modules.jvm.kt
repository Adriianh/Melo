package com.github.adriianh.melo.di

import com.github.adriianh.core.domain.player.JvmMeloPlayer
import com.github.adriianh.core.domain.player.MediaSessionManager
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.repository.SettingsRepository
import com.github.adriianh.data.local.DatabaseFactory
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.provider.audio.InnerTubeAudioProvider
import com.github.adriianh.data.provider.audio.PipedAudioProvider
import com.github.adriianh.data.provider.audio.YtDlpAudioProvider
import com.github.adriianh.data.remote.piped.PipedApiClient
import com.github.adriianh.data.repository.OfflineRepositoryImpl
import com.github.adriianh.melo.player.JvmMediaSessionManager
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module = module {
    single<MeloDatabase> { DatabaseFactory.create() }
    single<MeloPlayer> { JvmMeloPlayer() }

    single<MediaSessionManager> {
        JvmMediaSessionManager(
            playbackManager = get(),
            httpClient = get()
        ).also { it.init() }
    }

    val dataDir = File(System.getProperty("user.home"), ".melo")
    if (!dataDir.exists()) dataDir.mkdirs()

    single(named("configDirPath")) { dataDir.absolutePath }

    single<AudioProvider> {
        val ytDlpProvider = YtDlpAudioProvider(pipedApiClient = get<PipedApiClient>())
        val pipedProvider = PipedAudioProvider(
            apiClient = get<PipedApiClient>(),
            fallback = ytDlpProvider
        )
        InnerTubeAudioProvider(
            configDirPath = get<String>(named("configDirPath")),
            fallback = pipedProvider,
            settingsRepository = get<SettingsRepository>()
        )
    }

    single<OfflineRepository> {
        OfflineRepositoryImpl(
            dataDir = dataDir,
            settingsRepository = get<SettingsRepository>(),
            dispatcher = Dispatchers.IO
        )
    }
}