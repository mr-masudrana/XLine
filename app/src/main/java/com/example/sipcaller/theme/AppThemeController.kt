package com.example.sipcaller.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object AppThemeController {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
    private const val PREF = "app_theme"

    fun apply(context: Context) {
        when (context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("mode", SYSTEM)) {
            LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    fun mode(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("mode", SYSTEM) ?: SYSTEM
    fun set(context: Context, mode: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("mode", mode).apply()
        apply(context)
    }
}
