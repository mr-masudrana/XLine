package com.example.sipcaller.audio

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build

/** Detects available output routes and applies a conservative default route. */
class SipAudioRouter(context: Context) {
    enum class Route { EARPIECE, SPEAKER, WIRED, BLUETOOTH }
    private val appContext = context.applicationContext
    private val audio = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Suppress("DEPRECATION")
    fun currentRoute(): Route = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        } && audio.isBluetoothScoOn -> Route.BLUETOOTH
        audio.isWiredHeadsetOn -> Route.WIRED
        audio.isSpeakerphoneOn -> Route.SPEAKER
        else -> Route.EARPIECE
    }

    @Suppress("DEPRECATION")
    fun select(route: Route) {
        when (route) {
            Route.SPEAKER -> audio.isSpeakerphoneOn = true
            Route.EARPIECE -> { audio.stopBluetoothSco(); audio.isBluetoothScoOn = false; audio.isSpeakerphoneOn = false }
            Route.WIRED -> audio.isSpeakerphoneOn = false
            Route.BLUETOOTH -> { audio.isSpeakerphoneOn = false; audio.startBluetoothSco(); audio.isBluetoothScoOn = true }
        }
    }

    @Suppress("DEPRECATION")
    fun hasWiredHeadset(): Boolean = audio.isWiredHeadsetOn
}
