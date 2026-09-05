package com.example.sipcaller

import android.util.Log
import org.pjsip.pjsua2.*

/**
 * Thin wrapper around PJSUA2 (pjsua2.aar). Owns the Endpoint lifecycle,
 * SIP account registration, and outgoing/incoming call setup.
 *
 * Transport is fixed to UDP per project requirements. If your provider
 * ever moves to TCP/TLS, add a second TransportConfig + createTransport
 * call using PJSIP_TRANSPORT_TCP / PJSIP_TRANSPORT_TLS.
 */
object SipManager {

    private const val TAG = "SipManager"

    private var endpoint: Endpoint? = null
    private var account: SipAccount? = null

    data class SipCredentials(
        val username: String,     // SIP extension / auth username
        val password: String,
        val domain: String,       // e.g. sip.yourprovider.com
        val port: Int = 5060,     // default SIP UDP port, override if your provider uses another
        val proxy: String? = null // optional outbound proxy, e.g. "sip:sbc.yourprovider.com:5060"
    )

    var callListener: SipCallListener? = null

    interface SipCallListener {
        fun onIncomingCall(call: SipCall)
        fun onCallStateChanged(call: SipCall, state: String)
        fun onRegistrationStateChanged(isRegistered: Boolean, statusText: String)
    }

    /** Call once, e.g. from Application.onCreate(). */
    fun init() {
        if (endpoint != null) return

        try {
            endpoint = Endpoint()
            endpoint!!.libCreate()

            val epConfig = EpConfig()
            epConfig.logConfig.level = 4
            epConfig.logConfig.consoleLevel = 4
            epConfig.uaConfig.maxCalls = 4

            endpoint!!.libInit(epConfig)

            // --- UDP transport ---
            val udpCfg = TransportConfig()
            udpCfg.port = 0L // 0 = let PJSIP pick a free local port
            endpoint!!.transportCreate(
                pjsip_transport_type_e.PJSIP_TRANSPORT_UDP,
                udpCfg
            )

            endpoint!!.libStart()
            Log.i(TAG, "PJSIP endpoint started (UDP)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init PJSIP endpoint", e)
        }
    }

    fun registerAccount(creds: SipCredentials) {
        try {
            account?.delete()

            val accCfg = AccountConfig()
            val sipUri = "sip:${creds.username}@${creds.domain}:${creds.port}"
            accCfg.idUri = sipUri
            accCfg.regConfig.registrarUri = "sip:${creds.domain}:${creds.port}"
            accCfg.regConfig.registerOnAdd = true
            accCfg.regConfig.timeoutSec = 300L // re-register every 5 min, keeps UDP NAT binding alive

            val cred = AuthCredInfo(
                "digest",
                creds.domain,
                creds.username,
                0,
                creds.password
            )
            accCfg.sipConfig.authCreds.add(cred)

            if (!creds.proxy.isNullOrBlank()) {
                accCfg.sipConfig.proxies.add(creds.proxy)
            }

            // NAT keep-alive: important on UDP, sends periodic empty packets
            // so the router's NAT mapping doesn't expire and drop incoming calls.
            accCfg.natConfig.udpKaIntervalSec = 15L

            account = SipAccount(accCfg)
            account!!.create(accCfg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register account", e)
        }
    }

    fun makeCall(destinationNumber: String): SipCall? {
        val acc = account ?: run {
            Log.e(TAG, "No account registered yet")
            return null
        }
        return try {
            val destUri = "sip:$destinationNumber@${acc.accCfgDomain}"
            val call = SipCall(acc)
            val prm = CallOpParam(true)
            call.makeCall(destUri, prm)
            call
        } catch (e: Exception) {
            Log.e(TAG, "makeCall failed", e)
            null
        }
    }

    fun shutdown() {
        try {
            account?.delete()
            account = null
            endpoint?.libDestroy()
            endpoint?.delete()
            endpoint = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }
}
