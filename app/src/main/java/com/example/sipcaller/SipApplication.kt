package com.example.sipcaller

import android.app.Application

class SipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SipManager.init()
    }

    override fun onTerminate() {
        SipManager.shutdown()
        super.onTerminate()
    }
}
