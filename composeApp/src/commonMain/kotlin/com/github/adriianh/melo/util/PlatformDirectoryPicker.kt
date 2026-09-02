package com.github.adriianh.melo.util

import androidx.compose.runtime.Composable

/**
 * Multiplatform directory picker launcher.
 * Returns a lambda that opens a native folder selection dialog and calls [onDirectorySelected] with the path.
 */
@Composable
expect fun rememberDirectoryPicker(onDirectorySelected: (String) -> Unit): () -> Unit