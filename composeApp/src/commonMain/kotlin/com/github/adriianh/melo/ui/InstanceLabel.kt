package com.github.adriianh.melo.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Key of the running instance, provided by the platform entry point.
 *
 * The desktop app uses it to label the window and sidebar so concurrent Melo
 * processes can be told apart even when the window manager hides the title.
 */
val LocalInstanceLabel = staticCompositionLocalOf<String?> { null }
