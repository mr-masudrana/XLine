package com.example.sipcaller

import android.app.Application

class SipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.sipcaller.theme.AppThemeController.apply(this)
        // UI launches should recover the persistent SIP owner if a profile already exists.
        if (SipPreferences(this).load() != null && com.example.sipcaller.util.SipStartupGate.tryEnter()) {
            runCatching { SipServiceController.start(this) }
                .finally { com.example.sipcaller.util.SipStartupGate.leave() }
        }
    }
}
