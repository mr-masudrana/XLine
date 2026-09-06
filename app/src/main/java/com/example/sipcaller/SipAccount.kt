package com.example.sipcaller

import android.util.Log
import com.example.sipcaller.diagnostics.SipRegistrationMonitor
import org.pjsip.pjsua2.*

class SipAccount(private val cfg: AccountConfig, val accCfgDomain: String, val accCfgPort: Int) : Account() {

    private val TAG = "SipAccount"

    override fun onRegState(prm: OnRegStateParam) {
        synchronized(SipManager.nativeLock) {
        val code = prm.code.swigValue()
        val reason = prm.reason ?: ""
        val isOk = code in 200..299
        Log.i(TAG, "Registration state: $code $reason")
        if (isOk) {
            SipRegistrationMonitor.registered(code, reason.ifBlank { "OK" })
        } else {
            SipRegistrationMonitor.failed(code, reason.ifBlank { "Registration failed" })
        }
        SipManager.dispatchRegistration(isOk, "$code $reason".trim())
        }
    }

    override fun onIncomingCall(prm: OnIncomingCallParam) {
        synchronized(SipManager.nativeLock) {
        val call = SipCall(this, prm.callId)
        Log.i(TAG, "Incoming call, id=${prm.callId}")
        SipManager.dispatchIncoming(call)
        }
    }
}
