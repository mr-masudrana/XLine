package com.example.sipcaller

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder

/** Foreground owner for SIP lifecycle and persistent registration. */
class SipCallService : Service(), SipManager.SipCallListener {
    private lateinit var reconnectManager: com.example.sipcaller.network.SipReconnectManager
    private lateinit var notifications: SipServiceNotificationManager
    private var registered = false

    override fun onCreate() {
        super.onCreate()
        notifications = SipServiceNotificationManager(this)
        notifications.createChannels()
        startForeground(NOTIFICATION_ID, notifications.service("Connecting to SIP…", false))
        SipManager.addListener(this)
        reconnectManager = com.example.sipcaller.network.SipReconnectManager(this)
        reconnectManager.start()
        ensureSip()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            else -> ensureSip()
        }
        return START_STICKY
    }

    private fun ensureSip() {
        if (!SipManager.init()) {
            update("SIP engine failed to start", false)
            notifications.alert("SIP unavailable", "The SIP engine could not start. Open the app to check your account.")
            return
        }
        if (!SipManager.isAccountRegistered()) SipManager.restoreAndRegister(this)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // START_STICKY requests restoration if the app task is swiped away.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        if (::reconnectManager.isInitialized) reconnectManager.stop()
        SipManager.removeListener(this)
        // The foreground service owns the persistent SIP lifecycle. Release native
        // resources when the service is explicitly stopped to avoid orphaned engine state.
        SipManager.shutdown()
        stopForeground(true)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onRegistrationStateChanged(isRegistered: Boolean, statusText: String) {
        registered = isRegistered
        update(if (isRegistered) "SIP connected" else "SIP: $statusText", isRegistered)
        if (!isRegistered && statusText.isNotBlank()) notifications.alert("SIP registration failed", statusText)
        if (!isRegistered && ::reconnectManager.isInitialized &&
            com.example.sipcaller.data.repository.AppSettingsRepository(this).autoReconnect) {
            reconnectManager.scheduleReconnect(3_000L)
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
