package com.example.sipcaller.diagnostics

import android.content.Context
import android.media.AudioManager
import com.example.sipcaller.audio.SipAudioRouter

object SipAudioDiagnostics {
    fun report(context: Context): String {
        val audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val router = SipAudioRouter(context)
        return buildString {
            appendLine("Audio mode: ${audio.mode}")
            appendLine("Microphone muted: ${audio.isMicrophoneMute}")
            appendLine("Current route: ${router.currentRoute()}")
            appendLine("Available routes: ${router.availableRoutes().joinToString()}")
        }
    }
}
