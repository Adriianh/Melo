package com.github.adriianh.melo.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Entry point for initializing Koin.
 * Used by all platforms to start the dependency injection graph.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(
            commonModule,
            dataModule,
            useCaseModule,
            viewModelModule,
            platformModule
        )
    }
