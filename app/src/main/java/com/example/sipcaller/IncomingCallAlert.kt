package com.example.sipcaller

import android.content.*
import android.media.RingtoneManager
import android.os.Vibrator

/** Owns ringtone/vibration lifecycle independently of an Activity. */
class IncomingCallAlert(context: Context) {
    private val appContext = context.applicationContext
    private var ringtone: android.media.Ringtone? = null
    private val vibrator get() = appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun start() {
        stop()
        ringtone = RingtoneManager.getRingtone(appContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))?.apply { play() }
        @Suppress("DEPRECATION")
        vibrator.vibrate(longArrayOf(0, 700, 500), 0)
    }
    fun stop() {
        ringtone?.stop(); ringtone = null
        vibrator.cancel()
    }
}
