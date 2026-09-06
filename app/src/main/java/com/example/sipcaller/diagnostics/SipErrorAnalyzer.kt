package com.example.sipcaller.diagnostics

/** Converts SIP response codes into stable, user-friendly diagnostic categories. */
object SipErrorAnalyzer {
    enum class Category {
        NONE,
        NETWORK,
        AUTHENTICATION,
        REMOTE_USER,
        SERVER,
        LOCAL_CALL,
        UNKNOWN
    }

    data class Analysis(
        val code: Int,
        val reason: String,
        val category: Category,
        val title: String,
        val userMessage: String,
        val technicalHint: String,
        val retryRecommended: Boolean
    )

    fun analyze(code: Int, reason: String = ""): Analysis {
        val normalizedReason = reason.ifBlank { defaultReason(code) }
        return when (code) {
            in 200..299 -> Analysis(code, normalizedReason, Category.NONE, "Call completed", "Call completed successfully", "Successful SIP response", false)
            401, 407 -> Analysis(code, normalizedReason, Category.AUTHENTICATION, "Authentication required", "Account authentication is required", "Verify SIP username, password and authentication challenge handling", true)
            403 -> Analysis(code, normalizedReason, Category.AUTHENTICATION, "Access denied", "The SIP server rejected this account", "Check account credentials, permissions or provider policy", false)
            404 -> Analysis(code, normalizedReason, Category.REMOTE_USER, "User not found", "The number or SIP destination was not found", "Verify destination number and server dial plan", false)
            408 -> Analysis(code, normalizedReason, Category.NETWORK, "Request timeout", "The server did not respond in time", "Check internet connectivity, NAT/firewall and SIP server reachability", true)
            480 -> Analysis(code, normalizedReason, Category.REMOTE_USER, "Temporarily unavailable", "The destination is temporarily unavailable", "Usually returned by remote endpoint, registration/contact routing or provider routing", true)
            486 -> Analysis(code, normalizedReason, Category.REMOTE_USER, "User busy", "The destination is currently busy", "Remote endpoint returned Busy Here", true)
            487 -> Analysis(code, normalizedReason, Category.LOCAL_CALL, "Call terminated", "The call was cancelled before completion", "INVITE transaction was terminated or cancelled", false)
            500, 501, 502 -> Analysis(code, normalizedReason, Category.SERVER, "Server error", "The SIP server reported an error", "Inspect provider/server logs and SIP routing", true)
            503 -> Analysis(code, normalizedReason, Category.SERVER, "Service unavailable", "The SIP service is temporarily unavailable", "Provider/server may be overloaded or unavailable", true)
            600, 603 -> Analysis(code, normalizedReason, Category.REMOTE_USER, "Call declined", "The destination or server declined the call", "Remote endpoint, dial plan or provider policy rejected the INVITE", false)
            in 400..499 -> Analysis(code, normalizedReason, Category.UNKNOWN, "Call request failed", "The SIP request was rejected", "Inspect exact SIP response and transaction logs", false)
            in 500..599 -> Analysis(code, normalizedReason, Category.SERVER, "Server failure", "The SIP server could not complete the request", "Inspect server health and provider logs", true)
            in 600..699 -> Analysis(code, normalizedReason, Category.REMOTE_USER, "Global call failure", "The destination rejected the call", "A global SIP failure response was returned", false)
            else -> Analysis(code, normalizedReason, Category.UNKNOWN, "Unknown call result", "The call ended with an unknown SIP result", "Inspect SIP call-flow diagnostics", false)
        }
    }

    fun log(code: Int, reason: String, callId: Int, remote: String) : Analysis {
        val result = analyze(code, reason)
        SipDiagnostics.info(
            "SipErrorAnalyzer",
            "callId=$callId code=${result.code} category=${result.category} title=${result.title} retry=${result.retryRecommended} remote=$remote hint=${result.technicalHint}"
        )
        return result
    }

    private fun defaultReason(code: Int): String = when (code) {
        480 -> "Temporarily Unavailable"
        486 -> "Busy Here"
        603 -> "Decline"
        408 -> "Request Timeout"
        503 -> "Service Unavailable"
        else -> ""
    }
}
