package com.github.adriianh.data.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.github.adriianh.core.domain.network.NetworkMonitor
import com.github.adriianh.data.local.ContextHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

@SuppressLint("MissingPermission")
class AndroidNetworkMonitor(
    context: Context? = null,
) : NetworkMonitor {
    private val appContext = context?.applicationContext ?: ContextHolder.context
    private val connectivityManager =
        appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val validNetworks: MutableSet<Network> = Collections.synchronizedSet(mutableSetOf())

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
        val isValidated =
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        if (isValidated) {
            validNetworks.add(activeNetwork)
        } else {
            validNetworks.remove(activeNetwork)
        }

        return isValidated
    }

    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return

        try {
            val request =
                NetworkRequest
                    .Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        val capabilities = cm.getNetworkCapabilities(network)

                        if (capabilities != null &&
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        ) {
                            validNetworks.add(network)
                            _isOnline.value = true
                        }
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        val hasInternet =
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                        if (hasInternet) {
                            validNetworks.add(network)
                        } else {
                            validNetworks.remove(network)
                        }
                        _isOnline.value = hasInternet
                    }

                    override fun onLost(network: Network) {
                        validNetworks.remove(network)
                        _isOnline.value = false
                    }
                }
            cm.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
        }
    }
}
