package com.github.adriianh.innertube

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

private val defaultSharedEngine: HttpClientEngine by lazy {
    Darwin.create()
}

actual fun createPlatformHttpEngine(proxyConfig: ProxyConfig?): HttpClientEngine = defaultSharedEngine

actual fun createPlatformHttpClient(proxyConfig: ProxyConfig?): HttpClient =
    HttpClient(
        createPlatformHttpEngine(proxyConfig),
    )
