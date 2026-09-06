package com.example.sipcaller

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/** Foreground owner for SIP lifecycle; restores the saved account after process restart. */
class SipCallService : Service(), SipManager.SipCallListener {
    private val channelId = "sip_call_service_channel"
    override fun onCreate() { super.onCreate(); createChannel(); startForeground(NOTIFICATION_ID, notification("Connecting SIP…")); SipManager.addListener(this); SipManager.init(); SipManager.restoreAndRegister(this) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { SipManager.init(); SipManager.restoreAndRegister(this); return START_STICKY }
    override fun onDestroy() { SipManager.removeListener(this); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onRegistrationStateChanged(isRegistered: Boolean, statusText: String) { update(if (isRegistered) "SIP connected" else "SIP: $statusText") }
    override fun onCallStateChanged(call: SipCall, state: String) { update("Call: $state") }
    override fun onIncomingCall(call: SipCall) { showIncomingCall(call); startActivity(Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)) }
    private fun update(text: String) { val n=getSystemService(NotificationManager::class.java); n.notify(NOTIFICATION_ID, notification(text)) }
    private fun notification(text: String): Notification = NotificationCompat.Builder(this, channelId).setContentTitle("XLine").setContentText(text).setSmallIcon(android.R.drawable.sym_call_incoming).setOngoing(true).build()
    private fun showIncomingCall(call: SipCall) { val n=getSystemService(NotificationManager::class.java); n.notify(INCOMING_ID, NotificationCompat.Builder(this, channelId).setContentTitle("Incoming SIP call").setContentText(call.remoteUri.ifBlank { "Incoming call" }).setSmallIcon(android.R.drawable.sym_call_incoming).setPriority(NotificationCompat.PRIORITY_HIGH).build()) }
    private fun createChannel() { if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channelId,"SIP Connection",NotificationManager.IMPORTANCE_LOW)) }
    companion object { const val NOTIFICATION_ID=1001; const val INCOMING_ID=1002; fun start(context: Context) { androidx.core.content.ContextCompat.startForegroundService(context, Intent(context, SipCallService::class.java)) } }
}
