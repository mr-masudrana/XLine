package com.example.sipcaller.diagnostics

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/** Lightweight in-memory diagnostics ring buffer for SIP troubleshooting. */
object SipDiagnostics {
    private const val TAG = "SipDiagnostics"
    private const val MAX = 500
    data class Entry(val time: Long, val level: String, val source: String, val message: String)
    private val entries = CopyOnWriteArrayList<Entry>()
    private fun add(level: String, source: String, message: String, error: Throwable? = null) {
        if (entries.size >= MAX) entries.removeAt(0)
        entries += Entry(System.currentTimeMillis(), level, source, message + (error?.let { ": ${it.message}" } ?: ""))
        when (level) { "E" -> Log.e(TAG, "[$source] $message", error); "W" -> Log.w(TAG, "[$source] $message", error); else -> Log.i(TAG, "[$source] $message") }
    }
    fun info(source: String, message: String) = add("I", source, message)
    fun warn(source: String, message: String, error: Throwable? = null) = add("W", source, message, error)
    fun error(source: String, message: String, error: Throwable? = null) = add("E", source, message, error)
    fun snapshot(): List<Entry> = entries.toList()
    fun exportText(): String {
        val f = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return entries.joinToString("\n") { "${f.format(Date(it.time))} ${it.level}/${it.source}: ${it.message}" }
    }
    fun clear() = entries.clear()
}
