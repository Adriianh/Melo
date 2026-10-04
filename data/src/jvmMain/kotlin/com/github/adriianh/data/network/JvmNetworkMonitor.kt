package com.github.adriianh.data.network

import com.github.adriianh.core.domain.network.NetworkMonitor
import com.github.adriianh.core.util.MeloDispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class JvmNetworkMonitor(
    dispatcher: CoroutineDispatcher = MeloDispatchers.IO,
    scope: CoroutineScope? = null,
) : NetworkMonitor {
    private val monitorScope = scope ?: CoroutineScope(SupervisorJob() + dispatcher)
    private val _isOnline = MutableStateFlow(checkConnectivitySync())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var pollJob: Job? = null

    init {
        startPolling(dispatcher)
    }

    override fun checkNow(): Boolean {
        val connected = checkConnectivitySync()
        _isOnline.value = connected
        return connected
    }

    private fun startPolling(dispatcher: CoroutineDispatcher) {
        pollJob?.cancel()
        pollJob =
            monitorScope.launch(dispatcher) {
                while (isActive) {
                    val connected = checkConnectivityParallel(dispatcher)
                    _isOnline.value = connected
                    val delayMs = if (connected) 2_500L else 1_500L
                    delay(delayMs.milliseconds)
                }
            }
    }

    companion object {
        private const val PROBE_TIMEOUT_MS = 750
        private val DNS_PROBES =
            listOf(
                "1.1.1.1" to 53,
                "8.8.8.8" to 53,
            )

        internal suspend fun checkConnectivityParallel(dispatcher: CoroutineDispatcher = MeloDispatchers.IO): Boolean {
            val result = CompletableDeferred<Boolean>()
            val remainingProbes = AtomicInteger(DNS_PROBES.size)

            coroutineScope {
                for ((host, port) in DNS_PROBES) {
                    launch(dispatcher) {
                        val connected =
                            try {
                                Socket().use { socket ->
                                    socket.connect(InetSocketAddress(host, port), PROBE_TIMEOUT_MS)
                                    true
                                }
                            } catch (_: Exception) {
                                false
                            }
                        if (connected) {
                            result.complete(true)
                        } else if (remainingProbes.decrementAndGet() == 0) {
                            result.complete(false)
                        }
                    }
                }
            }
            return result.await()
        }

        fun checkConnectivity(): Boolean = checkConnectivitySync()

        private fun checkConnectivitySync(): Boolean {
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
