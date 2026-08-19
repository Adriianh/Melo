package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.kmpalette.generatePalette
import com.kmpalette.loader.ByteArrayLoader
import com.kmpalette.palette.graphics.Palette
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

data class AccentPalette(
    val dominant: Color,
    val onDominant: Color,
) {
    fun harmonize(isDarkMode: Boolean): AccentPalette {
        val harmonized = ColorUtils.harmonize(dominant, isDarkMode)
        val onColor = if (harmonized.luminance() > 0.5f) Color.Black else Color.White
        return AccentPalette(dominant = harmonized, onDominant = onColor)
    }
}

object AccentColorExtractor {
    val fallback = AccentPalette(
        dominant = Color(0xFFFF2D55),
        onDominant = Color.White
    )

    fun fromPalette(palette: Palette): AccentPalette {
        val dominantSwatch = palette.dominantSwatch ?: palette.vibrantSwatch ?: palette.mutedSwatch
        val dominantColor = dominantSwatch?.rgb?.let { Color(it) } ?: Color(0xFFFF2D55)
        val onColor = if (dominantColor.luminance() > 0.5f) Color.Black else Color.White
        return AccentPalette(dominant = dominantColor, onDominant = onColor)
    }

    suspend fun fromImageBytes(bytes: ByteArray): AccentPalette {
        return runCatching {
            val bitmap = ByteArrayLoader.load(bytes)
            fromPalette(bitmap.generatePalette())
        }.getOrElse { fallback }
    }

    suspend fun fromImageUrl(url: String?, httpClient: HttpClient): AccentPalette {
        if (url.isNullOrBlank()) return fallback
        return runCatching {
            val bytes = httpClient.get(url).body<ByteArray>()
            fromImageBytes(bytes)
        }.getOrElse { fallback }
    }
}
