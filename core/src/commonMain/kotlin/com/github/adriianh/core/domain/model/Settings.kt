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
enum class MeloAction(val displayName: String) {
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
    CLEAR_QUEUE("Clear Queue")
}

@Serializable
enum class DownloadFormat(val displayName: String) {
    MP3("mp3"),
    OPUS("opus"),
    FLAC("flac"),
    AAC("aac"),
    M4A("m4a"),
    OGG("ogg"),
    WAV("wav")
}

@Serializable
enum class DownloadQuality(val displayName: String) {
    LOW("64k"),
    MEDIUM("96k"),
    HIGH("128k"),
    VERY_HIGH("192k"),
    BEST("320k")
}


@Serializable
data class MeloKey(
    val char: Char? = null,
    val code: String? = null
)

@Serializable
enum class AudioQuality(val displayName: String) {
    AUTO("Auto"),
    HIGH("High (160-256 kbps)"),
    MEDIUM("Standard (128 kbps)"),
    LOW("Data Saver (48-64 kbps)")
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
enum class CacheSizeLimit(val sizeMb: Int, val displayName: String) {
    MB_500(512, "500 MB"),
    GB_1(1024, "1 GB"),
    GB_2(2048, "2 GB"),
    GB_5(5120, "5 GB"),
    GB_10(10240, "10 GB"),
    UNLIMITED(0, "Sin límite");

    companion object {
        fun fromSizeMb(sizeMb: Int): CacheSizeLimit {
            return entries.find { it.sizeMb == sizeMb } ?: GB_1
        }
    }
}

@Serializable
data class Settings(
    val theme: ThemePreset = ThemePreset.DEFAULT,
    val volume: Int = 75,
    val discordRpcEnabled: Boolean = true,
    val searchLanguage: String = "en",
    val artworkResolution: Int = 300,
    val autoDownload: Boolean = false,
    val maxOfflineSizeMb: Int = 1024,
    val maxOfflineAgeDays: Int = 30,
    val offlineMode: Boolean = false,
    val keybindings: Map<MeloAction, MeloKey> = mapOf(
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
        MeloAction.CLEAR_QUEUE to MeloKey(char = 'c')
    ),
    val downloadFormat: DownloadFormat = DownloadFormat.OPUS,
    val downloadQuality: DownloadQuality = DownloadQuality.HIGH,
    val downloadPath: String? = null,
    val cachePath: String? = null,
    val localLibraryPaths: List<String> = emptyList(),
    val sessionCookies: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val autoplay: Boolean = true,
    val dataSaver: Boolean = false,
    val audioQuality: AudioQuality = AudioQuality.AUTO,
    val syncHistoryToYouTube: Boolean = true,
)