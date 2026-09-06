package com.example.sipcaller

import android.util.Log
import java.util.concurrent.atomic.AtomicReference

/**
 * Small process-wide lifecycle state machine. It prevents duplicate init/shutdown
 * sequences and gives the service a single source of truth for core ownership.
 */
internal object SipLifecycleGuard {
    private const val TAG = "SipLifecycleGuard"
    enum class State { STOPPED, STARTING, RUNNING, STOPPING, FAILED }
    private val state = AtomicReference(State.STOPPED)

    fun state(): State = state.get()
    fun isRunning(): Boolean = state.get() == State.RUNNING

    fun beginStart(): Boolean = when (state.get()) {
        State.STOPPED, State.FAILED -> state.compareAndSet(state.get(), State.STARTING)
        State.RUNNING, State.STARTING -> false
        State.STOPPING -> false
    }

    fun markRunning() { state.set(State.RUNNING) }
    fun markFailed() { state.set(State.FAILED) }

    fun beginStop(): Boolean = state.compareAndSet(State.RUNNING, State.STOPPING) ||
        state.compareAndSet(State.FAILED, State.STOPPING) ||
        state.compareAndSet(State.STARTING, State.STOPPING)

    fun markStopped() {
        state.set(State.STOPPED)
        Log.i(TAG, "SIP lifecycle stopped")
    }
}
