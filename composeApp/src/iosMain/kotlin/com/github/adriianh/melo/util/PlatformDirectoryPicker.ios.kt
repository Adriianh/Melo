package com.github.adriianh.melo.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberDirectoryPicker(onDirectorySelected: (String) -> Unit): () -> Unit {
    return {}
}