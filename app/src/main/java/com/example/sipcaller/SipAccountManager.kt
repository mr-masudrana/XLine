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
                account?.delete()
                registered = false
                credentials = creds
                val cfg = AccountConfig()
                val host = "${creds.domain}:${creds.port}"
                cfg.idUri = "sip:${creds.username}@$host"
                cfg.regConfig.registrarUri = "sip:$host"
                cfg.regConfig.registerOnAdd = true
                cfg.regConfig.timeoutSec = 300L
                cfg.sipConfig.authCreds.add(AuthCredInfo("digest", "*", creds.username, 0, creds.password))
                if (!creds.proxy.isNullOrBlank()) cfg.sipConfig.proxies.add(creds.proxy)
                cfg.natConfig.udpKaIntervalSec = 15L
                account = SipAccount(cfg, creds.domain, creds.port).also { it.create(cfg) }
                SipDiagnostics.info(TAG, "Registration requested for ${creds.username}@${creds.domain}:${creds.port}")
                true
            } catch (t: Throwable) {
                Log.e(TAG, "Account registration setup failed", t)
                SipDiagnostics.error(TAG, "Account registration setup failed", t)
                false
            }
        } ?: false
    }

    fun onRegistrationState(value: Boolean, text: String) {
        registered = value
        SipDiagnostics.info(TAG, "Registration state=$value message=$text")
        SipEventDispatcher.registration(value, text)
    }

    fun destroy() {
        SipEngine.call {
            try { account?.delete() } catch (t: Throwable) { Log.e(TAG, "Account destroy failed", t) }
            finally { account = null; credentials = null; registered = false }
        }
    }
}
