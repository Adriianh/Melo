package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal

class VolumeCommand :
    CliktCommand(
        name = "volume",
    ) {
    private val value by argument(
        name = "value",
        help = "Volume level (0-100), delta (+5/-5), or 'mute'/'unmute'. Leave empty to view current volume.",
    ).optional()

    private val terminal = Terminal()

    override fun help(context: Context): String =
        "View or adjust playback volume across active player or daemon."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo volume\n" +
            "  melo volume 80\n" +
            "  melo volume +5\n" +
            "  melo volume -10\n" +
            "  melo volume mute\n" +
            "  melo volume unmute"

    override fun run() {
        val arg = value
        if (arg == null) {
            handleGetVolume()
        } else {
            handleSetOrAdjustVolume(arg)
        }
    }

    private fun handleGetVolume() {
        val result = LocalIpcClient.sendCommand("VOLUME_GET")
        if (result.startsWith("OK")) {
            val vol = result.removePrefix("OK").trim()
            terminal.println(cyan("Current volume: ") + green("$vol%"))
        } else {
            terminal.println(yellow("Melo player or daemon is not running."))
        }
    }

    private fun handleSetOrAdjustVolume(arg: String) {
        when {
            arg.equals("mute", ignoreCase = true) ->
                applyVolumeChange("VOLUME_SET", "0", yellow("Playback muted (0%)."))

            arg.equals("unmute", ignoreCase = true) ->
                applyVolumeChange("VOLUME_SET", "75", green("Playback unmuted (75%)."))

            arg.startsWith("+") || arg.startsWith("-") -> {
                val delta = arg.toIntOrNull()
                if (delta != null) {
                    applyVolumeChange("VOLUME_ADJUST", delta.toString(), null)
                } else {
                    terminal.println(red("Invalid volume delta: '$arg'"))
                }
            }

            else -> {
                val level = arg.toIntOrNull()
                if (level != null) {
                    val clamped = level.coerceIn(0, 100)
                    applyVolumeChange(
                        "VOLUME_SET",
                        clamped.toString(),
                        green("Volume set to $clamped%")
                    )
                } else {
                    terminal.println(red("Invalid volume argument: '$arg'. Use 0-100, +step, -step, mute, or unmute."))
                }
            }
        }
    }

    private fun applyVolumeChange(
        cmd: String,
        payload: String,
        successMsg: String?,
    ) {
        val result = LocalIpcClient.sendCommand(cmd, payload)
        if (result.startsWith("OK")) {
            val newVol = result.removePrefix("OK").trim()
            if (successMsg != null) {
                terminal.println(successMsg)
            } else {
                terminal.println(green("Volume set to $newVol%"))
            }
        } else {
            terminal.println(red("Failed to adjust volume: $result"))
        }
    }
}
