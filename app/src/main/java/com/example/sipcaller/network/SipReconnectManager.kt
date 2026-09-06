package com.example.sipcaller.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.sipcaller.SipManager
import com.example.sipcaller.SipPreferences

/** Debounced SIP recovery after internet returns or changes transport (Wi-Fi/mobile data). */
class SipReconnectManager(private val context: Context) : NetworkMonitor.Listener {
    private val handler = Handler(Looper.getMainLooper())
    private var networkWasAvailable = false
    private val reconnect = Runnable {
        if (!monitor.isConnected() || SipManager.isAccountRegistered()) return@Runnable
        val creds = SipPreferences(context).load() ?: return@Runnable
        SipManager.registerAccount(creds)
    }
    private val monitor = NetworkMonitor(context)

    fun start() { monitor.addListener(this); monitor.start() }
    fun stop() { handler.removeCallbacks(reconnect); monitor.removeListener(this); monitor.stop() }
    override fun onNetworkChanged(connected: Boolean) {
        if (!connected) { networkWasAvailable = false; handler.removeCallbacks(reconnect); return }
        if (!networkWasAvailable) networkWasAvailable = true
        scheduleReconnect()
    }
    fun scheduleReconnect(delayMs: Long = 2_000L) { handler.removeCallbacks(reconnect); handler.postDelayed(reconnect, delayMs) }
}
