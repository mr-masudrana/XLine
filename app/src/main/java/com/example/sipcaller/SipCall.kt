package com.example.sipcaller

import android.util.Log
import com.example.sipcaller.diagnostics.SipCallFlowLogger
import com.example.sipcaller.diagnostics.SipErrorAnalyzer
import org.pjsip.pjsua2.*

class SipCall : Call {
    private val TAG = "SipCall"

    @Volatile var remoteUri: String = ""
        private set
    @Volatile var isOnHold = false
        private set
    @Volatile var isMuted = false
        private set
    @Volatile var lastState: String = ""
        private set
    @Volatile var lastStatusCode: Int = 0
        private set
    @Volatile var lastReason: String = ""
        private set
    @Volatile var startedAt: Long = System.currentTimeMillis()
        private set
    @Volatile var connectedAt: Long = 0L
        private set
    @Volatile var endedAt: Long = 0L
        private set

    constructor(acc: SipAccount) : super(acc)
    constructor(acc: SipAccount, callId: Int) : super(acc, callId)

    fun answer() = SipManager.postNative {
        runCatching {
            val prm = CallOpParam(true)
            prm.statusCode = pjsip_status_code.PJSIP_SC_OK
            answer(prm)
        }.onFailure { Log.e(TAG, "answer() failed", it); com.example.sipcaller.diagnostics.SipFailureReporter.reportException("Answer call", it) }
    }

    fun decline() = SipManager.postNative {
        runCatching {
            val prm = CallOpParam(true)
            prm.statusCode = pjsip_status_code.PJSIP_SC_BUSY_HERE
            hangup(prm)
        }.onFailure { Log.e(TAG, "decline() failed", it); com.example.sipcaller.diagnostics.SipFailureReporter.reportException("Decline call", it) }
    }

    fun hangupCall() = SipManager.postNative {
        runCatching {
            val prm = CallOpParam(true)
            prm.statusCode = pjsip_status_code.PJSIP_SC_DECLINE
            hangup(prm)
        }.onFailure { Log.e(TAG, "hangupCall() failed", it); com.example.sipcaller.diagnostics.SipFailureReporter.reportException("End call", it) }
    }

    fun toggleHold() = SipManager.postNative {
        runCatching {
            val prm = CallOpParam(true)
            if (isOnHold) reinvite(prm) else setHold(prm)
            isOnHold = !isOnHold
        }.onFailure { Log.e(TAG, "toggleHold() failed", it); com.example.sipcaller.diagnostics.SipFailureReporter.reportException("Hold/resume", it) }
    }

    fun sendDtmf(digits: String) = SipManager.postNative {
        runCatching { dialDtmf(digits) }
            .onFailure { Log.e(TAG, "sendDtmf() failed", it); com.example.sipcaller.diagnostics.SipFailureReporter.reportException("Send DTMF", it) }
    }

    fun durationSeconds(now: Long = System.currentTimeMillis()): Long {
        val from = if (connectedAt > 0) connectedAt else startedAt
        val until = if (endedAt > 0) endedAt else now
        return ((until - from) / 1000L).coerceAtLeast(0L)
    }

    fun isDisconnected(): Boolean = lastState == STATE_DISCONNECTED || endedAt > 0L

    override fun onCallState(prm: OnCallStateParam) {
        synchronized(SipManager.nativeLock) {
            try {
                val ci = info
                remoteUri = ci.remoteUri
                lastState = mapState(ci.state)
                lastStatusCode = try { ci.lastStatusCode.swigValue() } catch (_: Throwable) { 0 }
                lastReason = ci.lastReason ?: ""
                val callId = try { getId() } catch (_: Throwable) { -1 }
                SipCallFlowLogger.record(callId, lastState, lastStatusCode, lastReason, remoteUri)
                if (lastState == STATE_DISCONNECTED && lastStatusCode >= 300) {
                    SipErrorAnalyzer.log(lastStatusCode, lastReason, callId, remoteUri)
                    com.example.sipcaller.diagnostics.SipFailureReporter.reportSip(lastStatusCode, lastReason)
                }

                if (lastState == STATE_CONFIRMED && connectedAt == 0L) {
                    connectedAt = System.currentTimeMillis()
                }
                if (lastState == STATE_DISCONNECTED) {
                    endedAt = System.currentTimeMillis()
                }

                Log.i(
                    TAG,
                    "Call state=$lastState native=${ci.stateText} code=$lastStatusCode reason=$lastReason remote=$remoteUri"
                )
                SipManager.dispatchCallState(this, lastState)
                if (lastState == STATE_DISCONNECTED) {
                    SipManager.clearActiveCall(this)
                }
                Unit
            } catch (e: Throwable) {
                Log.e(TAG, "onCallState error", e)
            }
        }
    }

    override fun onCallMediaState(prm: OnCallMediaStateParam) {
        synchronized(SipManager.nativeLock) {
            try {
                val ci = info
                for (mi in ci.media) {
                    if (mi.type == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
                        mi.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE
                    ) {
                        val aum = AudioMedia.typecastFromMedia(getMedia(mi.index))
                        val ep = Endpoint.instance()
                        ep.audDevManager().captureDevMedia.startTransmit(aum)
                        aum.startTransmit(ep.audDevManager().playbackDevMedia)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "onCallMediaState error", e)
            }
        }
    }

    private fun mapState(state: pjsip_inv_state): String = when (state) {
        pjsip_inv_state.PJSIP_INV_STATE_CALLING -> STATE_CALLING
        pjsip_inv_state.PJSIP_INV_STATE_INCOMING -> STATE_INCOMING
        pjsip_inv_state.PJSIP_INV_STATE_EARLY -> STATE_EARLY
        pjsip_inv_state.PJSIP_INV_STATE_CONNECTING -> STATE_CONNECTING
        pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED -> STATE_CONFIRMED
        pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED,
        pjsip_inv_state.PJSIP_INV_STATE_NULL -> STATE_DISCONNECTED
        else -> STATE_IDLE
    }

    companion object {
        const val STATE_IDLE = "IDLE"
        const val STATE_CALLING = "CALLING"
        const val STATE_INCOMING = "INCOMING"
        const val STATE_EARLY = "EARLY"
        const val STATE_CONNECTING = "CONNECTING"
        const val STATE_CONFIRMED = "CONFIRMED"
        const val STATE_DISCONNECTED = "DISCONNECTED"
    }
}
