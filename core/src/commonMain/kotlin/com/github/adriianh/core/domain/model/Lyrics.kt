package com.github.adriianh.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncedLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null
)

@Serializable
data class TrackLyrics(
    val plainLyrics: String? = null,
    val syncedLyrics: List<SyncedLine> = emptyList(),
    val hasSync: Boolean = syncedLyrics.isNotEmpty(),
    val targetLanguage: String? = null,
    val isTranslating: Boolean = false,
    val error: String? = null
)