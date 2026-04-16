package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.RepeatMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SearchTab
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionNext
import com.github.adriianh.cli.tui.component.screen.handleMediaSessionPrevious
import com.github.adriianh.cli.tui.handler.playback.clearQueue
import com.github.adriianh.cli.tui.handler.search.performSearch
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlin.system.exitProcess

object CommandBarHandlers {
    fun MeloScreen.handleCommandBarKey(event: KeyEvent): EventResult {
        if (!state.commandBar.isVisible) return EventResult.UNHANDLED
        val barState = state.commandBar
        when (event.code()) {
            KeyCode.ESCAPE -> {
                state = state.copy(commandBar = barState.copy(isVisible = false))
                return EventResult.HANDLED
            }

            KeyCode.ENTER -> {
                val input = barState.input.trim()
                if (input.isNotEmpty()) {
                    executeCommand(input)
                } else {
                    state = state.copy(commandBar = barState.copy(isVisible = false))
                }
                return EventResult.HANDLED
            }

            KeyCode.BACKSPACE -> {
                if (barState.input.isNotEmpty()) {
                    state = state.copy(
                        commandBar = barState.copy(
                            input = barState.input.substring(0, barState.input.length - 1),
                            cursorPosition = maxOf(0, barState.cursorPosition - 1),
                            errorMessage = null
                        )
                    )
                }
                return EventResult.HANDLED
            }

            KeyCode.CHAR -> {
                val c = event.character()
                state = state.copy(
                    commandBar = barState.copy(
                        input = barState.input + c,
                        cursorPosition = barState.cursorPosition + 1,
                        errorMessage = null
                    )
                )
                return EventResult.HANDLED
            }

            else -> {}
        }
        return EventResult.HANDLED
    }

    private fun MeloScreen.executeCommand(input: String) {
        val parts = input.split(" ", limit = 2)
        val command = parts[0]
        val arg = parts.getOrNull(1)
        val newHistory = (state.commandBar.history + input).takeLast(50)
        var errorMessage: String? = null
        var isVisible = false
        when (command) {
            "q", "quit" -> {
                exitProcess(0)
            }

            "vol" -> {
                val vol = arg?.toIntOrNull()
                if (vol != null && vol in 0..100) {
                    audioPlayer.setVolume(vol)
                    state = state.copy(player = state.player.copy(volume = vol))
                } else {
                    errorMessage = "Usage: vol <0-100>"
                    isVisible = true
                }
            }

            "skip", "next" -> {
                handleMediaSessionNext()
            }

            "prev" -> {
                handleMediaSessionPrevious()
            }

            "queue" -> {
                if (arg == "clear") {
                    clearQueue()
                } else {
                    errorMessage = "Usage: queue clear"
                    isVisible = true
                }
            }

            "shuffle" -> {
                when (arg) {
                    "on" -> state = state.copy(player = state.player.copy(shuffleEnabled = true))
                    "off" -> state = state.copy(player = state.player.copy(shuffleEnabled = false))
                    else -> {
                        errorMessage = "Usage: shuffle <on|off>"
                        isVisible = true
                    }
                }
            }

            "repeat" -> {
                when (arg) {
                    "off" -> state =
                        state.copy(player = state.player.copy(repeatMode = RepeatMode.OFF))

                    "one" -> state =
                        state.copy(player = state.player.copy(repeatMode = RepeatMode.ONE))

                    "all" -> state =
                        state.copy(player = state.player.copy(repeatMode = RepeatMode.ALL))

                    else -> {
                        errorMessage = "Usage: repeat <off|one|all>"
                        isVisible = true
                    }
                }
            }

            "goto" -> {
                when (arg) {
                    "home" -> {
                        applySidebarSelection(SidebarSection.HOME); activateSidebarSelection(
                            SidebarSection.HOME
                        )
                    }

                    "search" -> {
                        applySidebarSelection(SidebarSection.SEARCH); activateSidebarSelection(
                            SidebarSection.SEARCH
                        )
                    }

                    "library" -> {
                        applySidebarSelection(SidebarSection.LIBRARY); activateSidebarSelection(
                            SidebarSection.LIBRARY
                        )
                    }

                    "nowplaying" -> {
                        applySidebarSelection(SidebarSection.NOW_PLAYING); activateSidebarSelection(
                            SidebarSection.NOW_PLAYING
                        )
                    }

                    else -> {
                        errorMessage = "Usage: goto <home|search|library|nowplaying>"
                        isVisible = true
                    }
                }
            }

            "artist" -> {
                if (!arg.isNullOrBlank()) {
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
                } else {
                    errorMessage = "Usage: artist <name>"
                    isVisible = true
                }
            }

            else -> {
                errorMessage = "Unrecognized command: $command"
                isVisible = true
            }
        }
        state = state.copy(
            commandBar = state.commandBar.copy(
                isVisible = isVisible,
                input = if (isVisible) state.commandBar.input else "",
                history = newHistory,
                historyIndex = newHistory.size,
                errorMessage = errorMessage
            )
        )
    }
}
