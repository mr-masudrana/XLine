package com.example.sipcaller.diagnostics

import com.example.sipcaller.SipCall

object SipCallControlDiagnostics {
    fun report(call: SipCall?): String = buildString {
        appendLine("Call present: ${call != null}")
        appendLine("State: ${call?.lastState ?: "none"}")
        appendLine("On hold: ${call?.isOnHold ?: false}")
        appendLine("Muted: ${call?.isMuted ?: false}")
        appendLine("SIP status: ${call?.lastStatusCode ?: 0}")
        appendLine("Reason: ${call?.lastReason.orEmpty()}")
    }
}
