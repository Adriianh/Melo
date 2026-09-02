package com.github.adriianh.melo.util

import androidx.compose.runtime.Composable

/**
 * Multiplatform helper to request audio/media storage permissions if required by the OS.
 */
@Composable
expect fun rememberAudioPermissionRequester(onPermissionResult: (Boolean) -> Unit): () -> Unit