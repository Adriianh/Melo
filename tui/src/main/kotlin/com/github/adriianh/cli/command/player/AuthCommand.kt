package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.service.YouTubeAuthService
import com.github.adriianh.core.domain.usecase.playback.CompleteWebAuthUseCase
import com.github.adriianh.core.domain.usecase.playback.StartWebAuthUseCase
import com.github.adriianh.data.auth.BrowserAuthManager
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File

class AuthCommand : CliktCommand(name = "auth") {
    override fun help(context: Context): String =
        "Authentication management (YouTube Music, Last.fm)"

    init {
        subcommands(YouTubeAuthCommand(), LastFmAuthCommand())
    }

    override fun run() {}
}

class YouTubeAuthCommand : CliktCommand(name = "youtube") {
    override fun help(context: Context): String =
        "Manage YouTube Music authentication (import from browser, login with cookies, status)"

    init {
        subcommands(
            YouTubeStatusAuthCommand(),
            YouTubeImportAuthCommand(),
            YouTubeLoginAuthCommand(),
            YouTubeLogoutAuthCommand(),
        )
    }

    override fun run() {}
}

private inline fun <T> withKoin(block: () -> T): T {
    val startedHere = GlobalContext.getOrNull() == null
    if (startedHere) {
        startKoin { modules(appModule) }
    }
    return try {
        block()
    } finally {
        if (startedHere) {
            stopKoin()
        }
    }
}

class YouTubeStatusAuthCommand : CliktCommand(name = "status"), KoinComponent {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Check YouTube Music authentication status"

    override fun run() = withKoin {
        val authService: YouTubeAuthService by inject()
        runBlocking {
            val status = authService.getStatus()
            if (status.isLoggedIn) {
                terminal.println(green("✓ Logged in to YouTube Music"))
                terminal.println(cyan("  Account: ") + (status.accountName ?: "Unknown"))
            } else {
                terminal.println(yellow("○ Not logged in to YouTube Music"))
                if (status.error != null) {
                    terminal.println(red("  Error: ${status.error}"))
                }
                terminal.println(gray("\nTo log in:"))
                terminal.println(gray("  melo auth youtube import              # Auto-import from installed browser"))
                terminal.println(gray("  melo auth youtube login --file <path> # Import cookies.txt file"))
                terminal.println(gray("  melo auth youtube login --cookies \"\"  # Pass raw session cookies"))
            }
        }
    }
}

class YouTubeImportAuthCommand : CliktCommand(name = "import"), KoinComponent {
    private val terminal = Terminal()

    override fun help(context: Context): String =
        "Automatically import YouTube Music session from installed browsers (Chrome, Firefox, Edge, Brave, etc.)"

    override fun run() = withKoin {
        val authService: YouTubeAuthService by inject()
        terminal.println(cyan("Searching for active YouTube Music sessions in installed browsers..."))

        runBlocking {
            val result = authService.importFromBrowser()
            result.fold(
                onSuccess = { accountName ->
                    terminal.println(green("✓ Successfully imported YouTube Music session!"))
                    terminal.println(cyan("  Logged in as: ") + accountName)
                    terminal.println(gray("  Home feed and recommendations will now be personalized."))
                },
                onFailure = { error ->
                    terminal.println(red("✗ Failed to import browser cookies: ${error.message}"))
                    terminal.println(gray("\nAlternative methods:"))
                    terminal.println(gray("  1. Export cookies to a file and run: melo auth youtube login --file <cookies.txt>"))
                    terminal.println(gray("  2. Provide cookies directly: melo auth youtube login --cookies \"...\""))
                }
            )
        }
    }
}

class YouTubeLoginAuthCommand : CliktCommand(name = "login"), KoinComponent {
    private val terminal = Terminal()
    private val cookiesOption by option(
        "-c",
        "--cookies",
        help = "Raw cookie header string (e.g. \"SAPISID=...; SID=...\")"
    )
    private val fileOption by option(
        "-f",
        "--file",
        help = "Path to cookie file (supports Netscape cookies.txt or raw header)"
    )

    override fun help(context: Context): String =
        "Log in to YouTube Music using cookies or automated browser login"

    override fun run() = withKoin {
        val authService: YouTubeAuthService by inject()

        runBlocking {
            when {
                cookiesOption != null -> {
                    terminal.println(cyan("Verifying provided cookies..."))
                    val result = authService.loginWithCookies(cookiesOption!!)
                    handleResult(result)
                }

                fileOption != null -> {
                    val file = File(fileOption!!)
                    terminal.println(cyan("Reading and verifying cookies from: ${file.path}"))
                    val result = authService.loginWithCookiesFile(file)
                    handleResult(result)
                }

                BrowserAuthManager.findAvailableBrowser() != null -> {
                    terminal.println(cyan("Launching browser for YouTube Music login..."))
                    terminal.println(gray("Please log in to your Google account in the opened window."))
                    val result = authService.launchBrowserLogin()
                    handleResult(result)
                }

                else -> {
                    terminal.println(yellow("No supported desktop browser was found to launch."))
                    terminal.println(gray("Please provide cookies using one of the following options:"))
                    terminal.println(gray("  melo auth youtube login --cookies \"<raw-cookies>\""))
                    terminal.println(gray("  melo auth youtube login --file <path-to-cookies.txt>"))
                    terminal.println(gray("  melo auth youtube import"))
                }
            }
        }
    }

    private fun handleResult(result: Result<String>) {
        result.fold(
            onSuccess = { accountName ->
                terminal.println(green("✓ Successfully logged in to YouTube Music!"))
                terminal.println(cyan("  Logged in as: ") + accountName)
            },
            onFailure = { error ->
                terminal.println(red("✗ Login failed: ${error.message}"))
            }
        )
    }
}

class YouTubeLogoutAuthCommand : CliktCommand(name = "logout"), KoinComponent {
    private val terminal = Terminal()

    override fun help(context: Context): String =
        "Log out from YouTube Music and remove saved session cookies"

    override fun run() = withKoin {
        val authService: YouTubeAuthService by inject()
        runBlocking {
            authService.logout()
            terminal.println(green("✓ Successfully logged out from YouTube Music."))
        }
    }
}

class LastFmAuthCommand : CliktCommand(name = "lastfm"), KoinComponent {
    private val terminal = Terminal()
    private val token by argument(help = "The token from the Last.fm auth page (if completing auth)").optional()

    override fun help(context: Context): String = "Authenticate with Last.fm"

    override fun run() {
        val apiKey = resolveEnv("LASTFM_API_KEY")
        val sharedSecret = resolveEnv("LASTFM_SHARED_SECRET")

        if (apiKey == null || sharedSecret == null) {
            terminal.println(red("✗ LASTFM_API_KEY and LASTFM_SHARED_SECRET must be configured first."))
            terminal.println(gray("  Run: melo config set LASTFM_API_KEY <key>"))
            terminal.println(gray("  Run: melo config set LASTFM_SHARED_SECRET <secret>"))
            return
        }

        withKoin {
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
                        terminal.println(gray("Make sure you have configured LASTFM_API_KEY and LASTFM_SHARED_SECRET."))
                        terminal.println(gray("You can set them with: melo config set LASTFM_API_KEY <your_key>"))
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
        }
    }
}
