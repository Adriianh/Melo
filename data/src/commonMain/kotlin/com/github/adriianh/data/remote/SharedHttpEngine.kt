package com.github.adriianh.data.remote

import com.github.adriianh.innertube.createPlatformHttpEngine
import io.ktor.client.engine.HttpClientEngine

/**
 * Returns the shared platform [HttpClientEngine] used across `:data`, `:innertube`, and `:composeApp`.
 */
fun createSharedHttpEngine(): HttpClientEngine = createPlatformHttpEngine()
