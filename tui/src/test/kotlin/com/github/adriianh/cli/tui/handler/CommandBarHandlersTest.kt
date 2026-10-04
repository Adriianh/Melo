package com.github.adriianh.cli.tui.handler

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
    }

    @Test
    fun testCommandSortingExactMatchFirst() {
        val suggestions = CommandBarHandlers.computeSuggestions("play")
        assertTrue(suggestions.isNotEmpty())
        assertEquals("play", suggestions.first())
    }
}
