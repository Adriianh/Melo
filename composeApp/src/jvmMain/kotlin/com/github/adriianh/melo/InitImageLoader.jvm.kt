package com.github.adriianh.melo

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import java.io.File

@Composable
actual fun InitImageLoader() {
    setSingletonImageLoaderFactory { context ->
        val cacheDir = File(System.getProperty("user.home"), ".melo/cache/images")
        cacheDir.mkdirs()
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir)
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
