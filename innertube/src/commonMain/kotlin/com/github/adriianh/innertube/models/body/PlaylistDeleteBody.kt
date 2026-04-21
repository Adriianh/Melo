package com.github.adriianh.innertube.models.body

import com.github.adriianh.innertube.models.Context
import kotlinx.serialization.Serializable
@Serializable
data class PlaylistDeleteBody(
    val context: Context,
    val playlistId: String
)