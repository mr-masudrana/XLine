package com.example.sipcaller

import android.util.Log
import org.pjsip.pjsua2.*

class SipAccount(private val cfg: AccountConfig, val accCfgDomain: String, val accCfgPort: Int) : Account() {

    private val TAG = "SipAccount"

    override fun onRegState(prm: OnRegStateParam) {
        val code = prm.code
        val isOk = code in 200..299
        Log.i(TAG, "Registration state: $code ${prm.reason}")
        SipManager.setRegistered(isOk)
        SipManager.dispatchRegistration(isOk, "$code ${prm.reason}")
    }

    override fun onIncomingCall(prm: OnIncomingCallParam) {
        val call = SipCall(this, prm.callId)
        Log.i(TAG, "Incoming call, id=${prm.callId}")
        SipManager.dispatchIncoming(call)
    }
}
