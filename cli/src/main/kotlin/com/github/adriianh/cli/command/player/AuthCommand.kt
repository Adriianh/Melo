package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.usecase.playback.CompleteWebAuthUseCase
import com.github.adriianh.core.domain.usecase.playback.StartWebAuthUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class AuthCommand : CliktCommand(name = "auth") {
    override fun help(context: Context): String = "Authentication management (Last.fm, etc.)"

    init {
        subcommands(LastFmAuthCommand())
    }

    override fun run() {}
}

class LastFmAuthCommand : CliktCommand(name = "lastfm"), KoinComponent {
    private val terminal = Terminal()
    private val token by argument(help = "The token from the Last.fm auth page (if completing auth)").optional()

    override fun help(context: Context): String = "Authenticate with Last.fm"

    override fun run() {
        if (GlobalContext.getOrNull() == null) {
            startKoin { modules(appModule) }
        }
        try {
            runBlocking {
                if (token == null) {
                    val startWebAuth: StartWebAuthUseCase by inject()
                    val url = startWebAuth()
                    if (url != null) {
                        terminal.println(cyan("Please open the following URL in your browser to authorize Melo:"))
                        terminal.println(yellow(url))
                        terminal.println(gray("\nAfter authorizing, run: melo auth lastfm <token>"))
                        terminal.println(gray("(The token is the 'token' parameter in the URL you were redirected to)"))
                    } else {
                        terminal.println(yellow("Failed to start Last.fm authentication."))
                    }
                } else {
                    val completeWebAuth: CompleteWebAuthUseCase by inject()
                    val success = completeWebAuth(token!!)
                    if (success) {
                        terminal.println(green("Successfully authenticated with Last.fm!"))
                    } else {
                        terminal.println(yellow("Failed to complete Last.fm authentication. Check your token."))
                    }
                }
            }
        } finally {
            stopKoin()
        }
    }
}
