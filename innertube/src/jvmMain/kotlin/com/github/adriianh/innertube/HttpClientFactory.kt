package com.github.adriianh.innertube

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.net.InetSocketAddress
import java.net.Proxy

actual fun createPlatformHttpClient(proxyConfig: ProxyConfig?): HttpClient {
    return HttpClient(OkHttp) {
        if (proxyConfig != null) {
            engine {
                proxy = Proxy(
                    when (proxyConfig.type) {
                        ProxyType.HTTP -> Proxy.Type.HTTP
                        ProxyType.SOCKS -> Proxy.Type.SOCKS
                    },
                    InetSocketAddress(proxyConfig.host, proxyConfig.port)
                )
            }
        }
    }
}
