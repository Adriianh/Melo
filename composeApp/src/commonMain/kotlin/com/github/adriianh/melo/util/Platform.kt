package com.github.adriianh.melo.util

enum class PlatformType {
    ANDROID,
    IOS,
    DESKTOP
}

interface Platform {
    val type: PlatformType
    val name: String
}

expect fun getPlatform(): Platform
