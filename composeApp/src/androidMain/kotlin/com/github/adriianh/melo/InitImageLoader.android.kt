package com.github.adriianh.melo

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.size.Precision
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import org.koin.compose.koinInject

@OptIn(ExperimentalCoilApi::class)
@Composable
actual fun InitImageLoader() {
    val httpClient: HttpClient = koinInject()
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = httpClient))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("melo_image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .precision(Precision.INEXACT)
            .coroutineContext(Dispatchers.IO.limitedParallelism(4))
            .build()
    }
}