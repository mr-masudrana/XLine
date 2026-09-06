package com.example.sipcaller

import android.content.Context

/** Process-wide owner of the current ringing call; duplicate native callbacks are ignored. */
object SipIncomingCallController {
    private var handler: SipIncomingCallHandler? = null
    @Volatile private var ringingCall: SipCall? = null

    @Synchronized
    fun incoming(context: Context, call: SipCall) {
        if (ringingCall === call && handler != null) return
        handler?.stop()
        ringingCall = call
        CallActivity.pendingCall = call
        handler = SipIncomingCallHandler(context.applicationContext).also { it.show(call) }
    }

    @Synchronized
    fun answer(context: Context) { ringingCall?.answer(); stop() }

    @Synchronized
    fun decline() { ringingCall?.decline(); stop() }

    @Synchronized
    fun stop() {
        handler?.stop()
        handler = null
        ringingCall = null
    }
}
