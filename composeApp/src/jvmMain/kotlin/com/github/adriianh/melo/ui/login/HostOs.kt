package com.github.adriianh.melo.ui.login

/** The host operating system, resolved once and reused across the login/auth code. */
enum class HostOs {
    WINDOWS,
    MACOS,
    LINUX,
    ;

    companion object {
        val current: HostOs by lazy {
            val name = System.getProperty("os.name").orEmpty().lowercase()
            when {
                name.contains("win") -> WINDOWS
                name.contains("mac") -> MACOS
                else -> LINUX
            }
        }
    }
}