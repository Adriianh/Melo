package com.github.adriianh.cli.command.player.handler

import com.github.adriianh.cli.command.player.util.ItemPicker
import com.github.adriianh.cli.service.DaemonManager
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.usecase.offline.DownloadTrackUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.terminal.Terminal
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import org.koin.core.component.KoinComponent
import com.github.ajalt.mordant.rendering.TextColors.yellow as mordantYellow

object SearchActionHandler : KoinComponent {
    suspend fun handleTrackAction(
        track: Track,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrack: DownloadTrackUseCase,
        terminal: Terminal = Terminal(),
    ) {
        val actions = listOf("Play", "Play (foreground)", "Download", "Cancel")
        val selectedAction =
            ItemPicker.pickItem(
                actions,
                "What would you like to do with '${track.title}'?",
            ) { _, action, isSelected ->
                if (isSelected) {
                    yellow { textLine("> $action") }
                } else {
                    textLine("  $action")
                }
            }
        when (selectedAction) {
            "Play" -> {
                if (DaemonManager.ensureDaemonRunning(terminal)) {
                    if (DaemonManager.playTrack(track)) {
                        terminal.println(green("▶ Playing: ") + "${track.title} by ${track.artist}")
                        terminal.println(
                            gray("Playing in background. Use 'melo status' to inspect, 'melo pause' to pause."),
                        )
                        return
                    }
                }
                terminal.println(
                    mordantYellow("Warning: Could not start daemon. Falling back to foreground playback."),
                )
                PlayActionHandler.playTrack(track, getStream, terminal)
            }
            "Play (foreground)" -> PlayActionHandler.playTrack(track, getStream, terminal)
            "Download" ->
                DownloadActionHandler.downloadTrack(
                    track,
                    getStream,
                    getSettings,
                    downloadTrack,
                    terminal,
                )

            else -> terminal.println("Action cancelled.")
        }
    }
}
