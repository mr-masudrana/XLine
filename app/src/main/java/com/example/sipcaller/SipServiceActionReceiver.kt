package com.example.sipcaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles notification actions without exposing the service directly. */
class SipServiceActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SipServiceNotificationManager.ACTION_STOP_SERVICE) {
            SipServiceController.stop(context)
        }
    }
}
