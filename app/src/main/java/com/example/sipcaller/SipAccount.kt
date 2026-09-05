package com.example.sipcaller

import android.util.Log
import org.pjsip.pjsua2.*

class SipAccount(private val cfg: AccountConfig) : Account() {

    private val TAG = "SipAccount"

    // Domain kept around so SipManager can build "sip:number@domain" URIs
    val accCfgDomain: String = cfg.idUri.substringAfter("@").substringBefore(":")

    fun create(config: AccountConfig) {
        super.create(config)
    }

    override fun onRegState(prm: OnRegStateParam) {
        val code = prm.code.swigValue()
        val isOk = code in 200..299
        Log.i(TAG, "Registration state: $code ${prm.reason}")
        SipManager.callListener?.onRegistrationStateChanged(isOk, "$code ${prm.reason}")
    }

    override fun onIncomingCall(prm: OnIncomingCallParam) {
        val call = SipCall(this, prm.callId)
        Log.i(TAG, "Incoming call, id=${prm.callId}")
        SipManager.callListener?.onIncomingCall(call)
    }
}
