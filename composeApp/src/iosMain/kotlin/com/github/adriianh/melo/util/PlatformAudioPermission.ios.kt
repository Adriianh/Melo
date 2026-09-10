package com.github.adriianh.melo.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberAudioPermissionRequester(onPermissionResult: (Boolean) -> Unit): () -> Unit {
    return { onPermissionResult(true) }
}