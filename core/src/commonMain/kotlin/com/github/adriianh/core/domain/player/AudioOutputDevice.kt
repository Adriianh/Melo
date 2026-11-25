package com.github.adriianh.core.domain.player

enum class AudioOutputKind {
    SPEAKER,
    HEADPHONES,
    BLUETOOTH,
    USB,
    HDMI,
    OTHER,
}

data class AudioOutputDevice(
    val id: String,
    val name: String,
    val kind: AudioOutputKind = AudioOutputKind.OTHER,
    val isDefault: Boolean = false,
)
