package com.github.adriianh.innertube

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createPlatformHttpClient(proxyConfig: ProxyConfig?): HttpClient {
    return HttpClient(Darwin) {
        // Proxy configuration for Darwin engine can be added here if needed
        // For now, we provide a basic implementation
    }
}
