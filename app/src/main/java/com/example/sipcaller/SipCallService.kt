package com.example.sipcaller

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder

/** Foreground owner for SIP lifecycle and persistent registration. */
class SipCallService : Service(), SipManager.SipCallListener {
    private lateinit var reconnectManager: com.example.sipcaller.network.SipReconnectManager
    private lateinit var registrationRefreshManager: com.example.sipcaller.network.SipRegistrationRefreshManager
    private lateinit var notifications: SipServiceNotificationManager
    private var registered = false
    private var ensuringSip = false
    private var lastFailureText: String? = null
    private lateinit var networkRecoveryManager: com.example.sipcaller.network.SipNetworkRecoveryManager
    private lateinit var registrationWatchdog: com.example.sipcaller.network.SipRegistrationWatchdog

    override fun onCreate() {
        super.onCreate()
        notifications = SipServiceNotificationManager(this)
        notifications.createChannels()
        runCatching { startForeground(NOTIFICATION_ID, notifications.service("Connecting to SIP…", false)) }
            .onFailure { android.util.Log.e("SipCallService", "Foreground start failed", it) }
        SipManager.addListener(this)
        reconnectManager = com.example.sipcaller.network.SipReconnectManager(this)
        reconnectManager.start()
        networkRecoveryManager = com.example.sipcaller.network.SipNetworkRecoveryManager(this)
        networkRecoveryManager.start()
        registrationWatchdog = com.example.sipcaller.network.SipRegistrationWatchdog(this)
        registrationWatchdog.start()
        ensureSip()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            else -> {
                ensureSip()
                if (::registrationWatchdog.isInitialized) registrationWatchdog.start()
            }
        }
        return START_STICKY
    }

    @Synchronized
    private fun ensureSip() {
        if (ensuringSip) return
        ensuringSip = true
        try {
            if (!SipManager.init()) {
                update("SIP engine failed to start", false)
                if (lastFailureText != "engine") {
                    notifications.alert("SIP unavailable", "The SIP engine could not start. Open the app to check your account.")
                    lastFailureText = "engine"
                }
                return
            }
            if (!SipManager.isAccountRegistered()) SipManager.restoreAndRegister(this)
        } finally {
            ensuringSip = false
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep foreground SIP ownership alive when the UI task is removed.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (::registrationRefreshManager.isInitialized) registrationRefreshManager.stop()
        if (::reconnectManager.isInitialized) reconnectManager.stop()
        if (::networkRecoveryManager.isInitialized) networkRecoveryManager.stop()
        if (::registrationWatchdog.isInitialized) registrationWatchdog.stop()
        SipManager.removeListener(this)
        // The foreground service owns the persistent SIP lifecycle. Release native
        // resources when the service is explicitly stopped to avoid orphaned engine state.
        runCatching { SipManager.shutdown() }
        runCatching { stopForeground(true) }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onRegistrationStateChanged(isRegistered: Boolean, statusText: String) {
        registered = isRegistered
        update(if (isRegistered) "SIP connected" else "SIP: $statusText", isRegistered)
        if (!isRegistered && statusText.isNotBlank() && lastFailureText != statusText) {
            notifications.alert("SIP registration failed", statusText)
            lastFailureText = statusText
        }
        if (isRegistered) lastFailureText = null
        if (::reconnectManager.isInitialized) {
            reconnectManager.onRegistrationStateChanged(isRegistered)
        }
    }
    override fun onCallStateChanged(call: SipCall, state: String) { update("Call: $state", registered) }
    override fun onIncomingCall(call: SipCall) {
        SipIncomingCallController.incoming(this, call)
    }

    private fun update(text: String, isRegistered: Boolean = registered) {
        notifications.update(text, isRegistered)
    }
    companion object {
        const val NOTIFICATION_ID = 1001
        const val INCOMING_ID = 1002
        const val ACTION_ENSURE = "com.example.sipcaller.action.ENSURE"
        const val ACTION_STOP = "com.example.sipcaller.action.STOP"
        fun start(context: Context) = SipServiceController.start(context)
    }
}
