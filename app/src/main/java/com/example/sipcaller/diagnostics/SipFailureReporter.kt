package com.example.sipcaller.diagnostics

import java.util.concurrent.CopyOnWriteArrayList

object SipFailureReporter {
    data class Failure(
        val title: String,
        val message: String,
        val action: String,
        val retryRecommended: Boolean,
        val technical: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val history = CopyOnWriteArrayList<Failure>()
    @Volatile private var latest: Failure? = null

    fun reportSip(code: Int, reason: String = ""): Failure {
        val a = SipErrorAnalyzer.analyze(code, reason)
        return report(Failure(a.title, a.userMessage,
            if (a.retryRecommended) "Check connection and try again" else "Review account or destination",
            a.retryRecommended, a.technicalHint))
    }

    fun reportException(context: String, error: Throwable): Failure =
        report(Failure("Operation failed", "$context could not be completed safely",
            "Try again. If the problem continues, open SIP Diagnostics.", true,
            "${error.javaClass.simpleName}: ${error.message.orEmpty()}"))

    fun report(failure: Failure): Failure {
        latest = failure
        history.add(failure)
        while (history.size > 50) history.removeAt(0)
        SipDiagnostics.info("FailureReporter", "${failure.title}: ${failure.message} | ${failure.technical}")
        return failure
    }

    fun latest(): Failure? = latest
    fun history(): List<Failure> = history.toList()
}
