package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.kmpalette.generatePalette
import com.kmpalette.loader.ByteArrayLoader
import com.kmpalette.palette.graphics.Palette
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AccentPalette(
    val dominant: Color,
    val onDominant: Color,
    val rawDominant: Color = dominant
) {
    fun harmonize(isDarkMode: Boolean): AccentPalette {
        val harmonized = ColorUtils.harmonize(rawDominant, isDarkMode)
        val onColor = if (harmonized.luminance() > 0.45f) Color(0xFF111111) else Color.White
        return AccentPalette(dominant = harmonized, onDominant = onColor, rawDominant = rawDominant)
    }
}

object AccentColorExtractor {
    private const val MAX_PALETTE_CACHE_ENTRIES = 32
    private const val ARTWORK_CACHE_URL_SIZE_PX = 180
    private const val PALETTE_DECODE_SIZE_PX = 120
    private val paletteCacheMutex = Mutex()
    private val paletteCache = LinkedHashMap<Pair<String, Boolean>, AccentPalette>()

    val fallback = AccentPalette(
        dominant = Color(0xFFFF2D55),
        onDominant = Color.White,
        rawDominant = Color(0xFFFF2D55)
    )

    /**
     * Extracts the most vibrant, readable accent color from a [Palette].
     *
     * Evaluates all palette swatches (Vibrant, Light Vibrant, Dark Vibrant, Muted, Dominant),
     * scores them based on saturation, chromatic vibrancy and population, and automatically
     * harmonizes the winner against dark background for maximum legibility.
     */
    fun fromPalette(palette: Palette, isDarkMode: Boolean = true): AccentPalette {
        val candidateSwatches = listOfNotNull(
            palette.vibrantSwatch,
            palette.lightVibrantSwatch,
            palette.darkVibrantSwatch,
            palette.mutedSwatch,
            palette.lightMutedSwatch,
            palette.darkMutedSwatch,
            palette.dominantSwatch
        )

        val bestSwatch = candidateSwatches.maxByOrNull { swatch ->
            val color = Color(swatch.rgb)
            val hsl = ColorUtils.rgbToHsl(color)
            val saturation = hsl[1]
            val lightness = hsl[2]

            var score = saturation * 100f

            if (lightness in 0.20f..0.80f) {
                score += 50f
            } else if (lightness !in 0.10f..0.90f) {
                score -= 60f
            }

            if (saturation >= 0.40f) {
                score += 40f
            } else if (saturation < 0.15f) {
                score -= 50f
            }

            val popBonus = (swatch.population.toFloat() / 1000f).coerceIn(0f, 20f)
            score + popBonus
        }

        val rawColor = bestSwatch?.rgb?.let { Color(it) } ?: fallback.rawDominant
        val harmonized = ColorUtils.harmonize(rawColor, isDarkMode)
        val onColor = if (harmonized.luminance() > 0.45f) Color(0xFF111111) else Color.White

        return AccentPalette(
            dominant = harmonized,
            onDominant = onColor,
            rawDominant = rawColor
        )
    }

    suspend fun fromImageBytes(bytes: ByteArray, isDarkMode: Boolean = true): AccentPalette {
        return runCatching {
            val bitmap = ByteArrayLoader.load(bytes)
            fromPalette(bitmap.generatePalette(), isDarkMode)
        }.getOrElse { fallback }
    }

    suspend fun fromImageUrl(
        url: String?,
        httpClient: HttpClient,
        isDarkMode: Boolean = true,
    ): AccentPalette {
        if (url.isNullOrBlank()) return fallback
        val compactUrl = normalizeArtworkUrl(url, ARTWORK_CACHE_URL_SIZE_PX) ?: url
        val cacheKey = compactUrl to isDarkMode
        val cached = paletteCacheMutex.withLock { paletteCache[cacheKey] }

        return cached ?: runCatching {
            val cachedBitmap = loadCachedArtworkBitmap(compactUrl, PALETTE_DECODE_SIZE_PX)
            if (cachedBitmap != null) {
                fromPalette(cachedBitmap.generatePalette(), isDarkMode)
            } else {
                val bytes = httpClient.get(compactUrl).body<ByteArray>()
                fromImageBytes(bytes, isDarkMode)
            }
        }.getOrElse { fallback }.also { extracted ->
            if (extracted != fallback) {
                paletteCacheMutex.withLock {
                    if (paletteCache.size >= MAX_PALETTE_CACHE_ENTRIES) {
                        paletteCache.keys.firstOrNull()?.let { paletteCache.remove(it) }
                    }
                    paletteCache[cacheKey] = extracted
                }
            }
        }
    }
}