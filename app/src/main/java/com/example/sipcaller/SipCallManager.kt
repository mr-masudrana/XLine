package com.example.sipcaller

import android.util.Log
import org.pjsip.pjsua2.CallOpParam

/** Owns the active call reference and all app initiated call creation. */
internal object SipCallManager {
    private const val TAG = "SipCallManager"
    @Volatile private var activeCall: SipCall? = null
    fun currentCall(): SipCall? = activeCall

    fun makeCall(destination: String): SipCall? = SipEngine.call {
        val acc = SipAccountManager.account() ?: return@call null
        if (!SipAccountManager.isRegistered()) return@call null
        try {
            val target = destination.removePrefix("sip:").trim().replace(Regex("[\\s()\\-]"), "")
            val uri = if (target.contains("@")) "sip:$target" else "sip:$target@${acc.accCfgDomain}:${acc.accCfgPort}"
            SipCall(acc).also { call ->
                activeCall = call
                call.makeCall(uri, CallOpParam(true))
            }
        } catch (t: Throwable) { Log.e(TAG, "makeCall failed", t); null }
    }

    fun incoming(call: SipCall) {
        // Defensive single-call policy: reject ownership races rather than replacing
        // a live native call reference silently.
        if (activeCall != null && activeCall !== call) {
            Log.w(TAG, "Ignoring second concurrent call reference")
            return
        }
        activeCall = call
        SipEventDispatcher.incoming(call)
    }
    fun stateChanged(call: SipCall, state: String) {
        activeCall = call
        SipEventDispatcher.callState(call, state)
    }
    fun clear(call: SipCall? = null) { if (call == null || activeCall === call) activeCall = null }
    fun destroy() = SipEngine.call { try { activeCall?.delete() } catch (_: Throwable) {} finally { activeCall = null } }
}
