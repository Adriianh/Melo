package com.github.adriianh.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicDescriptionShelfRenderer(
    val header: Runs?,
    val subheader: Runs?,
    val description: Runs,
    val footer: Runs?,
)
