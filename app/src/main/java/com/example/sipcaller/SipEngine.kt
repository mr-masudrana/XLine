package com.example.sipcaller

import android.util.Log
import com.example.sipcaller.diagnostics.SipDiagnostics
import org.pjsip.pjsua2.*

/**
 * Owns the PJSUA2 endpoint lifecycle and all application-side native calls.
 * Uses the same PJSIP 2.5 Java/native runtime as the working IPDial project.
 */
internal object SipEngine {
    private const val TAG = "SipEngine"

    @Volatile private var endpoint: Endpoint? = null
    @Volatile private var udpTransportId: Int = -1
    private val registeredThreads = java.util.Collections.synchronizedSet(mutableSetOf<Long>())

    internal val nativeLock = Any()

    fun isReady() = endpoint != null && SipLifecycleGuard.isRunning()
    fun currentUdpTransportId(): Int = udpTransportId

    /** Register the dedicated app thread with PJSIP before app-initiated native calls. */
    private fun registerCurrentThreadIfNeeded() {
        val ep = endpoint ?: return
        val id = Thread.currentThread().id
        if (registeredThreads.contains(id)) return
        try {
            if (!ep.libIsThreadRegistered()) {
                ep.libRegisterThread(Thread.currentThread().name ?: "SipCaller-PJSIP")
            }
            registeredThreads.add(id)
            Log.i(TAG, "PJSIP app thread registered: ${Thread.currentThread().name}")
        } catch (t: Throwable) {
            // Some wrappers report an error when already registered. Do not repeatedly retry.
            registeredThreads.add(id)
            Log.w(TAG, "PJSIP thread registration: ${t.message}")
        }
    }

    fun start(): Boolean {
        if (!SipLifecycleGuard.beginStart()) return endpoint != null
        SipThread.start()
        return call {
            if (endpoint != null) {
                SipLifecycleGuard.markRunning()
                return@call true
            }
            try {
                // Load the exact native runtime bundled from the working IPDial project.
                try { System.loadLibrary("pjsua2") } catch (_: UnsatisfiedLinkError) { }

                val ep = Endpoint()
                ep.libCreate()
                val cfg = EpConfig().apply {
                    logConfig.level = 4
                    logConfig.consoleLevel = 4
                    uaConfig.maxCalls = 4
                    uaConfig.userAgent = "SipCaller/1.0 (Android)"
                    try { uaConfig.stunServer.add("stun.l.google.com:19302") } catch (_: Throwable) { }
                }
                ep.libInit(cfg)

                val tc = TransportConfig().apply { port = 0L }
                udpTransportId = ep.transportCreate(
                    pjsip_transport_type_e.PJSIP_TRANSPORT_UDP,
                    tc
                )

                ep.libStart()
                endpoint = ep
                registeredThreads.clear()
                registerCurrentThreadIfNeeded()

                SipLifecycleGuard.markRunning()
                Log.i(TAG, "Endpoint started, UDP transport id=$udpTransportId")
                SipDiagnostics.info(TAG, "Endpoint started with IPDial-compatible PJSIP 2.5 runtime; udpTransportId=$udpTransportId")
                true
            } catch (t: Throwable) {
                endpoint = null
                udpTransportId = -1
                registeredThreads.clear()
                SipLifecycleGuard.markFailed()
                Log.e(TAG, "Endpoint start failed", t)
                SipDiagnostics.error(TAG, "Endpoint start failed", t)
                false
            }
        } ?: false
    }

    fun stop() {
        if (!SipLifecycleGuard.beginStop()) return
        call {
            try {
                endpoint?.libDestroy()
                endpoint?.delete()
            } catch (t: Throwable) {
                Log.e(TAG, "Endpoint stop failed", t)
                SipDiagnostics.error(TAG, "Endpoint stop failed", t)
            } finally {
                endpoint = null
                udpTransportId = -1
                registeredThreads.clear()
            }
        }
        SipThread.stop()
        SipLifecycleGuard.markStopped()
    }

    internal fun post(block: () -> Unit): Boolean =
        SipThread.post {
            synchronized(nativeLock) {
                registerCurrentThreadIfNeeded()
                block()
            }
        }

    internal fun <T> call(block: () -> T): T? =
        SipThread.call {
            synchronized(nativeLock) {
                registerCurrentThreadIfNeeded()
                block()
            }
        }
}
