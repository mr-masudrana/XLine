package com.example.sipcaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sipcaller.data.repository.AppSettingsRepository

/** Restores SIP registration after device boot when a profile exists. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED != intent.action && Intent.ACTION_MY_PACKAGE_REPLACED != intent.action) return
        val settings = AppSettingsRepository(context.applicationContext)
        if (settings.autoStart && SipPreferences(context).load() != null) {
            SipServiceController.start(context)
        }
    }
}
