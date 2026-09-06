package com.example.sipcaller

import android.content.Context
import com.example.sipcaller.data.repository.SipAccountRepository

/** Backward-compatible facade over the account repository. */
class SipPreferences(context: Context) {
    private val repo = SipAccountRepository(context)
    fun save(accountName:String,creds:SipManager.SipCredentials)=repo.save(accountName,creds)
    fun load()=repo.load()
    fun accountName()=repo.accountName()
    fun clear()=repo.clear()
    fun isEnabled()=repo.isEnabled()
    fun setEnabled(enabled:Boolean)=repo.setEnabled(enabled)
}
