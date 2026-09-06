package com.example.sipcaller.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/** Voice-call route manager with API 31 communication-device support and legacy fallback. */
class SipAudioRouter(context: Context) {
    enum class Route { EARPIECE, SPEAKER, WIRED, BLUETOOTH }
    private val audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun availableRoutes(): Set<Route> {
        val routes = linkedSetOf(Route.EARPIECE, Route.SPEAKER)
        audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).forEach {
            when (it.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET -> routes += Route.WIRED
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> routes += Route.BLUETOOTH
            }
        }
        return routes
    }

    @Suppress("DEPRECATION")
    fun currentRoute(): Route {
        if (Build.VERSION.SDK_INT >= 31) {
            return when (audio.communicationDevice?.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET -> Route.BLUETOOTH
                AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET -> Route.WIRED
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> Route.SPEAKER
                else -> Route.EARPIECE
            }
        }
        return when {
            audio.isBluetoothScoOn -> Route.BLUETOOTH
            audio.isWiredHeadsetOn -> Route.WIRED
            audio.isSpeakerphoneOn -> Route.SPEAKER
            else -> Route.EARPIECE
        }
    }

    @Suppress("DEPRECATION")
    fun select(route: Route): Boolean {
        if (!availableRoutes().contains(route)) return false
        if (Build.VERSION.SDK_INT >= 31) {
            val type = when (route) {
                Route.EARPIECE -> AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                Route.SPEAKER -> AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                Route.WIRED -> null
                Route.BLUETOOTH -> AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
            val device = if (type == null) audio.availableCommunicationDevices.firstOrNull {
                it.type in setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET)
            } else audio.availableCommunicationDevices.firstOrNull { it.type == type }
            if (device != null) return audio.setCommunicationDevice(device)
        }
        when (route) {
            Route.SPEAKER -> audio.isSpeakerphoneOn = true
            Route.EARPIECE -> { audio.stopBluetoothSco(); audio.isBluetoothScoOn = false; audio.isSpeakerphoneOn = false }
            Route.WIRED -> audio.isSpeakerphoneOn = false
            Route.BLUETOOTH -> { audio.isSpeakerphoneOn = false; audio.startBluetoothSco(); audio.isBluetoothScoOn = true }
        }
        return true
    }

    fun clearCommunicationRoute() {
        if (Build.VERSION.SDK_INT >= 31) audio.clearCommunicationDevice()
    }
}
