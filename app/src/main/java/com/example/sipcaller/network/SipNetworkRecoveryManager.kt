package com.example.sipcaller.network

import android.content.Context
import android.net.*
import android.os.Handler
import android.os.Looper
import com.example.sipcaller.SipManager

/**
 * Process-wide network recovery layer. Debounces Wi-Fi/mobile transitions and
 * restores registration after a stable network becomes available.
 */
class SipNetworkRecoveryManager(context: Context) {
    private val app = context.applicationContext
    private val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val handler = Handler(Looper.getMainLooper())
    private var started = false
    private var pending = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = scheduleRecovery()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) scheduleRecovery()
        }
        override fun onLost(network: Network) { pending = false }
    }

    fun start() {
        if (started) return
        started = true
        runCatching {
            cm.registerNetworkCallback(
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                callback
            )
        }
    }

    fun stop() {
        if (!started) return
        started = false
        pending = false
        handler.removeCallbacksAndMessages(null)
        runCatching { cm.unregisterNetworkCallback(callback) }
    }

    private fun scheduleRecovery() {
        if (pending || SipManager.isAccountRegistered()) return
        pending = true
        handler.postDelayed({
            pending = false
            if (hasInternet() && !SipManager.isAccountRegistered()) {
                SipManager.restoreAndRegister(app)
            }
        }, 2500L)
    }

    private fun hasInternet(): Boolean {
        val n = cm.activeNetwork ?: return false
        val c = cm.getNetworkCapabilities(n) ?: return false
        return c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
