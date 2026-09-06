package com.example.sipcaller

import android.content.Context

/** Single source of truth for the currently ringing SIP call. */
object SipIncomingCallController {
    private var handler: SipIncomingCallHandler? = null
    private var ringingCall: SipCall? = null

    fun incoming(context: Context, call: SipCall) {
        ringingCall = call
        CallActivity.pendingCall = call
        handler = SipIncomingCallHandler(context).also { it.show(call) }
    }
    fun answer(context: Context) { ringingCall?.answer(); stop() }
    fun decline() { ringingCall?.decline(); stop() }
    fun stop() { handler?.stop(); handler = null; ringingCall = null }
}
