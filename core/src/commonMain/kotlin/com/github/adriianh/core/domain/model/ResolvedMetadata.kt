package com.github.adriianh.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ResolvedMetadata(
    val album: String?,
    val artworkUrl: String?
)
