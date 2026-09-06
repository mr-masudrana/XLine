package com.ipdial.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SipLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(tag: String, message: String) {
        val timestamp = timeFormat.format(Date())
        val logLine = "[$timestamp] [$tag] $message"
        // Keep last 500 lines of logs
        val current = _logs.value
        _logs.value = if (current.size > 500) {
            current.drop(1) + logLine
        } else {
            current + logLine
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
