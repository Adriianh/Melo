package com.github.adriianh.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemePreset(val displayName: String) {
    DEFAULT("Default"),
    CATPPUCCIN_MOCHA("Catppuccin Mocha"),
    GRUVBOX("Gruvbox"),
    NORD("Nord"),
    TOKYO_NIGHT("Tokyo Night")
}

@Serializable
data class Settings(
    val theme: ThemePreset = ThemePreset.DEFAULT,
    val volume: Int = 75,
    val searchLanguage: String = "en",
    val artworkResolution: Int = 300,
    val cacheSizeLimitMb: Int = 500
)