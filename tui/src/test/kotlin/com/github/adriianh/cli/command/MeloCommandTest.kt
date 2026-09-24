package com.github.adriianh.cli.command

import com.github.ajalt.clikt.testing.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MeloCommandTest {
    @Test
    fun helpDisplaysAllFiveCategories() {
        val cmd = MeloCommand()
        val result = cmd.test("--help")

        assertEquals(0, result.statusCode, "melo --help should exit with code 0")
        val output = result.output

        assertTrue(output.contains("Playback Controls:"), "Missing Playback Controls category")
        assertTrue(
            output.contains("Now Playing & Discovery:"),
            "Missing Now Playing & Discovery category"
        )
        assertTrue(output.contains("Queue & Playlists:"), "Missing Queue & Playlists category")
        assertTrue(output.contains("Library & Offline:"), "Missing Library & Offline category")
        assertTrue(
            output.contains("Services & Configuration:"),
            "Missing Services & Configuration category"
        )
    }

    @Test
    fun helpContainsAllTwentyFourSubcommands() {
        val cmd = MeloCommand()
        val result = cmd.test("--help")
        val output = result.output

        val expectedCommands =
            listOf(
                // Playback Controls
                "play",
                "pause",
                "resume",
                "next",
                "prev",
                "stop",
                "radio",
                // Now Playing & Discovery
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
                // Services & Configuration
                "daemon",
                "auth",
                "config",
                "stats",
                "scrobble",
                "rpc",
            )

        assertEquals(24, expectedCommands.size, "Expected exactly 24 commands to be audited")

        for (name in expectedCommands) {
            assertTrue(
                output.contains(Regex("""\b$name\b""")),
                "Command '$name' should be listed in melo --help",
            )
        }
    }

    @Test
    fun helpIncludesEpilogWithExamplesAndTuiHint() {
        val cmd = MeloCommand()
        val result = cmd.test("--help")
        val output = result.output

        assertTrue(output.contains("Examples:"), "Help should contain Examples section")
        assertTrue(
            output.contains("melo play \"Bohemian Rhapsody\""),
            "Help should contain play example"
        )
        assertTrue(
            output.contains("launch the interactive TUI"),
            "Help should inform user how to launch TUI"
        )
    }

    @Test
    fun aliasesAreCorrectlyConfigured() {
        val cmd = MeloCommand()
        val aliases = cmd.aliases()

        assertEquals(listOf("play"), aliases["p"])
        assertEquals(listOf("download"), aliases["dl"])
        assertEquals(listOf("status"), aliases["np"])
        assertEquals(listOf("status"), aliases["st"])
        assertEquals(listOf("queue"), aliases["q"])
        assertEquals(listOf("config"), aliases["cfg"])
        assertEquals(listOf("prev"), aliases["previous"])
    }

    @Test
    fun aliasesExecuteTargetCommandsHelpSuccessfully() {
        val testAliases = listOf("p", "dl", "np", "st", "q", "cfg", "previous")

        for (alias in testAliases) {
            val cmd = MeloCommand()
            val result = cmd.test("$alias --help")
            assertEquals(
                0,
                result.statusCode,
                "Alias '$alias --help' should exit with code 0. Output: ${result.output}",
            )
        }
    }

    @Test
    fun subcommandsWithoutCategoriesFallbackToStandardCommandsSection() {
        val cmd = MeloCommand()
        val result = cmd.test("config", "--help")

        assertEquals(0, result.statusCode)
        val output = result.output

        assertTrue(
            output.contains("Commands:"),
            "Nested subcommands should use default 'Commands:' section"
        )
        assertTrue(output.contains("list"), "config --help should list 'list' subcommand")
        assertTrue(output.contains("set"), "config --help should list 'set' subcommand")
    }

    @Test
    fun allSubcommandsHaveValidCategoryMapping() {
        val allCategories = MeloHelpFormatter.COMMAND_CATEGORIES
        assertEquals(24, allCategories.size, "All 24 commands must be categorized")

        for ((cmd, cat) in allCategories) {
            assertNotNull(cat, "Command '$cmd' must map to a valid CommandCategory")
        }
    }
}
