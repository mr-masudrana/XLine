package com.example.sipcaller.diagnostics

import java.util.concurrent.CopyOnWriteArraySet

/**
 * Central source of truth for SIP registration diagnostics.
 * It intentionally keeps the legacy boolean registration API intact while exposing
 * a richer state model for diagnostics and future UI.
 */
object SipRegistrationMonitor {
    enum class State {
        DISCONNECTED,
        INITIALIZING,
        REGISTERING,
        REGISTERED,
        RECONNECTING,
        FAILED
    }

    data class Snapshot(
        val state: State,
        val lastResponseCode: Int,
        val lastResponseMessage: String,
        val updatedAt: Long,
        val registeredAt: Long?,
        val lastSuccessfulRegistrationAt: Long?,
        val retryCount: Int,
        val server: String?,
        val transport: String?,
        val networkAvailable: Boolean?
    )

    interface Listener { fun onRegistrationSnapshotChanged(snapshot: Snapshot) }

    private val listeners = CopyOnWriteArraySet<Listener>()
    private val lock = Any()

    @Volatile private var state = State.DISCONNECTED
    @Volatile private var lastResponseCode = 0
    @Volatile private var lastResponseMessage = "Not registered"
    @Volatile private var updatedAt = System.currentTimeMillis()
    @Volatile private var registeredAt: Long? = null
    @Volatile private var lastSuccessfulRegistrationAt: Long? = null
    @Volatile private var retryCount = 0
    @Volatile private var server: String? = null
    @Volatile private var transport: String? = "UDP"
    @Volatile private var networkAvailable: Boolean? = null

    fun snapshot() = Snapshot(
        state, lastResponseCode, lastResponseMessage, updatedAt, registeredAt,
        lastSuccessfulRegistrationAt, retryCount, server, transport, networkAvailable
    )

    fun addListener(listener: Listener) { listeners.add(listener); listener.onRegistrationSnapshotChanged(snapshot()) }
    fun removeListener(listener: Listener) { listeners.remove(listener) }

    fun initializing(serverAddress: String? = null) = update(State.INITIALIZING, 0, "Initializing SIP account", serverAddress = serverAddress)
    fun registering(serverAddress: String? = null) = update(State.REGISTERING, 0, "Sending REGISTER", serverAddress = serverAddress)
    fun reconnecting(reason: String = "Reconnecting") = update(State.RECONNECTING, lastResponseCode, reason, incrementRetry = true)
    fun registered(code: Int = 200, message: String = "OK") = update(State.REGISTERED, code, message, resetRetry = true, markRegistered = true)
    fun failed(code: Int, message: String) = update(State.FAILED, code, message, incrementRetry = true)
    fun disconnected(message: String = "Disconnected") = update(State.DISCONNECTED, 0, message, clearRegistered = true)
    fun networkChanged(available: Boolean) {
        networkAvailable = available
        updatedAt = System.currentTimeMillis()
        publish()
    }

    private fun update(
        newState: State,
        code: Int,
        message: String,
        serverAddress: String? = null,
        incrementRetry: Boolean = false,
        resetRetry: Boolean = false,
        markRegistered: Boolean = false,
        clearRegistered: Boolean = false
    ) = synchronized(lock) {
        state = newState
        lastResponseCode = code
        lastResponseMessage = message.ifBlank { newState.name }
        updatedAt = System.currentTimeMillis()
        if (serverAddress != null) server = serverAddress
        if (incrementRetry) retryCount++
        if (resetRetry) retryCount = 0
        if (markRegistered) {
            val now = System.currentTimeMillis()
            if (registeredAt == null) registeredAt = now
            lastSuccessfulRegistrationAt = now
        }
        if (clearRegistered) registeredAt = null
        SipDiagnostics.info("SipRegistration", "state=$state code=$lastResponseCode message=$lastResponseMessage retry=$retryCount server=${server ?: "unknown"}")
        publish()
    }

    private fun publish() {
        val value = snapshot()
        listeners.forEach { it.onRegistrationSnapshotChanged(value) }
    }
}
