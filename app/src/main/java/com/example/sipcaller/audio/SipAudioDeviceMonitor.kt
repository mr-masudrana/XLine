package com.example.sipcaller.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * Observes headset/Bluetooth devices during a call and exposes route changes.
 * Registration is lifecycle-bound to the active call UI.
 */
class SipAudioDeviceMonitor(context: Context, private val onChanged: (Set<SipAudioRouter.Route>) -> Unit) {
    private val audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val router = SipAudioRouter(context)
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) = publish()
        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = publish()
    }
    fun start() {
        if (running) return
        running = true
        audio.registerAudioDeviceCallback(callback, handler)
        publish()
    }
    fun stop() {
        if (!running) return
        running = false
        audio.unregisterAudioDeviceCallback(callback)
    }
    private fun publish() { handler.post { onChanged(router.availableRoutes()) } }
}
