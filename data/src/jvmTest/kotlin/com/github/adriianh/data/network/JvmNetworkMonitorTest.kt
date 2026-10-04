package com.github.adriianh.data.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class JvmNetworkMonitorTest {
    @Test
    fun `jvm network monitor initializes and exposes isOnline flow`() {
        val monitor = JvmNetworkMonitor()
        assertNotNull(monitor.isOnline.value)
    }

    @Test
    fun `checkNow updates isOnline and returns boolean`() {
        val monitor = JvmNetworkMonitor()
        val result = monitor.checkNow()
        assertNotNull(result)
    }

    @Test
    fun `parallel connectivity check executes without throwing`() =
        runTest {
            val result = JvmNetworkMonitor.checkConnectivityParallel()
            assertNotNull(result)
        }

    @Test
    fun `static checkConnectivity returns valid boolean`() {
        val result = JvmNetworkMonitor.checkConnectivity()
        assertNotNull(result)
    }
}
