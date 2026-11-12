package com.github.adriianh.innertube.models

data class AccountChannel(
    val name: String,
    val handle: String?,
    val byline: String? = null,
    val avatarUrl: String?,
    val pageId: String?,
    val obfuscatedGaiaId: String?,
    val isSelected: Boolean = false,
)
