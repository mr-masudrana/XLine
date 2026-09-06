package com.example.sipcaller.diagnostics

import com.example.sipcaller.SipEngine
import com.example.sipcaller.SipLifecycleGuard

object AppStabilityDiagnostics {
    fun report(): String = buildString {
        appendLine("SIP engine ready: ${SipEngine.isReady()}")
        appendLine("Lifecycle running: ${SipLifecycleGuard.isRunning()}")
        appendLine("Native calls are serialized through SipEngine.nativeLock")
        appendLine("UI lifecycle uses defensive callback/listener cleanup")
    }
}
