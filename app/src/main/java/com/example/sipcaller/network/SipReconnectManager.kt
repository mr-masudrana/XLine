package com.example.sipcaller.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.sipcaller.SipManager
import com.example.sipcaller.SipPreferences
import com.example.sipcaller.data.repository.AppSettingsRepository
import com.example.sipcaller.diagnostics.SipRegistrationMonitor
import com.example.sipcaller.diagnostics.SipDiagnosticsStore

/** Phase B1.4: resilient SIP recovery with bounded exponential backoff. */
class SipReconnectManager(private val context: Context) : NetworkMonitor.Listener {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val monitor = NetworkMonitor(appContext)
    private var retryAttempt = 0
        SipDiagnosticsStore.update { copy(retryCount = 0) }
    private var lastConnected = false

    private val reconnect = Runnable {
        if (!AppSettingsRepository(appContext).autoReconnect || !monitor.isConnected()) return@Runnable
        if (SipManager.isAccountRegistered()) { reset(); return@Runnable }
        val credentials = SipPreferences(appContext).load() ?: return@Runnable
        SipRegistrationMonitor.reconnecting("Network recovery attempt ${retryAttempt + 1}")
        retryAttempt++
        SipDiagnosticsStore.update { copy(retryCount = retryAttempt, networkState = "Connected / recovering") }
        SipManager.registerAccount(credentials)
    }

    fun start() { monitor.addListener(this); monitor.start() }
    fun stop() { handler.removeCallbacks(reconnect); monitor.removeListener(this); monitor.stop(); reset() }

    override fun onNetworkChanged(connected: Boolean) {
        SipRegistrationMonitor.networkChanged(connected)
        SipDiagnosticsStore.update { copy(networkState = if (connected) "Connected" else "Offline") }
        if (!connected) {
            lastConnected = false
            handler.removeCallbacks(reconnect)
            if (SipManager.isAccountRegistered()) SipRegistrationMonitor.disconnected("Network unavailable")
            return
        }
        val restored = !lastConnected
        lastConnected = true
        scheduleReconnect(if (restored) NETWORK_SETTLE_DELAY_MS else TRANSPORT_CHANGE_DELAY_MS)
    }

    fun onRegistrationStateChanged(isRegistered: Boolean) {
        if (isRegistered) { handler.removeCallbacks(reconnect); reset() }
        else if (monitor.isConnected()) scheduleReconnect()
    }

    fun scheduleReconnect(delayMs: Long = nextDelay()) {
        if (!AppSettingsRepository(appContext).autoReconnect) return
        handler.removeCallbacks(reconnect)
        if (SipManager.isAccountRegistered()) { reset(); return }
        handler.postDelayed(reconnect, delayMs.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS))
    }

    private fun nextDelay(): Long = (BASE_DELAY_MS shl retryAttempt.coerceAtMost(MAX_BACKOFF_EXPONENT)).coerceAtMost(MAX_DELAY_MS)
    private fun reset() { retryAttempt = 0
        SipDiagnosticsStore.update { copy(retryCount = 0) }; lastConnected = monitor.isConnected() }

    companion object {
        private const val BASE_DELAY_MS = 2_000L
        private const val MIN_DELAY_MS = 500L
        private const val MAX_DELAY_MS = 60_000L
        private const val MAX_BACKOFF_EXPONENT = 5
        private const val NETWORK_SETTLE_DELAY_MS = 2_000L
        private const val TRANSPORT_CHANGE_DELAY_MS = 1_000L
    }
}
