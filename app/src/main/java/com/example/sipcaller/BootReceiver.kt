package com.example.sipcaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sipcaller.data.repository.AppSettingsRepository

/** Restores persistent SIP ownership after boot or app update when explicitly enabled. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending = goAsync()
        Thread {
            try {
                val app = context.applicationContext
                val settings = AppSettingsRepository(app)
                if (settings.autoStart && SipPreferences(app).load() != null) {
                    SipServiceController.start(app)
                }
            } finally { pending.finish() }
        }.start()
    }
}
