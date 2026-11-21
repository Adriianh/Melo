package com.github.adriianh.melo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.nav_explore
import com.github.adriianh.melo.resources.nav_home
import com.github.adriianh.melo.resources.nav_library
import org.jetbrains.compose.resources.StringResource

/**
 * High-level navigation tabs in Melo.
 */
enum class MainTab(
    val routeName: String,
    val titleRes: StringResource,
    val icon: ImageVector,
) {
    HOME("Home", Res.string.nav_home, Icons.Default.Home),
    SEARCH("Search", Res.string.nav_explore, Icons.Default.Explore),
    LIBRARY("Library", Res.string.nav_library, Icons.Default.LibraryMusic),
    ;

    companion object {
        fun fromRouteName(name: String): MainTab {
            return entries.firstOrNull { it.routeName.equals(name, ignoreCase = true) } ?: HOME
        }
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
        val author: String = "",
    ) : ScreenRoute

    data class Playlist(
        val id: String,
        val title: String = "",
        val artwork: String? = null,
        val author: String = "",
    ) : ScreenRoute

    data class Artist(
        val id: String,
        val name: String = "",
        val artwork: String? = null,
    ) : ScreenRoute
}
