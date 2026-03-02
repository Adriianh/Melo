package com.github.adriianh.cli.config

import java.util.Properties

object Messages {
    private val props = Properties().apply {
        Messages::class.java.getResourceAsStream("/messages.properties")
            ?.use { load(it) }
    }

    fun get(key: String, vararg replacements: Pair<String, String>): String {
        var message = props.getProperty(key) ?: "[$key]"
        replacements.forEach { (placeholder, value) ->
            message = message.replace("{$placeholder}", value)
        }
        return message
    }
}

