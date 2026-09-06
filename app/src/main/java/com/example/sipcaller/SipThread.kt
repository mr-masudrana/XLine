package com.example.sipcaller

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Single application-owned thread for all app initiated pjsua2 operations.
 * PJSIP callbacks still arrive on native worker threads, therefore SipManager.nativeLock
 * must also be held while touching native objects from callbacks.
 */
internal object SipThread {
    private const val TAG = "SipThread"
    private const val WAIT_SECONDS = 10L

    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private val lifecycleLock = Any()

    fun start() = synchronized(lifecycleLock) {
        if (thread?.isAlive == true && handler != null) return
        HandlerThread("SipCaller-PJSIP").also {
            it.start()
            thread = it
            handler = Handler(it.looper)
            Log.i(TAG, "PJSIP app thread started")
        }
    }

    fun isCurrentThread(): Boolean = Thread.currentThread() === thread

    fun post(block: () -> Unit): Boolean {
        val h = synchronized(lifecycleLock) { handler } ?: return false
        return h.post(block)
    }

    fun <T> call(block: () -> T): T? {
        if (isCurrentThread()) return runCatching(block).getOrNull()
        val h = synchronized(lifecycleLock) { handler } ?: return null
        val latch = CountDownLatch(1)
        var result: T? = null
        var error: Throwable? = null
        h.post {
            try { result = block() } catch (t: Throwable) { error = t } finally { latch.countDown() }
        }
        if (!latch.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
            Log.e(TAG, "Timed out waiting for PJSIP operation")
            return null
        }
        error?.let { Log.e(TAG, "PJSIP operation failed", it) }
        return result
    }

    fun stop() = synchronized(lifecycleLock) {
        handler?.removeCallbacksAndMessages(null)
        thread?.quitSafely()
        handler = null
        thread = null
    }
}
