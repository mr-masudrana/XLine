package com.example.sipcaller

import android.util.Log
import java.util.concurrent.CopyOnWriteArraySet
import org.pjsip.pjsua2.*

/** Process-wide PJSUA2 gateway. UI never owns the native endpoint. */
object SipManager {
    private const val TAG = "SipManager"
    private var endpoint: Endpoint? = null
    private var account: SipAccount? = null
    private var credentials: SipCredentials? = null
    private var isRegistered = false
    private var activeCall: SipCall? = null
    private val listeners = CopyOnWriteArraySet<SipCallListener>()

    data class SipCredentials(val username: String, val password: String, val domain: String, val port: Int = 5060, val proxy: String? = null)
    interface SipCallListener {
        fun onIncomingCall(call: SipCall)
        fun onCallStateChanged(call: SipCall, state: String)
        fun onRegistrationStateChanged(isRegistered: Boolean, statusText: String)
    }
    fun addListener(listener: SipCallListener) { listeners.add(listener) }
    fun removeListener(listener: SipCallListener) { listeners.remove(listener) }
    fun isAccountRegistered() = isRegistered
    fun currentCall() = activeCall
    fun setRegistered(registered: Boolean) { isRegistered = registered }
    fun dispatchRegistration(registered: Boolean, text: String) { listeners.forEach { it.onRegistrationStateChanged(registered, text) } }
    fun dispatchIncoming(call: SipCall) { activeCall = call; listeners.forEach { it.onIncomingCall(call) } }
    fun dispatchCallState(call: SipCall, state: String) { activeCall = call; listeners.forEach { it.onCallStateChanged(call, state) } }

    @Synchronized fun init(): Boolean {
        if (endpoint != null) return true
        return try {
            Endpoint().also { ep ->
                ep.libCreate(); val cfg = EpConfig(); cfg.logConfig.level = 4; cfg.logConfig.consoleLevel = 4; cfg.uaConfig.maxCalls = 4
                ep.libInit(cfg); val transport = TransportConfig(); transport.port = 0L
                ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, transport); ep.libStart(); endpoint = ep
            }
            true
        } catch (e: Exception) { Log.e(TAG, "PJSIP init failed", e); endpoint = null; false }
    }

    @Synchronized fun registerAccount(creds: SipCredentials): Boolean {
        if (!init()) return false
        credentials = creds; isRegistered = false
        return try {
            account?.delete(); val cfg = AccountConfig(); val host = "${creds.domain}:${creds.port}"
            cfg.idUri = "sip:${creds.username}@$host"; cfg.regConfig.registrarUri = "sip:$host"; cfg.regConfig.registerOnAdd = true
            cfg.regConfig.timeoutSec = 300L; cfg.sipConfig.authCreds.add(AuthCredInfo("digest", "*", creds.username, 0, creds.password))
            if (!creds.proxy.isNullOrBlank()) cfg.sipConfig.proxies.add(creds.proxy)
            cfg.natConfig.udpKaIntervalSec = 15L
            SipAccount(cfg, creds.domain, creds.port).also { it.create(cfg); account = it }; true
        } catch (e: Exception) { Log.e(TAG, "Registration setup failed", e); false }
    }

    fun restoreAndRegister(context: android.content.Context): Boolean = SipPreferences(context).load()?.let { registerAccount(it) } ?: false
    fun makeCall(destination: String): SipCall? {
        val acc = account ?: return null
        if (!isRegistered) {
            Log.w(TAG, "makeCall rejected: account is not registered")
            return null
        }
        return try {
            // Phone contacts often contain spaces, dashes and parentheses. They are not valid SIP URI characters.
            val target = destination.removePrefix("sip:").trim().replace(Regex("[\\s()\\-]"), "")
            val uri = if (target.contains("@")) {
                "sip:$target"
            } else {
                "sip:$target@${acc.accCfgDomain}:${acc.accCfgPort}"
            }
            Log.i(TAG, "Starting SIP INVITE to $uri")
            val prm = CallOpParam(true)
            SipCall(acc).also { call ->
                activeCall = call
                call.makeCall(uri, prm)
            }
        } catch (e: Exception) {
            Log.e(TAG, "makeCall failed for $destination", e)
            null
        }
    }
    fun clearActiveCall(call: SipCall? = null) { if (call == null || activeCall === call) activeCall = null }
    @Synchronized fun shutdown() { try { activeCall?.delete(); activeCall = null; account?.delete(); account = null; endpoint?.libDestroy(); endpoint?.delete() } catch (e: Exception) { Log.e(TAG, "shutdown", e) } finally { endpoint = null; isRegistered = false } }
}
