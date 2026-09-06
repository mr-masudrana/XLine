package com.example.sipcaller

import android.util.Log
import com.example.sipcaller.diagnostics.SipDiagnostics
import org.pjsip.pjsua2.*

/**
 * Owns the PJSUA2 endpoint lifecycle and the application UDP transport.
 * All public PJSIP operations are serialized through [SipThread].
 */
internal object SipEngine {
    private const val TAG = "SipEngine"

    @Volatile private var endpoint: Endpoint? = null
    @Volatile private var udpTransportId: Int = -1

    internal val nativeLock = Any()

    fun isReady() = endpoint != null && SipLifecycleGuard.isRunning()
    fun currentUdpTransportId(): Int = udpTransportId

    fun start(): Boolean {
        if (!SipLifecycleGuard.beginStart()) return endpoint != null
        SipThread.start()
        return call {
            if (endpoint != null) {
                SipLifecycleGuard.markRunning()
                return@call true
            }
            try {
                Endpoint().also { ep ->
                    ep.libCreate()
                    EpConfig().also { cfg ->
                        cfg.logConfig.level = 4
                        cfg.logConfig.consoleLevel = 4
                        cfg.uaConfig.maxCalls = 4
                        ep.libInit(cfg)
                    }

                    // Keep an explicit application-owned UDP transport and retain its id.
                    // The account layer binds to this transport instead of relying on an
                    // implicit/default transport selected by the native stack.
                    TransportConfig().also { tc ->
                        tc.port = 0L
                        udpTransportId = ep.transportCreate(
                            pjsip_transport_type_e.PJSIP_TRANSPORT_UDP,
                            tc
                        )
                    }

                    ep.libStart()
                    endpoint = ep
                }
                SipLifecycleGuard.markRunning()
                Log.i(TAG, "Endpoint started, UDP transport id=$udpTransportId")
                SipDiagnostics.info(TAG, "Endpoint started successfully; udpTransportId=$udpTransportId")
                true
            } catch (t: Throwable) {
                endpoint = null
                udpTransportId = -1
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
            }
        }
        SipThread.stop()
        SipLifecycleGuard.markStopped()
    }

    internal fun post(block: () -> Unit): Boolean =
        SipThread.post { synchronized(nativeLock) { block() } }

    internal fun <T> call(block: () -> T): T? =
        SipThread.call { synchronized(nativeLock) { block() } }
}
