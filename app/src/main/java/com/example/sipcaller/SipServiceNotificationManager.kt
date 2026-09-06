package com.example.sipcaller

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/** Central owner for the persistent SIP service notification and its actions. */
class SipServiceNotificationManager(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_SERVICE, "SIP Connection", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows SIP registration and service status" })
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ALERTS, "SIP Alerts", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Shows registration failures and recovery status" })
        }
    }

    fun service(status: String, registered: Boolean): Notification {
        val open = PendingIntent.getActivity(context, 10,
            Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), pendingFlags())
        val stop = PendingIntent.getBroadcast(context, 11,
            Intent(context, SipServiceActionReceiver::class.java).setAction(ACTION_STOP_SERVICE), pendingFlags())
        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(if (registered) android.R.drawable.presence_online else android.R.drawable.presence_invisible)
            .setContentTitle("XLine SIP")
            .setContentText(status)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun alert(title: String, message: String) {
        manager.notify(ALERT_NOTIFICATION_ID, NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title).setContentText(message)
            .setAutoCancel(true).setOnlyAlertOnce(true).build())
    }

    fun update(status: String, registered: Boolean) {
        manager.notify(SipCallService.NOTIFICATION_ID, service(status, registered))
    }

    private fun pendingFlags(): Int = PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    companion object {
        const val CHANNEL_SERVICE = "sip_call_service_channel"
        const val CHANNEL_ALERTS = "sip_alerts_channel"
        const val ALERT_NOTIFICATION_ID = 1003
        const val ACTION_STOP_SERVICE = "com.example.sipcaller.action.STOP_SERVICE"
    }
}
