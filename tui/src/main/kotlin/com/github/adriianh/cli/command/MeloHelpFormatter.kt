package com.github.adriianh.cli.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.output.HelpFormatter.ParameterHelp
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.widgets.Text

enum class CommandCategory(
    val title: String,
) {
    PLAYBACK("Playback Controls"),
    DISCOVERY("Now Playing & Discovery"),
    QUEUE("Queue & Playlists"),
    LIBRARY("Library & Offline"),
    SERVICES("Services & Configuration"),
    OTHER("Other Commands"),
}

open class MeloHelpFormatter(
    context: Context,
    requiredOptionMarker: String? = null,
    showDefaultValues: Boolean = false,
    showRequiredTag: Boolean = false,
) : MordantHelpFormatter(
    context = context,
    requiredOptionMarker = requiredOptionMarker,
    showDefaultValues = showDefaultValues,
    showRequiredTag = showRequiredTag,
) {
    companion object {
        val COMMAND_ORDER: List<String> =
            listOf(
                // Playback
                "play",
                "pause",
                "resume",
                "next",
                "prev",
                "stop",
                "radio",
                // Discovery
                "status",
                "lyrics",
                "search",
                "discover",
                "history",
                "share",
                // Queue & Playlists
                "queue",
                "playlist",
                // Library & Offline
                "download",
                "library",
                "tag",
                // Services & Config
                "daemon",
                "auth",
                "config",
                "stats",
                "scrobble",
                "rpc",
            )

        val COMMAND_CATEGORIES: Map<String, CommandCategory> =
            mapOf(
                // Playback Controls
                "play" to CommandCategory.PLAYBACK,
                "pause" to CommandCategory.PLAYBACK,
                "resume" to CommandCategory.PLAYBACK,
                "next" to CommandCategory.PLAYBACK,
                "prev" to CommandCategory.PLAYBACK,
                "stop" to CommandCategory.PLAYBACK,
                "radio" to CommandCategory.PLAYBACK,
                // Now Playing & Discovery
                "status" to CommandCategory.DISCOVERY,
                "lyrics" to CommandCategory.DISCOVERY,
                "search" to CommandCategory.DISCOVERY,
                "discover" to CommandCategory.DISCOVERY,
                "history" to CommandCategory.DISCOVERY,
                "share" to CommandCategory.DISCOVERY,
                // Queue & Playlists
                "queue" to CommandCategory.QUEUE,
                "playlist" to CommandCategory.QUEUE,
                // Library & Offline
                "download" to CommandCategory.LIBRARY,
                "library" to CommandCategory.LIBRARY,
                "tag" to CommandCategory.LIBRARY,
                // Services & Configuration
                "daemon" to CommandCategory.SERVICES,
                "auth" to CommandCategory.SERVICES,
                "config" to CommandCategory.SERVICES,
                "stats" to CommandCategory.SERVICES,
                "scrobble" to CommandCategory.SERVICES,
                "rpc" to CommandCategory.SERVICES,
            )
    }

    override fun renderEpilog(epilog: String): Widget =
        Text(epilog, whitespace = Whitespace.PRE_WRAP)

    override fun renderCommands(parameters: List<ParameterHelp>): List<RenderedSection<Widget>> {
        val subcommands = parameters.filterIsInstance<ParameterHelp.Subcommand>()
        if (subcommands.none { it.name in COMMAND_CATEGORIES }) {
            return super.renderCommands(parameters)
        }

        val grouped = subcommands.groupBy { COMMAND_CATEGORIES[it.name] ?: CommandCategory.OTHER }

        return CommandCategory.entries.mapNotNull { category ->
            val commandsInCat = grouped[category] ?: return@mapNotNull null
            if (commandsInCat.isEmpty()) return@mapNotNull null

            val sorted =
                commandsInCat.sortedBy { cmd ->
                    val idx = COMMAND_ORDER.indexOf(cmd.name)
                    if (idx == -1) 999 else idx
                }

            val rows =
                sorted.map { cmd ->
                    DefinitionRow(
                        styleSubcommandName(cmd.name),
                        renderParameterHelpText(cmd.help, cmd.tags),
                    )
                }
            val title = styleSectionTitle(renderSectionTitle(category.title))
            RenderedSection(title, buildParameterList(rows))
        }
    }
}
