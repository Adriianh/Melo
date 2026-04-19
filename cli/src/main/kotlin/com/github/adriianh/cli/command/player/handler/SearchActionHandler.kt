package com.github.adriianh.cli.command.player.handler

import com.github.adriianh.cli.command.player.ItemPicker
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.usecase.offline.DownloadTrackUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.ajalt.mordant.terminal.Terminal
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import org.koin.core.component.KoinComponent

object SearchActionHandler : KoinComponent {

    suspend fun handleTrackAction(
        track: Track,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrack: DownloadTrackUseCase,
        terminal: Terminal = Terminal()
    ) {
        val actions = listOf("Play with ffplay", "Download", "Cancel")
        val selectedAction = ItemPicker.pickItem(
            actions,
            "What would you like to do with '${track.title}'?"
        ) { _, action, isSelected ->
            if (isSelected) {
                yellow { textLine("> $action") }
            } else {
                textLine("  $action")
            }
        }
        when (selectedAction) {
            "Play with ffplay" -> PlayActionHandler.playTrack(track, getStream, terminal)
            "Download" -> DownloadActionHandler.downloadTrack(
                track,
                getStream,
                getSettings,
                downloadTrack,
                terminal
            )

            else -> terminal.println("Action cancelled.")
        }
    }
}