package com.example.sipcaller

/**
 * Backward-compatible public facade. Existing UI code can remain unchanged while
 * ownership is separated into SipEngine, SipAccountManager, SipCallManager and events.
 */
object SipManager {
    data class SipCredentials(val username: String, val password: String, val domain: String, val port: Int = 5060, val proxy: String? = null)
    interface SipCallListener {
        fun onIncomingCall(call: SipCall)
        fun onCallStateChanged(call: SipCall, state: String)
        fun onRegistrationStateChanged(isRegistered: Boolean, statusText: String)
    }

    internal val nativeLock get() = SipEngine.nativeLock
    fun addListener(listener: SipCallListener) = SipEventDispatcher.add(listener)
    fun removeListener(listener: SipCallListener) = SipEventDispatcher.remove(listener)
    fun isAccountRegistered() = SipAccountManager.isRegistered()
    fun currentCall() = SipCallManager.currentCall()
    fun init() = SipEngine.start()
    fun registerAccount(creds: SipCredentials) = SipAccountManager.register(creds)
    fun restoreAndRegister(context: android.content.Context): Boolean = SipPreferences(context).load()?.let { registerAccount(it) } ?: false
    fun makeCall(destination: String) = SipCallManager.makeCall(destination)
    internal fun postNative(block: () -> Unit) = SipEngine.post(block)
    internal fun <T> callNative(block: () -> T): T? = SipEngine.call(block)

    internal fun setRegistered(registered: Boolean) { SipAccountManager.onRegistrationState(registered, "") }
    internal fun dispatchRegistration(registered: Boolean, text: String) = SipAccountManager.onRegistrationState(registered, text)
    internal fun dispatchIncoming(call: SipCall) = SipCallManager.incoming(call)
    internal fun dispatchCallState(call: SipCall, state: String) = SipCallManager.stateChanged(call, state)
    internal fun clearActiveCall(call: SipCall? = null) = SipCallManager.clear(call)

    @Synchronized
    fun shutdown() {
        // Ordered teardown matters: calls -> account -> endpoint/thread.
        SipCallManager.destroy()
        SipAccountManager.destroy()
        SipEngine.stop()
    }
}
