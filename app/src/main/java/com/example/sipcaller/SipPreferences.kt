package com.example.sipcaller

import android.content.Context

/** Persistent SIP profile. Password is stored in app-private preferences; for high-security
 * deployments replace with Android Keystore-backed encryption. */
class SipPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sip_profile", Context.MODE_PRIVATE)

    fun save(accountName: String, creds: SipManager.SipCredentials) {
        prefs.edit().putString("account_name", accountName).putString("username", creds.username)
            .putString("password", creds.password).putString("domain", creds.domain)
            .putInt("port", creds.port).putString("proxy", creds.proxy).apply()
    }
    fun load(): SipManager.SipCredentials? {
        val username = prefs.getString("username", null) ?: return null
        val password = prefs.getString("password", null) ?: return null
        val domain = prefs.getString("domain", null) ?: return null
        return SipManager.SipCredentials(username, password, domain, prefs.getInt("port", 5060), prefs.getString("proxy", null))
    }
    fun accountName(): String = prefs.getString("account_name", "") ?: ""
    fun clear() = prefs.edit().clear().apply()
}
