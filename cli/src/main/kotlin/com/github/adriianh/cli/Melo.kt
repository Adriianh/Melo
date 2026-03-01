package com.github.adriianh.cli

import com.github.adriianh.cli.command.MeloCommand
import com.github.adriianh.cli.di.appModule
import com.github.ajalt.clikt.core.main
import org.koin.core.context.startKoin

fun main(args: Array<String>) {
    startKoin {
        modules(appModule)
    }

    MeloCommand().main(args)
}