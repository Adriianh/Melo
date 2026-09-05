package com.github.adriianh.melo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * High-level navigation tabs in Melo.
 */
enum class MainTab(val routeName: String, val title: String, val icon: ImageVector) {
    HOME("Home", "Inicio", Icons.Default.Home),
    SEARCH("Search", "Explorar", Icons.Default.Explore),
    LIBRARY("Library", "Biblioteca", Icons.Default.LibraryMusic);

    companion object {
        fun fromRouteName(name: String): MainTab =
            entries.firstOrNull { it.routeName.equals(name, ignoreCase = true) } ?: HOME
    }
}

/**
 * Immutable type-safe screen destinations.
 */
sealed interface ScreenRoute {
    data object Home : ScreenRoute
    data object Search : ScreenRoute
    data object Library : ScreenRoute

    data class Album(
        val id: String,
        val title: String = "",
        val artwork: String? = null,
        val author: String = ""
    ) : ScreenRoute

    data class Playlist(
        val id: String,
        val title: String = "",
        val artwork: String? = null,
        val author: String = ""
    ) : ScreenRoute

    data class Artist(
        val id: String,
        val name: String = "",
        val artwork: String? = null
    ) : ScreenRoute
}