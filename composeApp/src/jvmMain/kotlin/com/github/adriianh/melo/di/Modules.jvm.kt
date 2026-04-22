package com.github.adriianh.melo.di

import com.github.adriianh.core.domain.player.JvmMeloPlayer
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.data.local.DatabaseFactory
import com.github.adriianh.data.local.MeloDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<MeloDatabase> { DatabaseFactory.create() }
    single<MeloPlayer> { JvmMeloPlayer() }
}