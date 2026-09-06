package com.example.sipcaller

import android.util.Log
import org.pjsip.pjsua2.*

class SipAccount(private val cfg: AccountConfig, val accCfgDomain: String, val accCfgPort: Int) : Account() {

    private val TAG = "SipAccount"

    override fun onRegState(prm: OnRegStateParam) {
        synchronized(SipManager.nativeLock) {
        val code = prm.code
        val isOk = code in 200..299
        Log.i(TAG, "Registration state: $code ${prm.reason}")
        SipManager.dispatchRegistration(isOk, "$code ${prm.reason}")
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
