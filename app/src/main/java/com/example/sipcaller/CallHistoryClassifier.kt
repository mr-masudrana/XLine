package com.example.sipcaller

/** Converts raw SIP call state into consistent call-history labels. */
object CallHistoryClassifier {
    data class Outcome(val direction: String, val result: String)

    fun classify(
        incoming: Boolean,
        wasConnected: Boolean,
        statusCode: Int,
        reason: String,
        locallyDeclined: Boolean = false
    ): Outcome {
        val direction = if (incoming) "Incoming" else "Outgoing"
        val result = when {
            wasConnected -> "Completed"
            incoming && locallyDeclined -> "Declined"
            incoming && (statusCode == 0 || statusCode == 408 || statusCode == 480 || statusCode == 487) -> "Missed"
            incoming -> "Missed"
            statusCode in 486..487 -> "Not answered"
            statusCode >= 300 -> "Failed ${statusCode}${reason.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}"
            else -> "Not answered"
        }
        return Outcome(direction, result)
    }
}
