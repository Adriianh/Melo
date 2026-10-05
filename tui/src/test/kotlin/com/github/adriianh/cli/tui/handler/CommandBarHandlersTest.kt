package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.component.HelpRow
import com.github.adriianh.cli.tui.component.buildHelpRows
import com.github.adriianh.core.domain.model.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandBarHandlersTest {
    @Test
    fun testEmptyInputReturnsCappedSuggestions() {
        val suggestions = CommandBarHandlers.computeSuggestions("")
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.size > 6)
    }

    @Test
    fun testSubArgumentSuggestionsForGoto() {
        val gotoCommand = CommandBarHandlers.computeSuggestions("goto")
        assertEquals(listOf("goto <view>"), gotoCommand)

        val allGoto = CommandBarHandlers.computeSuggestions("goto ")
        assertTrue(allGoto.isNotEmpty())
        assertEquals(7, allGoto.size)
        assertTrue(allGoto.all { it.startsWith("goto ") })
        assertTrue(allGoto.contains("goto home"))

        val filteredGoto = CommandBarHandlers.computeSuggestions("goto h")
        assertEquals(listOf("goto home"), filteredGoto)

        val settingsGoto = CommandBarHandlers.computeSuggestions("goto set")
        assertEquals(listOf("goto settings"), settingsGoto)
    }

    @Test
    fun testSubArgumentSuggestionsForRepeat() {
        val allRepeat = CommandBarHandlers.computeSuggestions("repeat ")
        assertEquals(listOf("repeat off", "repeat one", "repeat all"), allRepeat)

        val filteredRepeat = CommandBarHandlers.computeSuggestions("repeat o")
        assertEquals(listOf("repeat off", "repeat one"), filteredRepeat)
    }

    @Test
    fun testSubArgumentSuggestionsForShuffleAndQueue() {
        val shuffleSuggestions = CommandBarHandlers.computeSuggestions("shuffle ")
        assertEquals(listOf("shuffle on", "shuffle off"), shuffleSuggestions)

        val queueSuggestions = CommandBarHandlers.computeSuggestions("queue ")
        assertEquals(listOf("queue clear", "queue open"), queueSuggestions)
    }

    @Test
    fun testSubArgumentSuggestionsForUpdate() {
        val allUpdate = CommandBarHandlers.computeSuggestions("update ")
        assertEquals(listOf("update nightly", "update stable"), allUpdate)

        val filteredUpdate = CommandBarHandlers.computeSuggestions("update n")
        assertEquals(listOf("update nightly"), filteredUpdate)

        val allUpgrade = CommandBarHandlers.computeSuggestions("upgrade ")
        assertEquals(listOf("upgrade nightly", "upgrade stable"), allUpgrade)
    }

    @Test
    fun testCommandWithTemplateArgument() {
        val volTemplate = CommandBarHandlers.computeSuggestions("vol ")
        assertEquals(listOf("vol <0-100>"), volTemplate)

        val volWithArg = CommandBarHandlers.computeSuggestions("vol 50")
        assertTrue(volWithArg.isEmpty())
    }

    @Test
    fun testCommandWithoutArgumentShowsEmptyOnTrailingSpace() {
        val pauseWithSpace = CommandBarHandlers.computeSuggestions("pause ")
        assertTrue(pauseWithSpace.isEmpty())

        val quitWithSpace = CommandBarHandlers.computeSuggestions("q ")
        assertTrue(quitWithSpace.isEmpty())
    }

    @Test
    fun testNewCommandsAreRecognized() {
        val help = CommandBarHandlers.computeSuggestions("help")
        assertTrue(help.contains("help"))

        val like = CommandBarHandlers.computeSuggestions("like")
        assertTrue(like.contains("like"))

        val lyrics = CommandBarHandlers.computeSuggestions("lyrics")
        assertTrue(lyrics.contains("lyrics"))

        val question = CommandBarHandlers.computeSuggestions("?")
        assertTrue(question.contains("?"))

        val update = CommandBarHandlers.computeSuggestions("update")
        assertTrue(update.contains("update"))

        val upgrade = CommandBarHandlers.computeSuggestions("upgrade")
        assertTrue(upgrade.contains("upgrade"))
    }

    @Test
    fun testCommandSortingExactMatchFirst() {
        val suggestions = CommandBarHandlers.computeSuggestions("play")
        assertTrue(suggestions.isNotEmpty())
        assertEquals("play", suggestions.first())
    }

    @Test
    fun testAllCommandsHaveCategories() {
        assertTrue(CommandBarHandlers.COMMANDS.all { it.category in CommandCategory.entries })
        val categories = CommandBarHandlers.COMMANDS.map { it.category }.toSet()
        assertEquals(setOf(CommandCategory.PLAYBACK, CommandCategory.NAVIGATION, CommandCategory.GENERAL), categories)
    }

    @Test
    fun testBuildHelpRowsProducesExpectedHeaders() {
        val rows = buildHelpRows(Settings())
        assertTrue(rows.isNotEmpty())
        val headers = rows.filterIsInstance<HelpRow.Header>().map { it.title }
        assertTrue(headers.contains(CommandCategory.PLAYBACK.title))
        assertTrue(headers.contains(CommandCategory.NAVIGATION.title))
        assertTrue(headers.contains(CommandCategory.GENERAL.title))
        assertTrue(headers.contains("Global Shortcuts"))
        assertTrue(headers.contains("Configured Actions"))

        val items = rows.filterIsInstance<HelpRow.Item>()
        val searchItem = items.first { it.trigger.startsWith(":search") }
        assertEquals("alias: :track, :song", searchItem.extra)
        val pauseItem = items.first { it.trigger == ":pause" }
        assertEquals(null, pauseItem.extra)
    }
}
