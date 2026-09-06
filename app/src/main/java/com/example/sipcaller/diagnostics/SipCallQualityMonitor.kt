package com.example.sipcaller.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.sipcaller.SipCall

object SipCallQualityMonitor {
    enum class Grade { EXCELLENT, GOOD, FAIR, POOR }

    data class Snapshot(
        val grade: Grade,
        val network: String,
        val latencyHint: String,
        val packetLossHint: String,
        val jitterHint: String,
        val codecHint: String,
        val audioRoute: String
    )

    fun snapshot(context: Context, call: SipCall): Snapshot {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val network = when {
            caps == null -> "No network"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val grade = when {
            !validated -> Grade.POOR
            call.lastStatusCode >= 400 -> Grade.FAIR
            else -> Grade.GOOD
        }
        return Snapshot(
            grade, network,
            if (validated) "Network validated" else "Network unavailable",
            "Native RTP statistics not exposed by current binding",
            "Native RTP statistics not exposed by current binding",
            "Negotiated codec: inspect PJSIP media diagnostics",
            "Use active audio route diagnostics"
        )
    }

    fun report(context: Context, call: SipCall): String {
        val s = snapshot(context, call)
        return """Call quality: ${s.grade}
Network: ${s.network}
Latency: ${s.latencyHint}
Packet loss: ${s.packetLossHint}
Jitter: ${s.jitterHint}
Codec: ${s.codecHint}
Audio: ${s.audioRoute}
SIP state: ${call.lastState}
SIP status: ${call.lastStatusCode} ${call.lastReason}"""
    }
}
