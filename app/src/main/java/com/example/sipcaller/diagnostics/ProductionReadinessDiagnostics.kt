package com.example.sipcaller.diagnostics

import java.util.concurrent.atomic.AtomicInteger

object ProductionReadinessDiagnostics {
    private val listenerFailures = AtomicInteger(0)
    private val recoveryAttempts = AtomicInteger(0)

    fun listenerFailure() { listenerFailures.incrementAndGet() }
    fun recoveryAttempt() { recoveryAttempts.incrementAndGet() }

    fun report(): String = buildString {
        appendLine("Listener failures: ${listenerFailures.get()}")
        appendLine("Recovery attempts: ${recoveryAttempts.get()}")
        appendLine("SIP engine diagnostics are available through the Diagnostics screen")
        appendLine("No unbounded in-memory event queue is used by this layer")
    }
}
