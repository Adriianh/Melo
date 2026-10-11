package com.github.adriianh.data.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        registerNetworkCallback()
        registerBroadcastReceiver()
    }

    override fun checkNow(): Boolean {
        val connected = checkCurrentConnectivity()
        _isOnline.value = connected
        return connected
    }

    private fun checkCurrentConnectivity(): Boolean {
        val cm = connectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if (hasInternet) {
            validNetworks.add(activeNetwork)
        } else {
            validNetworks.remove(activeNetwork)
        }

        return hasInternet
    }

    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return

        try {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        val capabilities = cm.getNetworkCapabilities(network)
                        val hasInternet =
                            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

                        if (hasInternet) {
                            validNetworks.add(network)
                            _isOnline.value = true
                        }
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        val hasInternet =
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

                        if (hasInternet) {
                            validNetworks.add(network)
                        } else {
                            validNetworks.remove(network)
                        }
                        _isOnline.value = validNetworks.isNotEmpty() || checkCurrentConnectivity()
                    }

                    override fun onLost(network: Network) {
                        validNetworks.remove(network)
                        _isOnline.value = validNetworks.isNotEmpty() || checkCurrentConnectivity()
                    }
                }

            cm.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
        }
    }

    private fun registerBroadcastReceiver() {
        val ctx = appContext ?: return
        try {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?,
                    ) {
                        checkNow()
                    }
                }
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            ctx.registerReceiver(receiver, filter)
        } catch (_: Exception) {
        }
    }
}
