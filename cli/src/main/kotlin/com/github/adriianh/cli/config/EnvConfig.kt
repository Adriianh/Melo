package com.github.adriianh.cli.config

import io.github.cdimascio.dotenv.dotenv

/**
 * Single source of truth for reading environment / .env values.
 *
 * Search order (first non-null value wins):
 *   1. Real environment variables (already exported in the shell)
 *   2. .env in the current working directory  (dev workflow)
 *   3. ~/.config/melo/.env                    (Linux / macOS install)
 *   4. %APPDATA%\melo\.env                    (Windows install)
 */
fun resolveEnv(key: String): String? {
    // 1. Real env var takes priority over any .env file
    System.getenv(key)?.takeIf { it.isNotBlank() }?.let { return it }

    val locations = listOf(
        System.getProperty("user.dir"),
        "${System.getProperty("user.home")}/.config/melo",
        "${System.getenv("APPDATA") ?: ""}\\melo",
    )
    for (dir in locations) {
        val value = dotenv {
            directory = dir
            ignoreIfMissing = true
        }.get(key, null)?.takeIf { it.isNotBlank() }
        if (value != null) return value
    }
    return null
}

val configDir: String get() {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> "${System.getenv("APPDATA") ?: System.getProperty("user.home")}\\melo"
        else               -> "${System.getProperty("user.home")}/.config/melo"
    }
}

val shareDir: String get() {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> "${System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")}\\melo"
        else               -> "${System.getProperty("user.home")}/.local/share/melo"
    }
}
