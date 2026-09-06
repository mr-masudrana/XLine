package com.example.sipcaller.data.repository

import android.content.Context
import com.example.sipcaller.CallHistoryItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Single persistence boundary for call logs.
 * Adds stable IDs, normalized numbers and short-window duplicate suppression.
 */
class CallHistoryRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized fun getAll(): MutableList<CallHistoryItem> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { i ->
                val o = array.getJSONObject(i)
                CallHistoryItem(
                    id = o.optString("id").ifBlank { legacyId(o, i) },
                    number = o.optString("number"),
                    direction = o.optString("direction"),
                    result = o.optString("result"),
                    timestamp = o.optLong("timestamp"),
                    durationSeconds = o.optLong("durationSeconds", 0L)
                )
            }.sortedByDescending { it.timestamp }.toMutableList()
        } catch (_: Exception) { mutableListOf() }
    }

    @Synchronized fun add(number: String, direction: String, result: String, durationSeconds: Long = 0L): Boolean {
        val normalized = normalizeNumber(number)
        if (normalized.isBlank()) return false
        val now = System.currentTimeMillis()
        val list = getAll()
        // Prevent duplicate callbacks from creating two identical history rows.
        val duplicate = list.any {
            it.number == normalized && it.direction == direction && it.result == result &&
                kotlin.math.abs(it.timestamp - now) <= DUPLICATE_WINDOW_MS
        }
        if (duplicate) return false
        list.add(CallHistoryItem(UUID.randomUUID().toString(), normalized, direction, result, now, durationSeconds.coerceAtLeast(0L)))
        persist(list.sortedByDescending { it.timestamp }.take(MAX_ITEMS))
        return true
    }

    @Synchronized fun delete(item: CallHistoryItem) {
        val id = item.id
        persist(getAll().filterNot {
            if (id.isNotBlank()) it.id == id
            else it.timestamp == item.timestamp && it.number == item.number && it.direction == item.direction
        })
    }

    @Synchronized fun clear() = persist(emptyList())

    private fun persist(list: List<CallHistoryItem>) {
        val array = JSONArray()
        list.forEach {
            array.put(JSONObject()
                .put("id", it.id)
                .put("number", it.number)
                .put("direction", it.direction)
                .put("result", it.result)
                .put("timestamp", it.timestamp)
                .put("durationSeconds", it.durationSeconds))
        }
        prefs.edit().putString(KEY, array.toString()).commit()
    }

    private fun normalizeNumber(value: String): String = value.trim().substringBefore("@").removePrefix("sip:").trim()
    private fun legacyId(o: JSONObject, index: Int): String = "legacy-${o.optLong("timestamp")}-${o.optString("number")}-${index}"

    companion object {
        private const val PREFS = "sip_call_history"
        private const val KEY = "history"
        private const val MAX_ITEMS = 500
        private const val DUPLICATE_WINDOW_MS = 5000L
    }
}
