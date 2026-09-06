package com.example.sipcaller.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build

/** Owns call audio lifecycle and keeps routing state out of CallActivity. */
class SipAudioController(context: Context) {
    private val appContext = context.applicationContext
    private val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var started = false
    private var muted = false

    fun startCallAudio() {
        if (started) return
        started = true
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        requestFocus()
    }

    fun setMuted(value: Boolean): Boolean {
        muted = value
        audio.isMicrophoneMute = value
        return muted
    }

    fun isMuted(): Boolean = muted

    @Suppress("DEPRECATION")
    fun setSpeaker(enabled: Boolean) { audio.isSpeakerphoneOn = enabled }

    @Suppress("DEPRECATION")
    fun isSpeakerOn(): Boolean = audio.isSpeakerphoneOn

    fun stopCallAudio() {
        muted = false
        audio.isMicrophoneMute = false
        @Suppress("DEPRECATION") audio.isSpeakerphoneOn = false
        audio.mode = AudioManager.MODE_NORMAL
        abandonFocus()
        started = false
    }

    @Suppress("DEPRECATION")
    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // MODE_IN_COMMUNICATION is sufficient for API 23+ baseline; focus request remains compatible.
        }
        audio.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
    }

    @Suppress("DEPRECATION")
    private fun abandonFocus() {
        audio.abandonAudioFocus(null)
    }
}
