package com.example.sipcaller.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/** Owns voice-call audio mode, focus and microphone state. */
class SipAudioController(context: Context) {
    private val audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var started = false
    private var muted = false
    private var focusRequest: AudioFocusRequest? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Preserve user mute choice; only protect privacy on focus loss.
                if (!muted) audio.isMicrophoneMute = true
            }
            AudioManager.AUDIOFOCUS_GAIN -> audio.isMicrophoneMute = muted
        }
    }

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

    fun isMuted() = muted

    @Suppress("DEPRECATION")
    fun setSpeaker(enabled: Boolean) { audio.isSpeakerphoneOn = enabled }

    @Suppress("DEPRECATION")
    fun isSpeakerOn() = audio.isSpeakerphoneOn

    fun stopCallAudio() {
        muted = false
        audio.isMicrophoneMute = false
        @Suppress("DEPRECATION") {
            audio.isSpeakerphoneOn = false
            audio.stopBluetoothSco()
            audio.isBluetoothScoOn = false
        }
        abandonFocus()
        audio.mode = AudioManager.MODE_NORMAL
        started = false
    }

    @Suppress("DEPRECATION")
    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            audio.requestAudioFocus(focusRequest!!)
        } else audio.requestAudioFocus(focusListener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
    }

    @Suppress("DEPRECATION")
    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) focusRequest?.let { audio.abandonAudioFocusRequest(it) }
        else audio.abandonAudioFocus(focusListener)
        focusRequest = null
    }
}
