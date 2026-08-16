package com.github.adriianh.melo

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory

@Composable
actual fun InitImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context).build()
    }
}