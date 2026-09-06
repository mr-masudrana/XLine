package com.example.sipcaller.diagnostics

data class SipDiagnosticsSnapshot(
    val registrationState: String = "Unknown",
    val networkState: String = "Unknown",
    val lastSipResponse: String = "-",
    val lastError: String = "-",
    val retryCount: Int = 0,
    val lastRegistrationAt: Long = 0L,
    val server: String = "-"
)

object SipDiagnosticsStore {
    @Volatile private var snapshot = SipDiagnosticsSnapshot()

    fun update(block: SipDiagnosticsSnapshot.() -> SipDiagnosticsSnapshot) {
        snapshot = snapshot.block()
    }

    fun current(): SipDiagnosticsSnapshot = snapshot
}
