package com.example.sipcaller

import java.util.concurrent.CopyOnWriteArraySet

/** Keeps UI/service listeners independent from the native SIP engine. */
internal object SipEventDispatcher {
    private val listeners = CopyOnWriteArraySet<SipManager.SipCallListener>()
    fun add(listener: SipManager.SipCallListener) { listeners.add(listener) }
    fun remove(listener: SipManager.SipCallListener) { listeners.remove(listener) }
    fun registration(registered: Boolean, text: String) = listeners.forEach { it.onRegistrationStateChanged(registered, text) }
    fun incoming(call: SipCall) = listeners.forEach { it.onIncomingCall(call) }
    fun callState(call: SipCall, state: String) = listeners.forEach { it.onCallStateChanged(call, state) }
}
