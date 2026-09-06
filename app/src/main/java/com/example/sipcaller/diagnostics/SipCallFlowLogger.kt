package com.example.sipcaller.diagnostics

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** Structured per-call timeline built from PJSIP callbacks and app call actions. */
object SipCallFlowLogger {
    data class Event(val time: Long, val callId: Int, val event: String, val code: Int = 0, val reason: String = "", val remote: String = "")
    private const val MAX_PER_CALL = 100
    private val flows = ConcurrentHashMap<Int, CopyOnWriteArrayList<Event>>()

    fun record(callId: Int, event: String, code: Int = 0, reason: String = "", remote: String = "") {
        val list = flows.getOrPut(callId) { CopyOnWriteArrayList() }
        if (list.size >= MAX_PER_CALL) list.removeAt(0)
        list += Event(System.currentTimeMillis(), callId, event, code, reason, remote)
        val suffix = buildString {
            if (code > 0) append(" code=$code")
            if (reason.isNotBlank()) append(" reason=$reason")
            if (remote.isNotBlank()) append(" remote=$remote")
        }
        SipDiagnostics.info("SipCallFlow", "callId=$callId event=$event$suffix")
    }

    fun snapshot(callId: Int): List<Event> = flows[callId]?.toList().orEmpty()
    fun latestCallIds(): List<Int> = flows.keys.sorted()
    fun clear(callId: Int? = null) { if (callId == null) flows.clear() else flows.remove(callId) }
}
