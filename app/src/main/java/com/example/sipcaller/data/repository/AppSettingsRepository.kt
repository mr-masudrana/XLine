package com.example.sipcaller.data.repository

import android.content.Context

/** Centralized, backward-compatible application settings storage (API 23+). */
class AppSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("sipcaller_settings", Context.MODE_PRIVATE)

    var autoStart: Boolean
        get() = prefs.getBoolean("auto_start", true)
        set(value) = prefs.edit().putBoolean("auto_start", value).apply()

    var keepAliveEnabled: Boolean
        get() = prefs.getBoolean("keep_alive", true)
        set(value) = prefs.edit().putBoolean("keep_alive", value).apply()

    var keepAliveSeconds: Int
        get() = prefs.getInt("keep_alive_seconds", 30).coerceIn(10, 300)
        set(value) = prefs.edit().putInt("keep_alive_seconds", value.coerceIn(10, 300)).apply()

    var incomingVibration: Boolean
        get() = prefs.getBoolean("incoming_vibration", true)
        set(value) = prefs.edit().putBoolean("incoming_vibration", value).apply()

    var speakerByDefault: Boolean
        get() = prefs.getBoolean("speaker_default", false)
        set(value) = prefs.edit().putBoolean("speaker_default", value).apply()

    var autoReconnect: Boolean
        get() = prefs.getBoolean("auto_reconnect", true)
        set(value) = prefs.edit().putBoolean("auto_reconnect", value).apply()
}
