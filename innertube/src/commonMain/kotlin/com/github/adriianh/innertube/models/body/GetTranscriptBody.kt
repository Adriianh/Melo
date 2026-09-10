package com.github.adriianh.innertube.models.body

import com.github.adriianh.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
