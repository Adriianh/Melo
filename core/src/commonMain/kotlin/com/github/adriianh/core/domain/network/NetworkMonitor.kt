package com.github.adriianh.core.domain.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Monitors network connectivity reactively across platforms.
 */
interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}
