package com.example.sipcaller

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SipContact(val name: String, val number: String)

object ContactStore {
    private const val PREFS = "sip_contacts"
    private const val KEY = "contacts"

    fun getAll(context: Context): MutableList<SipContact> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { i ->
                val item = array.getJSONObject(i)
                SipContact(item.optString("name"), item.optString("number"))
            }
        } catch (_: Exception) { mutableListOf() }
    }

    fun save(context: Context, contact: SipContact) {
        val list = getAll(context).filterNot { it.number == contact.number }.toMutableList()
        list.add(0, contact)
        persist(context, list)
    }

    fun delete(context: Context, number: String) {
        persist(context, getAll(context).filterNot { it.number == number })
    }

    fun findName(context: Context, numberOrUri: String): String? {
        val number = numberOrUri.substringBefore("@").removePrefix("sip:")
        return getAll(context).firstOrNull { it.number == number }?.name
    }

    private fun persist(context: Context, contacts: List<SipContact>) {
        val array = JSONArray()
        contacts.forEach { c -> array.put(JSONObject().put("name", c.name).put("number", c.number)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
