package com.github.adriianh.data.network

import com.github.adriianh.core.domain.network.NetworkMonitor
import com.github.adriianh.core.util.MeloDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.time.Duration.Companion.milliseconds

class JvmNetworkMonitor(
    dispatcher: CoroutineDispatcher = MeloDispatchers.IO,
    scope: CoroutineScope? = null,
) : NetworkMonitor {
    private val monitorScope = scope ?: CoroutineScope(SupervisorJob() + dispatcher)
    private val _isOnline = MutableStateFlow(checkConnectivity())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var pollJob: Job? = null

    init {
        startPolling(dispatcher)
    }

    fun checkNow(): Boolean {
        val connected = checkConnectivity()
        _isOnline.value = connected
        return connected
    }

    private fun startPolling(dispatcher: CoroutineDispatcher) {
        pollJob?.cancel()
        pollJob =
            monitorScope.launch(dispatcher) {
                while (isActive) {
                    val connected = checkConnectivity()
                    _isOnline.value = connected
                    val delayMs = if (connected) 10_000L else 3_000L
                    delay(delayMs.milliseconds)
                }
            }
    }

    companion object {
        private const val PROBE_TIMEOUT_MS = 1_500
        private val DNS_PROBES =
            listOf(
                "1.1.1.1" to 53,
                "8.8.8.8" to 53,
            )

        fun checkConnectivity(): Boolean {
            for ((host, port) in DNS_PROBES) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), PROBE_TIMEOUT_MS)
                        return true
                    }
                } catch (_: Exception) {
                    continue
                }
            }
            return false
        }
    }
}
