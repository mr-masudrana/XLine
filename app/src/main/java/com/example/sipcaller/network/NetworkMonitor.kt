package com.example.sipcaller.network

import android.content.Context
import android.net.*
import android.os.Build
import java.util.concurrent.CopyOnWriteArraySet

/** Observes validated internet connectivity. API 23 is supported via CONNECTIVITY_ACTION fallback. */
class NetworkMonitor(private val context: Context) {
    interface Listener { fun onNetworkChanged(connected: Boolean) }
    private val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val listeners = CopyOnWriteArraySet<Listener>()
    @Volatile private var started = false
    @Volatile private var lastState: Boolean? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = dispatchCurrent()
        override fun onLost(network: Network) = dispatchCurrent()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = dispatch(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
    }

    fun addListener(listener: Listener) { listeners.add(listener); listener.onNetworkChanged(isConnected()) }
    fun removeListener(listener: Listener) { listeners.remove(listener) }
    fun start() {
        if (started) return
        started = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) cm.registerDefaultNetworkCallback(callback)
        else @Suppress("DEPRECATION") cm.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), callback)
        dispatchCurrent()
    }
    fun stop() { if (!started) return; started = false; try { cm.unregisterNetworkCallback(callback) } catch (_: Exception) {} }
    fun refresh() = dispatchCurrent()
    private fun dispatchCurrent() = dispatch(isConnected())
    private fun dispatch(value: Boolean) { if (lastState == value) return; lastState = value; listeners.forEach { it.onNetworkChanged(value) } }
    fun isConnected(): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
