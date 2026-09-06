package com.example.sipcaller.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.sipcaller.SipManager
import com.example.sipcaller.SipPreferences
import com.example.sipcaller.data.repository.AppSettingsRepository
import com.example.sipcaller.diagnostics.SipDiagnostics

/** Phase B1.5 guarded registration watchdog; PJSIP remains authoritative for refresh. */
class SipRegistrationRefreshManager(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val settings get() = AppSettingsRepository(appContext)
    @Volatile private var running = false

    private val watchdog = object : Runnable {
        override fun run() {
            if (!running) return
            try {
                if (settings.keepAliveEnabled && !SipManager.isAccountRegistered()) {
                    SipPreferences(appContext).load()?.let {
                        SipDiagnostics.info(TAG, "Registration watchdog requesting recovery")
                        SipManager.registerAccount(it)
                    }
                }
            } finally {
                if (running) handler.postDelayed(this, interval())
            }
        }
    }

    fun start() { if (!running) { running = true; handler.postDelayed(watchdog, interval()) } }
    fun stop() { running = false; handler.removeCallbacksAndMessages(null) }

    fun onRegistrationStateChanged(registered: Boolean) {
        if (!running) return
        handler.removeCallbacks(watchdog)
        handler.postDelayed(watchdog, if (registered) interval() else GRACE_MS)
    }

    private fun interval() = (settings.keepAliveSeconds.coerceIn(10,300) * 2000L).coerceIn(20_000L,120_000L)

    companion object { private const val TAG="SipRegRefresh"; private const val GRACE_MS=5_000L }
}
