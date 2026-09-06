package com.example.sipcaller.data.repository

import android.content.Context
import com.example.sipcaller.SipManager

/** Central account persistence with an explicit enabled flag for service/boot recovery. */
class SipAccountRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun save(accountName: String, creds: SipManager.SipCredentials) = prefs.edit().putString("account_name", accountName).putString("username", creds.username).putString("password", creds.password).putString("domain", creds.domain).putInt("port", creds.port).putString("proxy", creds.proxy).putBoolean("enabled", true).apply()
    fun load(): SipManager.SipCredentials? { val u=prefs.getString("username",null)?:return null; val p=prefs.getString("password",null)?:return null; val d=prefs.getString("domain",null)?:return null; return SipManager.SipCredentials(u,p,d,prefs.getInt("port",5060),prefs.getString("proxy",null)) }
    fun accountName()=prefs.getString("account_name","")?:""
    fun isEnabled()=prefs.getBoolean("enabled", load()!=null)
    fun setEnabled(enabled:Boolean)=prefs.edit().putBoolean("enabled",enabled).apply()
    fun clear()=prefs.edit().clear().apply()
    companion object { private const val PREFS="sip_profile" }
}
