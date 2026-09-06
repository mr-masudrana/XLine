package com.example.sipcaller.data.state

import android.content.Context

/** Minimal recoverable metadata; never stores native SipCall objects. */
class PersistentCallState(context: Context) {
    data class Snapshot(val remote:String, val incoming:Boolean, val startedAt:Long, val connected:Boolean)
    private val prefs=context.applicationContext.getSharedPreferences("sip_call_state", Context.MODE_PRIVATE)
    fun begin(remote:String,incoming:Boolean)=prefs.edit().putString("remote",remote).putBoolean("incoming",incoming).putLong("started",System.currentTimeMillis()).putBoolean("connected",false).commit()
    fun markConnected()=prefs.edit().putBoolean("connected",true).apply()
    fun snapshot():Snapshot? { val remote=prefs.getString("remote",null)?:return null; return Snapshot(remote,prefs.getBoolean("incoming",false),prefs.getLong("started",0L),prefs.getBoolean("connected",false)) }
    fun clear()=prefs.edit().clear().apply()
}
