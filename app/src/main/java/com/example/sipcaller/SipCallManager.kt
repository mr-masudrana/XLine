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
            val raw = destination.trim()
            val target = raw.removePrefix("sip:").replace(Regex("[\\s()\\-]"), "")
            val uri = when {
                raw.startsWith("sip:", ignoreCase = true) && raw.contains("@") -> raw
                target.contains("@") -> "sip:$target"
                acc.accCfgPort > 0 && acc.accCfgPort != 5060 -> "sip:$target@${acc.accCfgDomain}:${acc.accCfgPort}"
                else -> "sip:$target@${acc.accCfgDomain}"
            }
            Log.i(TAG, "Starting outgoing call: $uri")
            SipCall(acc).also { call ->
                activeCall = call
                val prm = CallOpParam(true).apply {
                    opt.audioCount = 1
                    opt.videoCount = 0
                }
                call.makeCall(uri, prm)
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
