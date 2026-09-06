package com.example.sipcaller

import android.util.Log
import org.pjsip.pjsua2.*

class SipCall : Call {

    private val TAG = "SipCall"
    var remoteUri: String = ""
        private set
    var isOnHold = false
        private set
    var isMuted = false
        private set

    constructor(acc: SipAccount) : super(acc)
    constructor(acc: SipAccount, callId: Int) : super(acc, callId)

    fun answer() {
        try {
            val prm = CallOpParam()
            prm.statusCode = pjsip_status_code.PJSIP_SC_OK
            answer(prm)
        } catch (e: Exception) {
            Log.e(TAG, "answer() failed", e)
        }
    }

    fun decline() {
        try {
            val prm = CallOpParam()
            prm.statusCode = pjsip_status_code.PJSIP_SC_BUSY_HERE
            hangup(prm)
        } catch (e: Exception) {
            Log.e(TAG, "decline() failed", e)
        }
    }

    fun hangupCall() {
        try {
            val prm = CallOpParam()
            prm.statusCode = pjsip_status_code.PJSIP_SC_DECLINE
            hangup(prm)
        } catch (e: Exception) {
            Log.e(TAG, "hangupCall() failed", e)
        }
    }

    fun toggleHold() {
        try {
            val prm = CallOpParam(true)
            if (isOnHold) {
                reinvite(prm)
            } else {
                setHold(prm)
            }
            isOnHold = !isOnHold
        } catch (e: Exception) {
            Log.e(TAG, "toggleHold() failed", e)
        }
    }

    fun sendDtmf(digits: String) {
        try {
            dialDtmf(digits)
        } catch (e: Exception) {
            Log.e(TAG, "sendDtmf() failed", e)
        }
    }

    override fun onCallState(prm: OnCallStateParam) {
        try {
            val ci = info
            remoteUri = ci.remoteUri
            val stateText = ci.stateText
            Log.i(TAG, "Call state: $stateText")
            SipManager.dispatchCallState(this, stateText)
            if (ci.state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) SipManager.clearActiveCall(this)
        } catch (e: Exception) {
            Log.e(TAG, "onCallState error", e)
        }
    }

    override fun onCallMediaState(prm: OnCallMediaStateParam) {
        try {
            val ci = info
            for (mi in ci.media) {
                if (mi.type == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
                    mi.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE
                ) {
                    val aum = AudioMedia.typecastFromMedia(getMedia(mi.index))
                    val ep = org.pjsip.pjsua2.Endpoint.instance()
                    ep.audDevManager().captureDevMedia.startTransmit(aum)
                    aum.startTransmit(ep.audDevManager().playbackDevMedia)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCallMediaState error", e)
        }
    }
}
