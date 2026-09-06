package com.example.sipcaller

import android.util.Log
import com.example.sipcaller.diagnostics.SipDiagnostics
import org.pjsip.pjsua2.*

/** Owns account configuration, registration and credential persistence state. */
internal object SipAccountManager {
    private const val TAG = "SipAccountManager"
    @Volatile private var account: SipAccount? = null
    @Volatile private var credentials: SipManager.SipCredentials? = null
    @Volatile private var registered = false

    fun account(): SipAccount? = account
    fun isRegistered() = registered
    fun credentials() = credentials

    fun register(creds: SipManager.SipCredentials): Boolean {
        if (!SipEngine.start()) return false
        return SipEngine.call {
            try {
                try { account?.delete() } catch (_: Throwable) {}
                account = null
                registered = false
                credentials = creds

                val username = creds.username.trim()
                val password = creds.password
                val parsed = parseServer(creds.domain, creds.port)
                val host = parsed.first
                val port = parsed.second

                require(username.isNotBlank()) { "SIP username is empty" }
                require(host.isNotBlank()) { "SIP domain is empty" }

                val cfg = AccountConfig().apply {
                    // Provider-compatible split: identity uses host only, registrar owns port.
                    // This mirrors the proven IPDial account layout.
                    idUri = "sip:$username@$host"
                    regConfig.registrarUri = if (port > 0) "sip:$host:$port" else "sip:$host"
                    regConfig.registerOnAdd = true
                    regConfig.timeoutSec = 180L
                    regConfig.retryIntervalSec = 30L
                    regConfig.firstRetryIntervalSec = 15L
                    regConfig.delayBeforeRefreshSec = 90L

                    sipConfig.authCreds.add(
                        AuthCredInfo("digest", "*", username, 0, password)
                    )

                    normalizeProxy(creds.proxy)?.let { sipConfig.proxies.add(it) }

                    // Explicitly bind the account to the UDP transport created by SipEngine.
                    val transportId = SipEngine.currentUdpTransportId()
                    if (transportId >= 0) sipConfig.transportId = transportId

                    // Mobile/NAT settings aligned with the working IPDial reference.
                    natConfig.iceEnabled = false
                    natConfig.turnEnabled = false
                    natConfig.sipStunUse = pjsua_stun_use.PJSUA_STUN_USE_DEFAULT
                    natConfig.contactRewriteUse = 1
                    natConfig.sipOutboundUse = 0
                    natConfig.udpKaIntervalSec = 15L
                }

                account = SipAccount(cfg, host, port).also { it.create(cfg) }
                SipDiagnostics.info(
                    TAG,
                    "Registration requested identity=sip:$username@$host registrar=${cfg.regConfig.registrarUri} transport=${SipEngine.currentUdpTransportId()}"
                )
                true
            } catch (t: Throwable) {
                registered = false
                Log.e(TAG, "Account registration setup failed", t)
                SipDiagnostics.error(TAG, "Account registration setup failed", t)
                false
            }
        } ?: false
    }

    private fun parseServer(rawDomain: String, requestedPort: Int): Pair<String, Int> {
        var value = rawDomain.trim()
            .removePrefix("sip:")
            .removePrefix("SIP:")
            .removePrefix("sips:")
            .removePrefix("SIPS:")
            .substringBefore("/")

        var port = requestedPort
        // Simple host:port parsing for the SIP account format used by this project.
        val colon = value.lastIndexOf(':')
        if (colon > 0 && value.indexOf(':') == colon) {
            val parsedPort = value.substring(colon + 1).toIntOrNull()
            if (parsedPort != null) {
                value = value.substring(0, colon)
                if (port <= 0 || port == 5060) port = parsedPort
            }
        }
        return value.trim() to port
    }

    private fun normalizeProxy(rawProxy: String?): String? {
        val value = rawProxy?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val noScheme = value.removePrefix("sip:").removePrefix("SIP:")
        val base = "sip:$noScheme"
        return if (base.contains(";lr", ignoreCase = true)) base else "$base;lr"
    }

    fun onRegistrationState(value: Boolean, text: String) {
        registered = value
        SipDiagnostics.info(TAG, "Registration state=$value message=$text")
        SipEventDispatcher.registration(value, text)
    }

    fun destroy() {
        SipEngine.call {
            try { account?.delete() }
            catch (t: Throwable) { Log.e(TAG, "Account destroy failed", t) }
            finally { account = null; credentials = null; registered = false }
        }
    }
}
