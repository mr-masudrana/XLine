package com.example.sipcaller.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.sipcaller.SipManager

/**
 * Lightweight watchdog for persistent SIP registration.
 * It never tears down an active engine; it only asks the existing manager to
 * restore registration when the account is unexpectedly offline.
 */
class SipRegistrationWatchdog(context: Context) {
    private val app = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            if (!SipManager.isAccountRegistered()) {
                SipManager.restoreAndRegister(app)
            }
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    fun start() {
        if (running) return
        running = true
        handler.postDelayed(tick, INITIAL_DELAY_MS)
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val INITIAL_DELAY_MS = 15_000L
        private const val INTERVAL_MS = 60_000L
    }
}
