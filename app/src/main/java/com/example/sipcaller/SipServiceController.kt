package com.example.sipcaller

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Single entry point for starting/stopping the persistent SIP service. */
object SipServiceController {
    fun start(context: Context) {
        ContextCompat.startForegroundService(context.applicationContext,
            Intent(context.applicationContext, SipCallService::class.java).setAction(SipCallService.ACTION_ENSURE))
    }
    fun stop(context: Context) {
        context.applicationContext.stopService(Intent(context.applicationContext, SipCallService::class.java))
    }
}
