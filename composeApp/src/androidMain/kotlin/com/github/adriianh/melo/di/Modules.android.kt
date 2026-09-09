package com.github.adriianh.melo.di

import com.github.adriianh.core.domain.player.AndroidMeloPlayer
import com.github.adriianh.core.domain.player.MediaSessionManager
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.data.local.DatabaseFactory
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.provider.audio.InnerTubeAudioProvider
import com.github.adriianh.data.provider.audio.PipedAudioProvider
import com.github.adriianh.data.repository.AndroidOfflineRepositoryImpl
import com.github.adriianh.melo.player.AndroidMediaSessionManager
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<MeloDatabase> { DatabaseFactory.create() }
    single<MeloPlayer> { AndroidMeloPlayer(androidContext()) }
    single<MediaSessionManager> {
        AndroidMediaSessionManager(
            context = androidContext(),
            playbackManager = get(),
            meloPlayer = get()
        ).also { it.init() }
    }


    single(named("configDirPath")) { androidContext().filesDir.absolutePath }

    single<AudioProvider> {
        val pipedProvider = PipedAudioProvider(apiClient = get())
        InnerTubeAudioProvider(
            configDirPath = get(named("configDirPath")),
            fallback = pipedProvider,
            settingsRepository = get()
        )
    }

    single<OfflineRepository> {
        AndroidOfflineRepositoryImpl(
            dataDir = androidContext().filesDir,
            settingsRepository = get(),
            dispatcher = Dispatchers.IO,
            context = androidContext()
        )
    }
}