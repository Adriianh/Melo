package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.CommandBarState
import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.HelpOverlayState
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SearchTab
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionNext
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionPrevious
import com.github.adriianh.cli.tui.component.screen.onStopLifecycle
import com.github.adriianh.cli.tui.handler.playback.clearQueue
import com.github.adriianh.cli.tui.handler.playback.setRepeatMode
import com.github.adriianh.cli.tui.handler.playback.setShuffleEnabled
import com.github.adriianh.cli.tui.handler.playback.setVolumePercent
import com.github.adriianh.cli.tui.handler.playback.togglePlayPause
import com.github.adriianh.cli.tui.handler.playback.toggleQueue
import com.github.adriianh.cli.tui.handler.search.loadLyricsIfNeeded
import com.github.adriianh.cli.tui.handler.search.performSearch
import com.github.adriianh.cli.tui.util.ToastKind
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.core.util.MeloVersion
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

data class CommandResult(
    val errorMessage: String? = null,
    val keepBarOpen: Boolean = false,
    val restoreFocus: Boolean = true,
)

enum class CommandCategory(
    val title: String,
) {
    PLAYBACK("Playback & Queue"),
    NAVIGATION("Navigation & Search"),
    GENERAL("General & System"),
}

abstract class Command(
    val names: List<String>,
    val description: String? = null,
    val argumentDescription: String? = null,
    val requiresArgument: Boolean = false,
    val subArguments: List<String> = emptyList(),
    val category: CommandCategory = CommandCategory.GENERAL,
) {
    val suggestionTexts: List<String>
        get() = names.map { if (argumentDescription != null) "$it $argumentDescription" else it }

    abstract fun MeloScreen.execute(arg: String?): CommandResult
}

object CommandBarHandlers {
    internal val COMMANDS =
        listOf(
            object : Command(
                names = listOf("q", "quit"),
                description = "Quit the application",
                category = CommandCategory.GENERAL,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    onStopLifecycle()
                    appRunner()?.quit()
                    exitProcess(0)
                }
            },
            object : Command(
                names = listOf("settings"),
                description = "Open the settings panel",
                category = CommandCategory.GENERAL,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    applySidebarSelection(SidebarSection.SETTINGS)
                    activateSidebarSelection(SidebarSection.SETTINGS)
                    return CommandResult(restoreFocus = false)
                }
            },
            object : Command(
                names = listOf("update", "upgrade"),
                description = "Check for updates to Melo",
                category = CommandCategory.GENERAL,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    val updateUseCase = checkForUpdate
                    if (updateUseCase == null) {
                        showToast("Update service is not available", ToastKind.WARNING)
                        return CommandResult()
                    }
                    showToast("Checking for updates...", ToastKind.INFO)
                    scope.launch {
                        updateUseCase(MeloVersion.CURRENT).fold(
                            onSuccess = { release ->
                                appRunner()?.runOnRenderThread {
                                    if (release != null) {
                                        showToast(
                                            "Update v${release.version} available! Run 'melo update' to install.",
                                            ToastKind.SUCCESS,
                                        )
                                    } else {
                                        showToast("Melo is up to date (v${MeloVersion.CURRENT})", ToastKind.INFO)
                                    }
                                }
                            },
                            onFailure = { error ->
                                appRunner()?.runOnRenderThread {
                                    showToast("Update check failed: ${error.message}", ToastKind.ERROR)
                                }
                            },
                        )
                    }
                    return CommandResult()
                }
            },
            object : Command(
                names = listOf("vol"),
                description = "Set the playback volume",
                argumentDescription = "<0-100>",
                requiresArgument = true,
                category = CommandCategory.PLAYBACK,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    val vol = arg?.toIntOrNull()
                    return if (vol != null && vol in 0..100) {
                        setVolumePercent(vol)
                        CommandResult()
                    } else {
                        CommandResult(errorMessage = "Usage: vol <0-100>", keepBarOpen = true)
                    }
                }
            },
            object : Command(
                names = listOf("skip", "next"),
                description = "Skip to the next track",
                category = CommandCategory.PLAYBACK,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    handleMediaSessionNext()
                    return CommandResult()
                }
            },
            object : Command(
                names = listOf("prev"),
                description = "Go to the previous track",
                category = CommandCategory.PLAYBACK,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    handleMediaSessionPrevious()
                    return CommandResult()
                }
            },
            object : Command(
                names = listOf("queue"),
                description = "Open or clear the playback queue",
                argumentDescription = "<action>",
                requiresArgument = true,
                subArguments = listOf("clear", "open"),
                category = CommandCategory.PLAYBACK,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult =
                    when (arg) {
                        "clear" -> {
                            clearQueue()
                            CommandResult()
                        }

                        "open" -> {
                            toggleQueue()
                            CommandResult()
                        }

                        else ->
                            CommandResult(
                                errorMessage = "Usage: queue <clear|open>",
                                keepBarOpen = true,
                            )
                    }
            },
            object : Command(
                names = listOf("shuffle"),
                description = "Toggle shuffle mode",
                argumentDescription = "<state>",
                requiresArgument = true,
                subArguments = listOf("on", "off"),
                category = CommandCategory.PLAYBACK,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult =
                    when (arg) {
                        "on" -> {
                            setShuffleEnabled(true)
                            CommandResult()
                        }

                        "off" -> {
                            setShuffleEnabled(false)
                            CommandResult()
                        }

                        else ->
                            CommandResult(
                                errorMessage = "Usage: shuffle <on|off>",
                                keepBarOpen = true,
                            )
                    }
            },
            object : Command(
                names = listOf("repeat"),
                description = "Set the repeat mode",
                argumentDescription = "<mode>",
                requiresArgument = true,
                subArguments = listOf("off", "one", "all"),
                category = CommandCategory.PLAYBACK,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult =
                    when (arg) {
                        "off", "none" -> {
                            setRepeatMode(RepeatMode.NONE)
                            CommandResult()
                        }

                        "one" -> {
                            setRepeatMode(RepeatMode.ONE)
                            CommandResult()
                        }

                        "all" -> {
                            setRepeatMode(RepeatMode.ALL)
                            CommandResult()
                        }

                        else ->
                            CommandResult(
                                errorMessage = "Usage: repeat <off|one|all>",
                                keepBarOpen = true,
                            )
                    }
            },
            object : Command(
                names = listOf("pause"),
                description = "Pause playback",
                category = CommandCategory.PLAYBACK,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    if (playbackManager.playbackState.value.isPlaying) togglePlayPause()
                    return CommandResult()
                }
            },
            object : Command(
                names = listOf("play", "resume"),
                description = "Resume playback",
                category = CommandCategory.PLAYBACK,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    if (!playbackManager.playbackState.value.isPlaying) togglePlayPause()
                    return CommandResult()
                }
            },
            object : Command(
                names = listOf("goto"),
                description = "Navigate directly to a view or a screen",
                argumentDescription = "<view>",
                requiresArgument = true,
                subArguments =
                    listOf(
                        "home",
                        "search",
                        "library",
                        "nowplaying",
                        "statistics",
                        "downloads",
                        "settings",
                    ),
                category = CommandCategory.NAVIGATION,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    val section =
                        when (arg) {
                            "home" -> SidebarSection.HOME
                            "search" -> SidebarSection.SEARCH
                            "library" -> SidebarSection.LIBRARY
                            "nowplaying" -> SidebarSection.NOW_PLAYING
                            "statistics" -> SidebarSection.STATS
                            "downloads" -> SidebarSection.OFFLINE
                            "settings" -> SidebarSection.SETTINGS
                            else -> return CommandResult(
                                errorMessage = "Usage: goto <home|search|library|nowplaying|statistics|downloads>",
                                keepBarOpen = true,
                            )
                        }
                    applySidebarSelection(section)
                    activateSidebarSelection(section)
                    return CommandResult(restoreFocus = false)
                }
            },
            object : Command(
                names = listOf("search", "track", "song"),
                description = "Search for a track",
                argumentDescription = "<name>",
                requiresArgument = true,
                category = CommandCategory.NAVIGATION,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult =
                    if (!arg.isNullOrBlank()) {
                        applySidebarSelection(SidebarSection.SEARCH)
                        state =
                            state.copy(screen = ScreenState.Search(query = arg, tab = SearchTab.SONGS))
                        searchInputState.setText(arg)
                        performSearch()
                        CommandResult(restoreFocus = false)
                    } else {
                        CommandResult(errorMessage = "Usage: track <name>", keepBarOpen = true)
                    }
            },
            object : Command(
                names = listOf("album"),
                description = "Search for an album",
                argumentDescription = "<name>",
                requiresArgument = true,
                category = CommandCategory.NAVIGATION,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult =
                    if (!arg.isNullOrBlank()) {
                        applySidebarSelection(SidebarSection.SEARCH)
                        state =
                            state.copy(screen = ScreenState.Search(query = arg, tab = SearchTab.ALBUMS))
                        searchInputState.setText(arg)
                        performSearch()
                        CommandResult(restoreFocus = false)
                    } else {
                        CommandResult(errorMessage = "Usage: album <name>", keepBarOpen = true)
                    }
            },
            object : Command(
                names = listOf("artist"),
                description = "Search for an artist",
                argumentDescription = "<name>",
                requiresArgument = true,
                category = CommandCategory.NAVIGATION,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult =
                    if (!arg.isNullOrBlank()) {
                        applySidebarSelection(SidebarSection.SEARCH)
                        state =
                            state.copy(
                                screen =
                                    ScreenState.Search(
                                        query = arg,
                                        tab = SearchTab.ARTISTS,
                                    ),
                            )
                        searchInputState.setText(arg)
                        performSearch()
                        CommandResult(restoreFocus = false)
                    } else {
                        CommandResult(errorMessage = "Usage: artist <name>", keepBarOpen = true)
                    }
            },
            object : Command(
                names = listOf("playlist"),
                description = "Search for a playlist",
                argumentDescription = "<name>",
                requiresArgument = true,
                category = CommandCategory.NAVIGATION,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult =
                    if (!arg.isNullOrBlank()) {
                        applySidebarSelection(SidebarSection.SEARCH)
                        state =
                            state.copy(
                                screen =
                                    ScreenState.Search(
                                        query = arg,
                                        tab = SearchTab.PLAYLISTS,
                                    ),
                            )
                        searchInputState.setText(arg)
                        performSearch()
                        CommandResult(restoreFocus = false)
                    } else {
                        CommandResult(errorMessage = "Usage: playlist <name>", keepBarOpen = true)
                    }
            },
            object : Command(
                names = listOf("help", "?"),
                description = "Show available commands",
                category = CommandCategory.GENERAL,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    state = state.copy(helpOverlay = HelpOverlayState(isVisible = true, scrollOffset = 0))
                    appRunner()?.focusManager()?.setFocus("help-overlay-panel")
                    return CommandResult(restoreFocus = false)
                }
            },
            object : Command(
                names = listOf("like", "fav"),
                description = "Like the current track",
                category = CommandCategory.PLAYBACK,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    val currentTrack = playbackManager.playbackState.value.currentTrack
                    return if (currentTrack != null) {
                        toggleFavorite(currentTrack)
                        CommandResult()
                    } else {
                        CommandResult(errorMessage = "No track currently playing", keepBarOpen = true)
                    }
                }
            },
            object : Command(
                names = listOf("lyrics"),
                description = "Show lyrics of the current track",
                category = CommandCategory.PLAYBACK,
            ) {
                override fun MeloScreen.execute(arg: String?): CommandResult {
                    state =
                        state.copy(
                            detail =
                                state.detail.copy(
                                    detailTab = DetailTab.LYRICS,
                                    lyricsScrollOffset = 0,
                                    isAutoScrollLyrics = true,
                                ),
                        )
                    loadLyricsIfNeeded()
                    appRunner()?.focusManager()?.setFocus("detail-panel")
                    return CommandResult(restoreFocus = false)
                }
            },
        )

    private val ALL_COMMANDS_FLAT = COMMANDS.flatMap { it.suggestionTexts }

    fun computeSuggestions(input: String): List<String> {
        val trimmed = input.trimStart()
        if (trimmed.isBlank()) return ALL_COMMANDS_FLAT

        val parts = trimmed.split(Regex("\\s+"), limit = 2)
        val cmdInput = parts[0]
        val hasSpace = trimmed.length > cmdInput.length

        return if (hasSpace) {
            computeSubArgumentSuggestions(
                cmdInput,
                parts.getOrNull(1)?.trimStart() ?: "",
            )
        } else {
            computeCommandNameSuggestions(cmdInput)
        }
    }

    fun MeloScreen.handleCommandBarKey(event: KeyEvent): EventResult {
        if (!state.commandBar.isVisible) return EventResult.UNHANDLED
        val barState = state.commandBar
        when (event.code()) {
            KeyCode.ESCAPE -> closeCommandBar()
            KeyCode.ENTER -> handleEnter(barState)
            KeyCode.TAB -> handleTab(barState, event.modifiers().shift())
            KeyCode.UP -> handleArrowUp(barState)
            KeyCode.DOWN -> handleArrowDown(barState)
            KeyCode.LEFT, KeyCode.RIGHT, KeyCode.HOME, KeyCode.END ->
                handleCursorNavigation(event.code(), barState)
            KeyCode.BACKSPACE, KeyCode.DELETE -> handleEditingKey(event.code(), barState)
            KeyCode.CHAR -> handleChar(event, barState)
            else -> {}
        }
        return EventResult.HANDLED
    }

    fun findCommand(name: String): Command? =
        COMMANDS.firstOrNull { cmd ->
            cmd.names.any { it.equals(name, ignoreCase = true) }
        }
}

private fun computeSubArgumentSuggestions(
    cmdInput: String,
    argPrefix: String,
): List<String> {
    val matchingCommand =
        CommandBarHandlers.COMMANDS.firstOrNull { cmd ->
            cmd.names.any { it.equals(cmdInput, ignoreCase = true) }
        } ?: return emptyList()

    return if (matchingCommand.subArguments.isNotEmpty()) {
        matchingCommand.subArguments
            .filter { it.startsWith(argPrefix, ignoreCase = true) }
            .map { "$cmdInput $it" }
    } else if (matchingCommand.requiresArgument && argPrefix.isBlank()) {
        listOf("$cmdInput ${matchingCommand.argumentDescription ?: ""}")
    } else {
        emptyList()
    }
}

private fun computeCommandNameSuggestions(cmdInput: String): List<String> =
    CommandBarHandlers.COMMANDS
        .flatMap { cmd ->
            cmd.names
                .filter { it.contains(cmdInput, ignoreCase = true) }
                .map { if (cmd.requiresArgument) "$it ${cmd.argumentDescription ?: ""}" else it }
        }.sortedWith(
            compareByDescending<String> {
                it.split(" ").first().equals(cmdInput, ignoreCase = true)
            }.thenByDescending {
                it.split(" ").first().startsWith(cmdInput, ignoreCase = true)
            }.thenBy { it },
        )

private fun MeloScreen.handleTab(
    barState: CommandBarState,
    isShift: Boolean,
) {
    if (barState.suggestions.isEmpty()) return

    val total = barState.suggestions.size
    val newIndex =
        if (isShift) {
            if (barState.selectedSuggestionIndex == null) {
                total - 1
            } else {
                (barState.selectedSuggestionIndex - 1 + total) % total
            }
        } else {
            if (barState.selectedSuggestionIndex == null) {
                0
            } else {
                (barState.selectedSuggestionIndex + 1) % total
            }
        }

    state =
        state.copy(
            commandBar =
                barState.copy(
                    selectedSuggestionIndex = newIndex,
                ),
        )
}

private fun MeloScreen.handleArrowUp(barState: CommandBarState) {
    if (barState.selectedSuggestionIndex != null) {
        val prev = barState.selectedSuggestionIndex - 1
        state =
            state.copy(
                commandBar =
                    barState.copy(
                        selectedSuggestionIndex = if (prev < 0) null else prev,
                    ),
            )
        return
    }

    val history = barState.history
    if (history.isNotEmpty()) {
        val newIndex =
            if (barState.historyIndex in history.indices) {
                maxOf(0, barState.historyIndex - 1)
            } else {
                history.size - 1
            }
        val pastInput = history[newIndex]
        state =
            state.copy(
                commandBar =
                    barState.copy(
                        input = pastInput,
                        cursorPosition = pastInput.length,
                        historyIndex = newIndex,
                        suggestions = CommandBarHandlers.computeSuggestions(pastInput),
                        selectedSuggestionIndex = null,
                    ),
            )
    }
}

private fun MeloScreen.handleArrowDown(barState: CommandBarState) {
    if (barState.selectedSuggestionIndex == null && barState.historyIndex in barState.history.indices) {
        val newIndex = barState.historyIndex + 1
        if (newIndex < barState.history.size) {
            val pastInput = barState.history[newIndex]
            state =
                state.copy(
                    commandBar =
                        barState.copy(
                            input = pastInput,
                            cursorPosition = pastInput.length,
                            historyIndex = newIndex,
                            suggestions = CommandBarHandlers.computeSuggestions(pastInput),
                            selectedSuggestionIndex = null,
                        ),
                )
        } else {
            state =
                state.copy(
                    commandBar =
                        barState.copy(
                            input = "",
                            cursorPosition = 0,
                            historyIndex = barState.history.size,
                            suggestions = CommandBarHandlers.computeSuggestions(""),
                            selectedSuggestionIndex = null,
                        ),
                )
        }
        return
    }

    if (barState.suggestions.isNotEmpty()) {
        val next =
            if (barState.selectedSuggestionIndex == null) {
                0
            } else {
                minOf(barState.suggestions.size - 1, barState.selectedSuggestionIndex + 1)
            }
        state = state.copy(commandBar = barState.copy(selectedSuggestionIndex = next))
    }
}

private fun MeloScreen.handleCursorNavigation(
    code: KeyCode,
    barState: CommandBarState,
) {
    val inputLen = barState.input.length
    val newCursor =
        when (code) {
            KeyCode.LEFT -> maxOf(0, barState.cursorPosition - 1)
            KeyCode.RIGHT -> minOf(inputLen, barState.cursorPosition + 1)
            KeyCode.HOME -> 0
            KeyCode.END -> inputLen
            else -> barState.cursorPosition
        }
    state = state.copy(commandBar = barState.copy(cursorPosition = newCursor))
}

private fun MeloScreen.handleEditingKey(
    code: KeyCode,
    barState: CommandBarState,
) {
    val cursor = barState.cursorPosition.coerceIn(0, barState.input.length)
    val newInput =
        when (code) {
            KeyCode.BACKSPACE if cursor > 0 -> {
                barState.input.removeRange(cursor - 1, cursor)
            }
            KeyCode.DELETE if cursor < barState.input.length -> {
                barState.input.removeRange(cursor, cursor + 1)
            }
            else -> {
                return
            }
        }
    val newCursor = if (code == KeyCode.BACKSPACE) cursor - 1 else cursor
    state =
        state.copy(
            commandBar =
                barState.copy(
                    input = newInput,
                    cursorPosition = newCursor,
                    errorMessage = null,
                    suggestions = CommandBarHandlers.computeSuggestions(newInput),
                    selectedSuggestionIndex = null,
                ),
        )
}

private fun MeloScreen.handleChar(
    event: KeyEvent,
    barState: CommandBarState,
) {
    val str = event.string()
    val cursor = barState.cursorPosition.coerceIn(0, barState.input.length)
    val newInput = StringBuilder(barState.input).insert(cursor, str).toString()
    state =
        state.copy(
            commandBar =
                barState.copy(
                    input = newInput,
                    cursorPosition = cursor + str.length,
                    errorMessage = null,
                    suggestions = CommandBarHandlers.computeSuggestions(newInput),
                    selectedSuggestionIndex = null,
                ),
        )
}

private fun MeloScreen.handleEnter(barState: CommandBarState) {
    var inputToExecute = barState.input.trim()

    if (barState.selectedSuggestionIndex != null && barState.suggestions.isNotEmpty()) {
        val sug = barState.suggestions[barState.selectedSuggestionIndex]
        val parts = sug.split(" ")
        val isTemplate = parts.size > 1 && parts[1].startsWith("<")
        val completedWord = if (isTemplate) parts[0] + " " else sug

        if (isTemplate &&
            barState.input
                .trim()
                .split(Regex("\\s+"))
                .size == 1
        ) {
            state =
                state.copy(
                    commandBar =
                        barState.copy(
                            input = completedWord,
                            cursorPosition = completedWord.length,
                            suggestions = CommandBarHandlers.computeSuggestions(completedWord),
                            selectedSuggestionIndex = null,
                        ),
                )
            return
        } else if (!isTemplate) {
            inputToExecute = completedWord
        }
    }

    if (inputToExecute.isNotEmpty()) {
        executeCommand(inputToExecute)
    } else {
        closeCommandBar()
    }
}

private fun MeloScreen.closeCommandBar(restoreFocus: Boolean = true) {
    val prevFocus = state.commandBar.previousFocusId
    state =
        state.copy(
            commandBar =
                state.commandBar.copy(
                    isVisible = false,
                    previousFocusId = null,
                ),
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

    val command =
        CommandBarHandlers.COMMANDS.firstOrNull { cmd ->
            cmd.names.any { it.equals(commandName, ignoreCase = true) }
        }

    val newHistory = (state.commandBar.history + input).takeLast(50)

    val result =
        command?.run { execute(arg) }
            ?: CommandResult(
                errorMessage = "Unrecognized command: $commandName",
                keepBarOpen = true,
            )

    state =
        state.copy(
            commandBar =
                state.commandBar.copy(
                    isVisible = result.keepBarOpen,
                    input = if (result.keepBarOpen) state.commandBar.input else "",
                    history = newHistory,
                    historyIndex = newHistory.size,
                    errorMessage = result.errorMessage,
                    previousFocusId = if (result.keepBarOpen) state.commandBar.previousFocusId else null,
                ),
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
