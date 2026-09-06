package com.example.sipcaller

import android.content.Context
import android.provider.ContactsContract
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

    fun getPhoneContacts(context: Context): List<SipContact> {
        val result = LinkedHashMap<String, SipContact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)?.trim().orEmpty()
                    val number = cursor.getString(numberIndex)?.trim().orEmpty()
                    if (name.isNotBlank() && number.isNotBlank()) {
                        val key = normalize(number)
                        if (key.isNotBlank() && !result.containsKey(key)) {
                            result[key] = SipContact(name, number)
                        }
                    }
                }
            }
        } catch (_: SecurityException) { }
        return result.values.toList()
    }

    fun save(context: Context, contact: SipContact) {
        val key = normalize(contact.number)
        val list = getAll(context).filterNot { normalize(it.number) == key }.toMutableList()
        list.add(0, contact)
        persist(context, list)
    }

    fun delete(context: Context, number: String) {
        val key = normalize(number)
        persist(context, getAll(context).filterNot { normalize(it.number) == key })
    }

    fun findName(context: Context, numberOrUri: String): String? {
        val number = numberOrUri.substringBefore("@").removePrefix("sip:")
        val key = normalize(number)
        getAll(context).firstOrNull { normalize(it.number) == key }?.let { return it.name }
        return getPhoneContacts(context).firstOrNull { normalize(it.number) == key }?.name
    }

    fun normalize(number: String): String = number
        .substringBefore("@")
        .removePrefix("sip:")
        .replace(Regex("[^0-9+*#]"), "")

    private fun persist(context: Context, contacts: List<SipContact>) {
        val array = JSONArray()
        contacts.forEach { c -> array.put(JSONObject().put("name", c.name).put("number", c.number)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
