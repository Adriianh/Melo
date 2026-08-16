package com.github.adriianh.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountProfile(
    val name: String,
    val email: String? = null,
    val channelHandle: String? = null,
    val avatarUrl: String? = null,
)