package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.RepeatMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SearchTab
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionNext
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionPrevious
import com.github.adriianh.cli.tui.handler.playback.clearQueue
import com.github.adriianh.cli.tui.handler.playback.toggleQueue
import com.github.adriianh.cli.tui.handler.search.performSearch
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlin.system.exitProcess

data class CommandResult(
    val errorMessage: String? = null,
    val keepBarOpen: Boolean = false,
    val restoreFocus: Boolean = true
)

abstract class Command(
    val names: List<String>,
    val argumentDescription: String? = null,
    val requiresArgument: Boolean = false
) {
    val suggestionTexts: List<String>
        get() = names.map { if (argumentDescription != null) "$it $argumentDescription" else it }

    abstract fun MeloScreen.execute(arg: String?): CommandResult
}

object CommandBarHandlers {
    private val COMMANDS = listOf(
        object : Command(listOf("q", "quit")) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                exitProcess(0)
            }
        },
        object : Command(listOf("settings")) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                applySidebarSelection(SidebarSection.SETTINGS)
                activateSidebarSelection(SidebarSection.SETTINGS)
                return CommandResult(restoreFocus = false)
            }
        },
        object : Command(listOf("vol"), "<0-100>", requiresArgument = true) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                val vol = arg?.toIntOrNull()
                return if (vol != null && vol in 0..100) {
                    audioPlayer.setVolume(vol)
                    state = state.copy(player = state.player.copy(volume = vol))
                    CommandResult()
                } else {
                    CommandResult(errorMessage = "Usage: vol <0-100>", keepBarOpen = true)
                }
            }
        },
        object : Command(listOf("skip", "next")) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                handleMediaSessionNext()
                return CommandResult()
            }
        },
        object : Command(listOf("prev")) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                handleMediaSessionPrevious()
                return CommandResult()
            }
        },
        object : Command(listOf("queue"), "<clear|open>", requiresArgument = true) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                return when (arg) {
                    "clear" -> {
                        clearQueue()
                        CommandResult()
                    }

                    "open" -> {
                        toggleQueue()
                        CommandResult()
                    }

                    else -> CommandResult(
                        errorMessage = "Usage: queue <clear|open>",
                        keepBarOpen = true
                    )
                }
            }
        },
        object : Command(listOf("shuffle"), "<on|off>", requiresArgument = true) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                return when (arg) {
                    "on" -> {
                        state = state.copy(player = state.player.copy(shuffleEnabled = true))
                        CommandResult()
                    }

                    "off" -> {
                        state = state.copy(player = state.player.copy(shuffleEnabled = false))
                        CommandResult()
                    }

                    else -> CommandResult(
                        errorMessage = "Usage: shuffle <on|off>",
                        keepBarOpen = true
                    )
                }
            }
        },
        object : Command(listOf("repeat"), "<off|one|all>", requiresArgument = true) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                return when (arg) {
                    "off" -> {
                        state = state.copy(player = state.player.copy(repeatMode = RepeatMode.OFF))
                        CommandResult()
                    }

                    "one" -> {
                        state = state.copy(player = state.player.copy(repeatMode = RepeatMode.ONE))
                        CommandResult()
                    }

                    "all" -> {
                        state = state.copy(player = state.player.copy(repeatMode = RepeatMode.ALL))
                        CommandResult()
                    }

                    else -> CommandResult(
                        errorMessage = "Usage: repeat <off|one|all>",
                        keepBarOpen = true
                    )
                }
            }
        },
        object : Command(listOf("pause")) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                state = state.copy(player = state.player.copy(isPlaying = false))
                audioPlayer.pause()
                return CommandResult()
            }
        },
        object : Command(listOf("play", "resume")) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                state = state.copy(player = state.player.copy(isPlaying = true))
                audioPlayer.resume()
                return CommandResult()
            }
        },
        object : Command(
            listOf("goto"),
            "<home|search|library|nowplaying|statistics|downloads>",
            requiresArgument = true
        ) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                val section = when (arg) {
                    "home" -> SidebarSection.HOME
                    "search" -> SidebarSection.SEARCH
                    "library" -> SidebarSection.LIBRARY
                    "nowplaying" -> SidebarSection.NOW_PLAYING
                    "statistics" -> SidebarSection.STATS
                    "downloads" -> SidebarSection.OFFLINE
                    else -> return CommandResult(
                        errorMessage = "Usage: goto <home|search|library|nowplaying|statistics|downloads>",
                        keepBarOpen = true
                    )
                }
                applySidebarSelection(section)
                activateSidebarSelection(section)
                return CommandResult(restoreFocus = false)
            }
        },
        object : Command(listOf("search", "track", "song"), "<name>", requiresArgument = true) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                return if (!arg.isNullOrBlank()) {
                    applySidebarSelection(SidebarSection.SEARCH)
                    activateSidebarSelection(SidebarSection.SEARCH)
                    state =
                        state.copy(screen = ScreenState.Search(query = arg, tab = SearchTab.SONGS))
                    searchInputState.setText(arg)
                    performSearch()
                    CommandResult(restoreFocus = false)
                } else {
                    CommandResult(errorMessage = "Usage: track <name>", keepBarOpen = true)
                }
            }
        },
        object : Command(listOf("album"), "<name>", requiresArgument = true) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                return if (!arg.isNullOrBlank()) {
                    applySidebarSelection(SidebarSection.SEARCH)
                    activateSidebarSelection(SidebarSection.SEARCH)
                    state =
                        state.copy(screen = ScreenState.Search(query = arg, tab = SearchTab.ALBUMS))
                    searchInputState.setText(arg)
                    performSearch()
                    CommandResult(restoreFocus = false)
                } else {
                    CommandResult(errorMessage = "Usage: album <name>", keepBarOpen = true)
                }
            }
        },
        object : Command(listOf("artist"), "<name>", requiresArgument = true) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                return if (!arg.isNullOrBlank()) {
                    applySidebarSelection(SidebarSection.SEARCH)
                    activateSidebarSelection(SidebarSection.SEARCH)
                    state = state.copy(
                        screen = ScreenState.Search(
                            query = arg,
                            tab = SearchTab.ARTISTS
                        )
                    )
                    searchInputState.setText(arg)
                    performSearch()
                    CommandResult(restoreFocus = false)
                } else {
                    CommandResult(errorMessage = "Usage: artist <name>", keepBarOpen = true)
                }
            }
        },
        object : Command(listOf("playlist"), "<name>", requiresArgument = true) {
            override fun MeloScreen.execute(arg: String?): CommandResult {
                return if (!arg.isNullOrBlank()) {
                    applySidebarSelection(SidebarSection.SEARCH)
                    activateSidebarSelection(SidebarSection.SEARCH)
                    state = state.copy(
                        screen = ScreenState.Search(
                            query = arg,
                            tab = SearchTab.PLAYLISTS
                        )
                    )
                    searchInputState.setText(arg)
                    performSearch()
                    CommandResult(restoreFocus = false)
                } else {
                    CommandResult(errorMessage = "Usage: playlist <name>", keepBarOpen = true)
                }
            }
        }
    )

    private val ALL_COMMANDS_FLAT = COMMANDS.flatMap { it.suggestionTexts }

    fun computeSuggestions(input: String): List<String> {
        val trimmed = input.trimStart()
        if (trimmed.isBlank()) return ALL_COMMANDS_FLAT

        val parts = trimmed.split(Regex("\\s+"), limit = 2)
        val cmdInput = parts[0]
        val hasSpace = trimmed.length > cmdInput.length

        if (hasSpace) {
            val matchingCommands = COMMANDS.filter { cmd ->
                cmd.names.any { it.equals(cmdInput, ignoreCase = true) }
            }
            return matchingCommands.flatMap { cmd ->
                if (cmd.requiresArgument) {
                    cmd.names.filter { it.equals(cmdInput, ignoreCase = true) }
                        .map { "$it ${cmd.argumentDescription ?: ""}" }
                } else {
                    emptyList()
                }
            }
        }

        return COMMANDS.flatMap { cmd ->
            cmd.names.filter { it.contains(cmdInput, ignoreCase = true) }
                .map { if (cmd.requiresArgument) "$it ${cmd.argumentDescription ?: ""}" else it }
        }.sortedWith(
            compareByDescending<String> {
                it.split(" ").first().equals(cmdInput, ignoreCase = true)
            }
                .thenByDescending { it.split(" ").first().startsWith(cmdInput, ignoreCase = true) }
                .thenBy { it }
        ).take(8)
    }

    fun MeloScreen.handleCommandBarKey(event: KeyEvent): EventResult {
        if (!state.commandBar.isVisible) return EventResult.UNHANDLED
        val barState = state.commandBar
        when (event.code()) {
            KeyCode.ESCAPE -> {
                closeCommandBar()
                return EventResult.HANDLED
            }

            KeyCode.ENTER -> {
                var inputToExecute = barState.input.trim()

                if (barState.selectedSuggestionIndex != null && barState.suggestions.isNotEmpty()) {
                    val sug = barState.suggestions[barState.selectedSuggestionIndex]
                    val parts = sug.split(" ")
                    val isTemplate = parts.size > 1 && parts[1].startsWith("<")
                    val completedWord = if (isTemplate) parts[0] + " " else sug

                    if (isTemplate && barState.input.trim().split(Regex("\\s+")).size == 1) {
                        state = state.copy(
                            commandBar = barState.copy(
                                input = completedWord,
                                cursorPosition = completedWord.length,
                                suggestions = computeSuggestions(completedWord),
                                selectedSuggestionIndex = null
                            )
                        )
                        return EventResult.HANDLED
                    } else {
                        if (!isTemplate) {
                            inputToExecute = completedWord
                        }
                    }
                }

                if (inputToExecute.isNotEmpty()) {
                    executeCommand(inputToExecute)
                } else {
                    closeCommandBar()
                }
                return EventResult.HANDLED
            }

            KeyCode.UP -> {
                if (barState.suggestions.isNotEmpty()) {
                    val prevIndex = if (barState.selectedSuggestionIndex == null) -1 else maxOf(
                        -1,
                        barState.selectedSuggestionIndex - 1
                    )
                    state = state.copy(
                        commandBar = barState.copy(
                            selectedSuggestionIndex = if (prevIndex == -1) null else prevIndex
                        )
                    )
                }
                return EventResult.HANDLED
            }

            KeyCode.DOWN -> {
                if (barState.suggestions.isNotEmpty()) {
                    val nextIndex = if (barState.selectedSuggestionIndex == null) 0 else minOf(
                        barState.suggestions.size - 1,
                        barState.selectedSuggestionIndex + 1
                    )
                    state = state.copy(
                        commandBar = barState.copy(
                            selectedSuggestionIndex = nextIndex
                        )
                    )
                }
                return EventResult.HANDLED
            }

            KeyCode.BACKSPACE -> {
                if (barState.input.isNotEmpty()) {
                    val newInput = barState.input.substring(0, barState.input.length - 1)
                    state = state.copy(
                        commandBar = barState.copy(
                            input = newInput,
                            cursorPosition = maxOf(0, barState.cursorPosition - 1),
                            errorMessage = null,
                            suggestions = computeSuggestions(newInput),
                            selectedSuggestionIndex = null
                        )
                    )
                }
                return EventResult.HANDLED
            }

            KeyCode.CHAR -> {
                val c = event.character()
                val newInput = barState.input + c
                state = state.copy(
                    commandBar = barState.copy(
                        input = newInput,
                        cursorPosition = barState.cursorPosition + 1,
                        errorMessage = null,
                        suggestions = computeSuggestions(newInput),
                        selectedSuggestionIndex = null
                    )
                )
                return EventResult.HANDLED
            }

            else -> {}
        }
        return EventResult.HANDLED
    }

    private fun MeloScreen.closeCommandBar(restoreFocus: Boolean = true) {
        val prevFocus = state.commandBar.previousFocusId
        state = state.copy(
            commandBar = state.commandBar.copy(
                isVisible = false,
                previousFocusId = null
            )
        )
        if (restoreFocus) {
            if (prevFocus != null) {
                appRunner()?.focusManager()?.setFocus(prevFocus)
            } else {
                appRunner()?.focusManager()?.setFocus("home-panel")
            }
        }
    }

    private fun MeloScreen.executeCommand(input: String) {
        val parts = input.split(" ", limit = 2)
        val commandName = parts[0]
        val arg = parts.getOrNull(1)

        val command = COMMANDS.firstOrNull { cmd ->
            cmd.names.any { it.equals(commandName, ignoreCase = true) }
        }

        val newHistory = (state.commandBar.history + input).takeLast(50)

        val result = command?.run { execute(arg) }
            ?: CommandResult(
                errorMessage = "Unrecognized command: $commandName",
                keepBarOpen = true
            )

        state = state.copy(
            commandBar = state.commandBar.copy(
                isVisible = result.keepBarOpen,
                input = if (result.keepBarOpen) state.commandBar.input else "",
                history = newHistory,
                historyIndex = newHistory.size,
                errorMessage = result.errorMessage,
                previousFocusId = if (result.keepBarOpen) state.commandBar.previousFocusId else null
            )
        )

        if (!result.keepBarOpen && result.restoreFocus) {
            val prevFocus = state.commandBar.previousFocusId
            if (prevFocus != null) {
                appRunner()?.focusManager()?.setFocus(prevFocus)
            } else {
                appRunner()?.focusManager()?.setFocus("home-panel")
            }
        }
    }
}
