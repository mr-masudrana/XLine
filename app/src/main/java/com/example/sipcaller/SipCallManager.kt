package com.example.sipcaller

import android.util.Log
import com.example.sipcaller.diagnostics.SipDiagnostics
import org.pjsip.pjsua2.CallOpParam

/** Owns the active call reference and all app initiated call creation. */
internal object SipCallManager {
    private const val TAG = "SipCallManager"
    @Volatile private var activeCall: SipCall? = null

    fun currentCall(): SipCall? = activeCall

    fun makeCall(destination: String): SipCall? = SipEngine.call {
        val acc = SipAccountManager.account() ?: run {
            SipDiagnostics.error(TAG, "makeCall rejected: no SIP account", null)
            return@call null
        }
        if (!SipAccountManager.isRegistered()) {
            SipDiagnostics.info(TAG, "makeCall rejected: account is not registered")
            return@call null
        }

        try {
            val uri = formatDestination(destination, acc.accCfgDomain)
            require(uri.length > 4) { "Empty SIP destination" }

            Log.i(TAG, "Starting outgoing call: $uri")
            SipDiagnostics.info(TAG, "Outgoing INVITE target=$uri")

            val call = SipCall(acc)
            val prm = CallOpParam(true).apply {
                opt.audioCount = 1
                opt.videoCount = 0
            }

            call.makeCall(uri, prm)
            activeCall = call
            SipDiagnostics.info(TAG, "call.makeCall accepted target=$uri callId=${safeCallId(call)}")
            call
        } catch (t: Throwable) {
            activeCall = null
            Log.e(TAG, "makeCall failed", t)
            SipDiagnostics.error(TAG, "makeCall failed: ${t.message}", t)
            null
        }
    }

    private fun formatDestination(destination: String, accountDomain: String): String {
        val raw = destination.trim()
        if (raw.startsWith("sip:", ignoreCase = true) && raw.contains("@")) return raw
        if (raw.startsWith("sips:", ignoreCase = true) && raw.contains("@")) return raw

        // Match the working IPDial behavior: destination uses account domain only;
        // the registrar port is not appended to the Request-URI unless the user
        // explicitly supplied a full SIP URI.
        val number = raw
            .removePrefix("sip:")
            .removePrefix("SIP:")
            .substringBefore("@")
            .replace(Regex("[\\s()\\-]"), "")

        val domain = accountDomain.trim()
            .removePrefix("sip:")
            .removePrefix("SIP:")
            .substringBefore(":")

        return if (domain.isNotBlank()) "sip:$number@$domain" else "sip:$number"
    }

    private fun safeCallId(call: SipCall): Int = try { call.getId() } catch (_: Throwable) { -1 }

    fun incoming(call: SipCall) {
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

    fun clear(call: SipCall? = null) {
        if (call == null || activeCall === call) activeCall = null
    }

    fun destroy() = SipEngine.call {
        try { activeCall?.delete() } catch (_: Throwable) {}
        finally { activeCall = null }
    }
}
