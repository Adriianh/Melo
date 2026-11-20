package com.github.adriianh.innertube

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

internal val sharedOkHttpClient: OkHttpClient by lazy {
    val dispatcher =
        Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 8
        }
    OkHttpClient
        .Builder()
        .dispatcher(dispatcher)
        .connectionPool(ConnectionPool(5, 2, TimeUnit.MINUTES))
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()
}

private val defaultSharedEngine: HttpClientEngine by lazy {
    OkHttp.create {
        preconfigured = sharedOkHttpClient
    }
}

actual fun createPlatformHttpEngine(proxyConfig: ProxyConfig?): HttpClientEngine =
    if (proxyConfig == null) {
        defaultSharedEngine
    } else {
        OkHttp.create {
            preconfigured =
                sharedOkHttpClient
                    .newBuilder()
                    .proxy(
                        Proxy(
                            when (proxyConfig.type) {
                                ProxyType.HTTP -> Proxy.Type.HTTP
                                ProxyType.SOCKS -> Proxy.Type.SOCKS
                            },
                            InetSocketAddress(proxyConfig.host, proxyConfig.port),
                        ),
                    ).build()
        }
    }

actual fun createPlatformHttpClient(proxyConfig: ProxyConfig?): HttpClient =
    HttpClient(
        createPlatformHttpEngine(proxyConfig),
    )
