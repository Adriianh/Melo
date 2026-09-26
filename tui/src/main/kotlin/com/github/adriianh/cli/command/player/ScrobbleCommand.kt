package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.repository.ScrobblingRepository
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
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

class ScrobbleCommand : CliktCommand(name = "scrobble") {
    override fun help(context: Context): String = "Check or disconnect Last.fm scrobbling integration."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo scrobble status\n" +
            "  melo scrobble logout"

    init {
        subcommands(
            ScrobbleStatusCommand(),
            ScrobbleLogoutCommand(),
        )
    }

    override fun run() {}
}

class ScrobbleStatusCommand :
    CliktCommand(name = "status"),
    KoinComponent {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Display Last.fm scrobbling authentication status"

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo scrobble status"

    override fun run() {
        if (GlobalContext.getOrNull() == null) {
            startKoin { modules(appModule) }
        }
        try {
            runBlocking {
                val scrobbling: ScrobblingRepository by inject()
                val sessionKey = scrobbling.getSessionKey()
                if (sessionKey != null) {
                    terminal.println(green("Scrobbling is ACTIVE (authenticated with Last.fm)"))
                    terminal.println(gray("Session Key: ${sessionKey.take(5)}..."))
                } else {
                    terminal.println(yellow("Scrobbling is INACTIVE (not authenticated)"))
                    terminal.println(gray("Run 'melo auth lastfm' to authenticate."))
                }
            }
        } finally {
            stopKoin()
        }
    }
}

class ScrobbleLogoutCommand :
    CliktCommand(name = "logout"),
    KoinComponent {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Disconnect and remove saved Last.fm session"

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo scrobble logout"

    override fun run() {
        if (GlobalContext.getOrNull() == null) {
            startKoin { modules(appModule) }
        }
        try {
            runBlocking {
                val scrobbling: ScrobblingRepository by inject()
                scrobbling.logout()
                terminal.println(green("Logged out from Last.fm."))
            }
        } finally {
            stopKoin()
        }
    }
}
