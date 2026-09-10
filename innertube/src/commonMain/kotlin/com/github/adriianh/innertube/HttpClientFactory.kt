package com.github.adriianh.innertube

import io.ktor.client.HttpClient

/**
 * Creates a platform-specific [HttpClient] with the appropriate engine.
 *
 * On JVM this uses OkHttp; on iOS it will use Darwin; on Android, OkHttp.
 */
expect fun createPlatformHttpClient(proxyConfig: ProxyConfig? = null): HttpClient

/**
 * Platform-agnostic proxy configuration.
 * On JVM, this maps to [java.net.Proxy]; on other platforms, to engine-specific settings.
 */
data class ProxyConfig(
    val host: String,
    val port: Int,
    val type: ProxyType = ProxyType.HTTP,
)

enum class ProxyType { HTTP, SOCKS }
