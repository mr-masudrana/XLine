package com.example.sipcaller

import android.content.Context
import android.content.Intent

/** Keeps the in-memory session synchronized with the persisted SIP profile. */
object AccountSessionManager {
    fun restore(context: Context): Boolean {
        val prefs = SipPreferences(context)
        val creds = prefs.load() ?: return false
        if (!prefs.isEnabled()) return false
        SessionStore.accountName = prefs.accountName().ifBlank { creds.username }
        SessionStore.username = creds.username
        SessionStore.domain = creds.domain
        return true
    }

    fun activate(context: Context, accountName: String, creds: SipManager.SipCredentials) {
        SipPreferences(context).save(accountName, creds)
        SessionStore.accountName = accountName.ifBlank { creds.username }
        SessionStore.username = creds.username
        SessionStore.domain = creds.domain
    }

    fun signOut(context: Context) {
        SipPreferences(context).clear()
        SessionStore.accountName = ""
        SessionStore.username = ""
        SessionStore.domain = ""
        context.stopService(Intent(context, SipCallService::class.java))
    }
}
