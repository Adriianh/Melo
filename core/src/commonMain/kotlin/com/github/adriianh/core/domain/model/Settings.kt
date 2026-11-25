package com.github.adriianh.core.domain.model

import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.core.util.MeloVersion
import kotlinx.serialization.Serializable

@Serializable
enum class ThemePreset(
    val displayName: String,
) {
    DEFAULT("Default"),
    CATPPUCCIN_MOCHA("Catppuccin Mocha"),
    GRUVBOX("Gruvbox"),
    NORD("Nord"),
    TOKYO_NIGHT("Tokyo Night"),
}

@Serializable
enum class MeloAction(
    val displayName: String,
) {
    PLAY_PAUSE("Play/Pause"),
    NEXT("Next Track"),
    PREVIOUS("Previous Track"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
    TOGGLE_QUEUE("Toggle Queue Panel"),
    ADD_TO_QUEUE("Add to Queue"),
    FAVORITE("Toggle Favorite"),
    REPEAT("Cycle Repeat Mode"),
    SHUFFLE("Toggle Shuffle"),
    LYRICS("Load Lyrics"),
    ADD_PLAYLIST("Add to Playlist"),
    DELETE("Delete/Remove Item"),
    CLEAR_QUEUE("Clear Queue"),
    TRACK_OPTIONS("Track Options Menu"),
    TOGGLE_SELECTION("Toggle Selection Mode"),
    TOGGLE_DETAIL("Toggle Detail Panel"),
}

@Serializable
enum class DownloadFormat(
    val displayName: String,
) {
    MP3("mp3"),
    OPUS("opus"),
    FLAC("flac"),
    AAC("aac"),
    M4A("m4a"),
    OGG("ogg"),
    WAV("wav"),
}

@Serializable
enum class DownloadQuality(
    val displayName: String,
) {
    LOW("64k"),
    MEDIUM("96k"),
    HIGH("128k"),
    VERY_HIGH("192k"),
    BEST("320k"),
}

@Serializable
data class MeloKey(
    val char: Char? = null,
    val code: String? = null,
    val ctrl: Boolean = false,
)

@Serializable
enum class AudioQuality(
    val displayName: String,
) {
    AUTO("Auto"),
    HIGH("High (160-256 kbps)"),
    MEDIUM("Standard (128 kbps)"),
    LOW("Data Saver (48-64 kbps)"),
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
enum class AppLanguage(
    val languageTag: String?,
) {
    SYSTEM(null),
    SPANISH("es"),
    ENGLISH("en"),
}

@Serializable
enum class CacheSizeLimit(
    val sizeMb: Int,
    val displayName: String,
) {
    MB_500(512, "500 MB"),
    GB_1(1024, "1 GB"),
    GB_2(2048, "2 GB"),
    GB_5(5120, "5 GB"),
    GB_10(10240, "10 GB"),
    UNLIMITED(0, "Sin límite"),
    ;

    companion object {
        fun fromSizeMb(sizeMb: Int): CacheSizeLimit = entries.find { it.sizeMb == sizeMb } ?: GB_1
    }
}

@Serializable
data class Settings(
    val theme: ThemePreset = ThemePreset.DEFAULT,
    val volume: Int = 75,
    val discordRpcEnabled: Boolean = true,
    val discordRpcToken: String? = null,
    val searchLanguage: String = "en",
    val artworkResolution: Int = 300,
    val autoDownload: Boolean = false,
    val maxOfflineSizeMb: Int = 1024,
    val maxOfflineAgeDays: Int = 30,
    val offlineMode: Boolean = false,
    val keybindings: Map<MeloAction, MeloKey> =
        mapOf(
            MeloAction.PLAY_PAUSE to MeloKey(char = ' '),
            MeloAction.NEXT to MeloKey(char = 'n'),
            MeloAction.PREVIOUS to MeloKey(char = 'p'),
            MeloAction.VOLUME_UP to MeloKey(char = '+'),
            MeloAction.VOLUME_DOWN to MeloKey(char = '-'),
            MeloAction.TOGGLE_QUEUE to MeloKey(char = 'q'),
            MeloAction.ADD_TO_QUEUE to MeloKey(char = 'q'),
            MeloAction.FAVORITE to MeloKey(char = 'f'),
            MeloAction.REPEAT to MeloKey(char = 'r'),
            MeloAction.SHUFFLE to MeloKey(char = 's'),
            MeloAction.LYRICS to MeloKey(char = 'l'),
            MeloAction.ADD_PLAYLIST to MeloKey(char = 'a'),
            MeloAction.DELETE to MeloKey(code = "DELETE"),
            MeloAction.CLEAR_QUEUE to MeloKey(char = 'c'),
            MeloAction.TRACK_OPTIONS to MeloKey(char = 'm'),
            MeloAction.TOGGLE_SELECTION to MeloKey(char = 'v'),
            MeloAction.TOGGLE_DETAIL to MeloKey(char = 'd', ctrl = true),
        ),
    val downloadFormat: DownloadFormat = DownloadFormat.OPUS,
    val downloadQuality: DownloadQuality = DownloadQuality.HIGH,
    val downloadPath: String? = null,
    val cachePath: String? = null,
    val localLibraryPaths: List<String> = emptyList(),
    val sessionCookies: String? = null,
    val accountPageId: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val dynamicColor: Boolean = true,
    val ambientMotion: Boolean = false,
    val nowPlayingBlur: Boolean = true,
    val nowPlayingBlurIntensity: NowPlayingBlurIntensity = NowPlayingBlurIntensity.MEDIUM,
    val nowPlayingBackdropScrim: NowPlayingBackdropScrim = NowPlayingBackdropScrim.NORMAL,
    val reduceTransparency: Boolean = false,
    val reduceMotion: Boolean = false,
    val lyricsBlurEnabled: Boolean = true,
    val lyricsActiveLineZoom: Boolean = true,
    val lyricsFontScale: LyricsFontScale = LyricsFontScale.NORMAL,
    val lyricsCustomFontScale: Float = 1.0f,
    val lyricsLineSpacing: LyricsLineSpacing = LyricsLineSpacing.NORMAL,
    val lyricsCustomLineSpacingDp: Int = 20,
    val lyricsActiveLineWeight: LyricsActiveLineWeight = LyricsActiveLineWeight.EXTRA_BOLD,
    val lyricsInactiveOpacity: LyricsInactiveOpacity = LyricsInactiveOpacity.NORMAL,
    val lyricsCustomInactiveOpacity: Float = 0.55f,
    val lyricsAlignment: LyricsAlignment = LyricsAlignment.START,
    val lyricsActiveLineColor: LyricsActiveLineColor = LyricsActiveLineColor.WHITE,
    val lyricsAnchorPosition: LyricsAnchorPosition = LyricsAnchorPosition.UPPER,
    val lyricsSyncOffsetMs: Int = 0,
    val lyricsShowSourceBadge: Boolean = true,
    val lyricsShowInstrumentalBreaks: Boolean = true,
    val lyricsShowCompactPreviewLine: Boolean = true,
    val lyricsAutoResumeDelay: LyricsAutoResumeDelay = LyricsAutoResumeDelay.SECONDS_3,
    val lyricsCustomAutoResumeSeconds: Int = 4,
    val lyricsKeepSyncButtonOnIdle: Boolean = true,
    val lyricsAutoTranslate: Boolean = false,
    val lyricsDefaultTargetLanguage: String = "es",
    val artworkCornerStyle: ArtworkCornerStyle = ArtworkCornerStyle.ROUNDED,
    val artworkCustomCornerRadiusDp: Int = 20,
    val nowPlayingInitialPanel: NowPlayingInitialPanel = NowPlayingInitialPanel.AUTO,
    val playerShowRemainingTime: Boolean = false,
    val fullscreenIdleHideDelay: FullscreenIdleHideDelay = FullscreenIdleHideDelay.SECONDS_3,
    val fullscreenCustomIdleHideSeconds: Int = 4,
    val nowPlayingKeepScreenOn: Boolean = false,
    val autoplay: Boolean = true,
    val resumeLastActiveSession: Boolean = false,
    val stopPlaybackOnTaskRemoval: Boolean = false,
    val dataSaver: Boolean = false,
    val audioQuality: AudioQuality = AudioQuality.AUTO,
    val preferredAudioOutputId: String? = null,
    val syncHistoryToYouTube: Boolean = true,
    val syncLikesToYouTube: Boolean = true,
    val autoCheckUpdates: Boolean = true,
    val updateChannel: UpdateChannel =
        if (MeloVersion.CURRENT.contains("nightly", ignoreCase = true)) {
            UpdateChannel.NIGHTLY
        } else {
            UpdateChannel.STABLE
        },
    val lastAccentColor: Long? = null,
    val lastSeenVersion: String? = null,
) {
    fun resolvedLyricsFontScale(): Float =
        when (lyricsFontScale) {
            LyricsFontScale.CUSTOM -> lyricsCustomFontScale.coerceIn(0.75f, 1.40f)
            else -> lyricsFontScale.scaleFactor
        }

    fun resolvedLyricsLineSpacingDp(): Int =
        when (lyricsLineSpacing) {
            LyricsLineSpacing.CUSTOM -> lyricsCustomLineSpacingDp.coerceIn(8, 40)
            else -> lyricsLineSpacing.spacingDp
        }

    fun resolvedLyricsInactiveOpacity(): Float =
        when (lyricsInactiveOpacity) {
            LyricsInactiveOpacity.CUSTOM -> lyricsCustomInactiveOpacity.coerceIn(0.20f, 0.85f)
            else -> lyricsInactiveOpacity.baseAlpha
        }

    fun resolvedArtworkCornerRadiusDp(): Int =
        when (artworkCornerStyle) {
            ArtworkCornerStyle.CUSTOM -> artworkCustomCornerRadiusDp.coerceIn(0, 36)
            else -> artworkCornerStyle.radiusDp
        }

    fun resolvedFullscreenIdleHideDelayMs(): Long? =
        when (fullscreenIdleHideDelay) {
            FullscreenIdleHideDelay.OFF -> null
            FullscreenIdleHideDelay.CUSTOM -> fullscreenCustomIdleHideSeconds.coerceIn(1, 30) * 1_000L
            else -> fullscreenIdleHideDelay.delayMs
        }

    fun resolvedLyricsAutoResumeDelayMs(): Long? =
        when (lyricsAutoResumeDelay) {
            LyricsAutoResumeDelay.OFF -> null
            LyricsAutoResumeDelay.CUSTOM -> lyricsCustomAutoResumeSeconds.coerceIn(1, 30) * 1_000L
            else -> lyricsAutoResumeDelay.delayMs
        }

    fun resetNowPlayingDefaults(): Settings {
        val defaults = Settings()
        return copy(
            nowPlayingBlur = defaults.nowPlayingBlur,
            nowPlayingBlurIntensity = defaults.nowPlayingBlurIntensity,
            nowPlayingBackdropScrim = defaults.nowPlayingBackdropScrim,
            artworkCornerStyle = defaults.artworkCornerStyle,
            artworkCustomCornerRadiusDp = defaults.artworkCustomCornerRadiusDp,
            nowPlayingInitialPanel = defaults.nowPlayingInitialPanel,
            playerShowRemainingTime = defaults.playerShowRemainingTime,
            fullscreenIdleHideDelay = defaults.fullscreenIdleHideDelay,
            fullscreenCustomIdleHideSeconds = defaults.fullscreenCustomIdleHideSeconds,
            nowPlayingKeepScreenOn = defaults.nowPlayingKeepScreenOn,
            lyricsBlurEnabled = defaults.lyricsBlurEnabled,
            lyricsActiveLineZoom = defaults.lyricsActiveLineZoom,
            lyricsFontScale = defaults.lyricsFontScale,
            lyricsCustomFontScale = defaults.lyricsCustomFontScale,
            lyricsLineSpacing = defaults.lyricsLineSpacing,
            lyricsCustomLineSpacingDp = defaults.lyricsCustomLineSpacingDp,
            lyricsActiveLineWeight = defaults.lyricsActiveLineWeight,
            lyricsInactiveOpacity = defaults.lyricsInactiveOpacity,
            lyricsCustomInactiveOpacity = defaults.lyricsCustomInactiveOpacity,
            lyricsAlignment = defaults.lyricsAlignment,
            lyricsActiveLineColor = defaults.lyricsActiveLineColor,
            lyricsAnchorPosition = defaults.lyricsAnchorPosition,
            lyricsSyncOffsetMs = defaults.lyricsSyncOffsetMs,
            lyricsShowSourceBadge = defaults.lyricsShowSourceBadge,
            lyricsShowInstrumentalBreaks = defaults.lyricsShowInstrumentalBreaks,
            lyricsShowCompactPreviewLine = defaults.lyricsShowCompactPreviewLine,
            lyricsAutoResumeDelay = defaults.lyricsAutoResumeDelay,
            lyricsCustomAutoResumeSeconds = defaults.lyricsCustomAutoResumeSeconds,
            lyricsKeepSyncButtonOnIdle = defaults.lyricsKeepSyncButtonOnIdle,
            lyricsAutoTranslate = defaults.lyricsAutoTranslate,
            lyricsDefaultTargetLanguage = defaults.lyricsDefaultTargetLanguage,
        )
    }
}

@Serializable
enum class NowPlayingBlurIntensity(
    val displayName: String,
    val sigmaPx: Float,
) {
    OFF("Desactivado", 0f),
    SOFT("Suave", 4.5f),
    MEDIUM("Medio", 7.2f),
    INTENSE("Intenso", 13.5f),
}

@Serializable
enum class NowPlayingBackdropScrim(
    val displayName: String,
    val topAlpha: Float,
    val upperMidAlpha: Float,
    val lowerMidAlpha: Float,
    val bottomAlpha: Float,
) {
    LIGHT("Ligero", 0.18f, 0.08f, 0.28f, 0.55f),
    NORMAL("Normal", 0.42f, 0.26f, 0.60f, 0.86f),
    DARK("Oscuro", 0.68f, 0.55f, 0.84f, 0.98f),
}

@Serializable
enum class LyricsFontScale(
    val scaleFactor: Float,
) {
    COMPACT(0.85f),
    NORMAL(1.0f),
    LARGE(1.18f),
    CUSTOM(1.0f),
}

@Serializable
enum class LyricsLineSpacing(
    val spacingDp: Int,
) {
    COMPACT(12),
    NORMAL(20),
    SPACIOUS(30),
    CUSTOM(20),
}

@Serializable
enum class LyricsActiveLineWeight {
    NORMAL,
    SEMIBOLD,
    BOLD,
    EXTRA_BOLD,
}

@Serializable
enum class LyricsInactiveOpacity(
    val baseAlpha: Float,
) {
    SUBTLE(0.32f),
    NORMAL(0.55f),
    HIGH(0.75f),
    CUSTOM(0.55f),
}

@Serializable
enum class LyricsAlignment {
    START,
    CENTER,
}

@Serializable
enum class LyricsActiveLineColor {
    WHITE,
    ACCENT,
}

@Serializable
enum class LyricsAnchorPosition(
    val viewportFraction: Float,
) {
    UPPER(0.35f),
    CENTER(0.50f),
}

@Serializable
enum class ArtworkCornerStyle(
    val radiusDp: Int,
) {
    SQUARE(4),
    ROUNDED(20),
    PILL(32),
    CUSTOM(20),
}

@Serializable
enum class NowPlayingInitialPanel {
    AUTO,
    ARTWORK,
    LYRICS,
}

@Serializable
enum class FullscreenIdleHideDelay(
    val delayMs: Long?,
) {
    OFF(null),
    SECONDS_3(3_000L),
    SECONDS_5(5_000L),
    SECONDS_10(10_000L),
    CUSTOM(4_000L),
}

@Serializable
enum class LyricsAutoResumeDelay(
    val delayMs: Long?,
) {
    OFF(null),
    SECONDS_3(3_000L),
    SECONDS_5(5_000L),
    SECONDS_10(10_000L),
    CUSTOM(4_000L),
}
