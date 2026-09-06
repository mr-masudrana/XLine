package com.example.sipcaller.util

import android.util.Log

object SafeExecution {
    inline fun <T> run(tag: String, fallback: T, block: () -> T): T =
        try { block() } catch (t: Throwable) {
            Log.e(tag, "Recoverable failure", t)
            fallback
        }

    inline fun run(tag: String, block: () -> Unit) {
        try { block() } catch (t: Throwable) { Log.e(tag, "Recoverable failure", t) }
    }
}
