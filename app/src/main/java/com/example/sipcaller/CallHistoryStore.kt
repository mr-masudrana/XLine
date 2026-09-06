package com.example.sipcaller

import android.content.Context
import com.example.sipcaller.data.repository.CallHistoryRepository

data class CallHistoryItem(
    val id: String = "",
    val number: String,
    val direction: String,
    val result: String,
    val timestamp: Long,
    val durationSeconds: Long = 0L
)

/** Backward-compatible facade. New code should use CallHistoryRepository. */
object CallHistoryStore {
    fun getAll(context: Context) = CallHistoryRepository(context).getAll()
    fun add(context: Context, number: String, direction: String, result: String, durationSeconds: Long = 0L) =
        CallHistoryRepository(context).add(number, direction, result, durationSeconds)
    fun delete(context: Context, item: CallHistoryItem) = CallHistoryRepository(context).delete(item)
    fun clear(context: Context) = CallHistoryRepository(context).clear()
}
