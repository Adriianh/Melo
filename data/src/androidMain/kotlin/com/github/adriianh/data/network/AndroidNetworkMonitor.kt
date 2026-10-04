package com.github.adriianh.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.github.adriianh.core.domain.network.NetworkMonitor
import com.github.adriianh.data.local.ContextHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class AndroidNetworkMonitor(
    context: Context? = null,
) : NetworkMonitor {
    private val appContext = context?.applicationContext ?: ContextHolder.context
    private val connectivityManager =
        appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        registerNetworkCallback()
    }

    override fun checkNow(): Boolean {
        val connected = checkInitialConnectivity()
        _isOnline.value = connected
        return connected
    }

    private fun checkInitialConnectivity(): Boolean {
        val cm = connectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return
        try {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnline.value = checkInitialConnectivity()
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = false
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        val hasInternet =
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        _isOnline.value = hasInternet
                    }
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(callback)
            } else {
                val request =
                    NetworkRequest
                        .Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                cm.registerNetworkCallback(request, callback)
            }
        } catch (_: Exception) {
            // Fallback in case of restricted permissions or test environments
        }
    }
}
