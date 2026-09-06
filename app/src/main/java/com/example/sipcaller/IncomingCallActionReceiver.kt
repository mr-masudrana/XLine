package com.example.sipcaller

import android.content.*

class IncomingCallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val call = SipManager.currentCall() ?: CallActivity.pendingCall
        when (intent.action) {
            SipIncomingCallHandler.ACTION_ANSWER -> {
                SipIncomingCallController.answer(context)
                context.startActivity(Intent(context, CallActivity::class.java).putExtra(CallActivity.EXTRA_INCOMING, true)
                    .putExtra(CallActivity.EXTRA_REMOTE, call?.remoteUri ?: "")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }
            SipIncomingCallHandler.ACTION_DECLINE -> SipIncomingCallController.decline()
        }
    }
}
