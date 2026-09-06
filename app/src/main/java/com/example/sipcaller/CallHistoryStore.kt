package com.example.sipcaller

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CallHistoryItem(
    val number: String,
    val direction: String,
    val result: String,
    val timestamp: Long
)

object CallHistoryStore {
    private const val PREFS = "sip_call_history"
    private const val KEY = "history"
    private const val MAX_ITEMS = 200

    fun getAll(context: Context): MutableList<CallHistoryItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { i ->
                val o = array.getJSONObject(i)
                CallHistoryItem(o.optString("number"), o.optString("direction"), o.optString("result"), o.optLong("timestamp"))
            }
        } catch (_: Exception) { mutableListOf() }
    }

    fun add(context: Context, number: String, direction: String, result: String) {
        val list = getAll(context)
        list.add(0, CallHistoryItem(number.removePrefix("sip:"), direction, result, System.currentTimeMillis()))
        persist(context, list.take(MAX_ITEMS))
    }

    fun clear(context: Context) = persist(context, emptyList())

    private fun persist(context: Context, list: List<CallHistoryItem>) {
        val array = JSONArray()
        list.forEach { item ->
            array.put(JSONObject()
                .put("number", item.number)
                .put("direction", item.direction)
                .put("result", item.result)
                .put("timestamp", item.timestamp))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
