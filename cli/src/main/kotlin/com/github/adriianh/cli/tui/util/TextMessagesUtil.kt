package com.github.adriianh.cli.tui.util

import com.github.adriianh.cli.tui.*

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

object TextMessagesUtil {
    fun buildGreeting(): String {
        val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
        return when {
            hour < 12 -> "  Good morning"
            hour < 18 -> "  Good afternoon"
            else      -> "  Good evening"
        }
    }
}