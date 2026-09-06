package com.example.sipcaller

import android.util.Log
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
            val prm = CallOpParam()
            prm.statusCode = pjsip_status_code.PJSIP_SC_OK
            answer(prm)
        }.onFailure { Log.e(TAG, "answer() failed", it) }
    }

    fun decline() = SipManager.postNative {
        runCatching {
            val prm = CallOpParam()
            prm.statusCode = pjsip_status_code.PJSIP_SC_BUSY_HERE
            hangup(prm)
        }.onFailure { Log.e(TAG, "decline() failed", it) }
    }

    fun hangupCall() = SipManager.postNative {
        runCatching {
            val prm = CallOpParam()
            prm.statusCode = pjsip_status_code.PJSIP_SC_DECLINE
            hangup(prm)
        }.onFailure { Log.e(TAG, "hangupCall() failed", it) }
    }

    fun toggleHold() = SipManager.postNative {
        runCatching {
            val prm = CallOpParam(true)
            if (isOnHold) reinvite(prm) else setHold(prm)
            isOnHold = !isOnHold
        }.onFailure { Log.e(TAG, "toggleHold() failed", it) }
    }

    fun sendDtmf(digits: String) = SipManager.postNative {
        runCatching { dialDtmf(digits) }
            .onFailure { Log.e(TAG, "sendDtmf() failed", it) }
    }

    fun durationSeconds(now: Long = System.currentTimeMillis()): Long {
        val from = if (connectedAt > 0) connectedAt else startedAt
        val until = if (endedAt > 0) endedAt else now
        return ((until - from) / 1000L).coerceAtLeast(0L)
    }

    fun isDisconnected(): Boolean = lastState.contains("DISCONN", true) || endedAt > 0L

    override fun onCallState(prm: OnCallStateParam) {
        synchronized(SipManager.nativeLock) {
            try {
                val ci = info
                remoteUri = ci.remoteUri
                lastState = ci.state.toString()
                lastStatusCode = ci.lastStatusCode.toInt()
                lastReason = ci.lastReason
                if (lastState.contains("CONFIRMED", true) && connectedAt == 0L) connectedAt = System.currentTimeMillis()
                if (lastState.contains("DISCONN", true)) endedAt = System.currentTimeMillis()
                Log.i(TAG, "Call state=$lastState code=$lastStatusCode reason=$lastReason remote=$remoteUri")
                SipManager.dispatchCallState(this, lastState)
                if (lastState.contains("DISCONN", true)) SipManager.clearActiveCall(this)
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
                    if (mi.type == pjmedia_type.PJMEDIA_TYPE_AUDIO && mi.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE) {
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
}
