package com.example.sipcaller

import java.util.concurrent.CopyOnWriteArraySet

/** Keeps UI/service listeners independent from the native SIP engine. */
internal object SipEventDispatcher {
    private val listeners = CopyOnWriteArraySet<SipManager.SipCallListener>()
    fun add(listener: SipManager.SipCallListener) { listeners.add(listener) }
    fun remove(listener: SipManager.SipCallListener) { listeners.remove(listener) }
    fun registration(registered: Boolean, text: String) = listeners.forEach {
        runCatching { it.onRegistrationStateChanged(registered, text) }
            .onFailure { e -> com.example.sipcaller.diagnostics.ProductionReadinessDiagnostics.listenerFailure(); com.example.sipcaller.diagnostics.SipFailureReporter.reportException("Registration event delivery", e) }
    }
    fun incoming(call: SipCall) = listeners.forEach {
        runCatching { it.onIncomingCall(call) }
            .onFailure { e -> com.example.sipcaller.diagnostics.ProductionReadinessDiagnostics.listenerFailure(); com.example.sipcaller.diagnostics.SipFailureReporter.reportException("Incoming call event delivery", e) }
    }
    fun callState(call: SipCall, state: String) = listeners.forEach {
        runCatching { it.onCallStateChanged(call, state) }
            .onFailure { e -> com.example.sipcaller.diagnostics.ProductionReadinessDiagnostics.listenerFailure(); com.example.sipcaller.diagnostics.SipFailureReporter.reportException("Call state event delivery", e) }
    }
}
