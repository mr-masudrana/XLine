package com.example.sipcaller

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** Centralized incoming-call presentation: wake lock, ringtone/vibration and full-screen notification. */
class SipIncomingCallHandler(private val context: Context) {
    private val appContext = context.applicationContext
    private val powerManager get() = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    private val alert = IncomingCallAlert(appContext)

    fun show(call: SipCall) {
        acquireWakeLock()
        alert.start()
        createChannel()
        val contentIntent = PendingIntent.getActivity(appContext, 0, callIntent(call), PendingIntent.FLAG_UPDATE_CURRENT or immutable())
        val answerIntent = PendingIntent.getBroadcast(appContext, 1, actionIntent(ACTION_ANSWER), PendingIntent.FLAG_UPDATE_CURRENT or immutable())
        val declineIntent = PendingIntent.getBroadcast(appContext, 2, actionIntent(ACTION_DECLINE), PendingIntent.FLAG_UPDATE_CURRENT or immutable())
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Incoming SIP call")
            .setContentText(call.remoteUri.ifBlank { "Incoming call" })
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .addAction(android.R.drawable.ic_menu_call, "Answer", answerIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declineIntent)
            .build()
        if (canPostNotifications()) {
            try {
                NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Notification permission can be revoked while the app is running.
            }
        }
    }

    fun stop() {
        alert.stop()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        try {
            NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID)
        } catch (_: SecurityException) {
            // Safe cleanup if notification permission/state changed.
        }
    }

    private fun acquireWakeLock() {
        wakeLock?.let { if (it.isHeld) return }
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SipCaller:IncomingCall").apply {
            setReferenceCounted(false)
            acquire(30_000L)
        }
    }

    private fun callIntent(call: SipCall) = Intent(appContext, CallActivity::class.java)
        .putExtra(CallActivity.EXTRA_INCOMING, true)
        .putExtra(CallActivity.EXTRA_REMOTE, call.remoteUri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    private fun actionIntent(action: String) = Intent(appContext, IncomingCallActionReceiver::class.java).setAction(action)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = appContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            })
        }
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            appContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun immutable() = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0

    companion object {
        const val CHANNEL_ID = "sip_incoming_calls"
        const val NOTIFICATION_ID = 1002
        const val ACTION_ANSWER = "com.example.sipcaller.ANSWER"
        const val ACTION_DECLINE = "com.example.sipcaller.DECLINE"
    }
}
