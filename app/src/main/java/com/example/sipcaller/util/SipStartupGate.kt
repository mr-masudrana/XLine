package com.example.sipcaller.util

import java.util.concurrent.atomic.AtomicBoolean

/** Prevents duplicate startup work from process/activity/service entry points. */
object SipStartupGate {
    private val starting = AtomicBoolean(false)
    fun tryEnter(): Boolean = starting.compareAndSet(false, true)
    fun leave() { starting.set(false) }
}
