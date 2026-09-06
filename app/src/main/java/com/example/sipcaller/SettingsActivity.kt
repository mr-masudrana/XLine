package com.example.sipcaller

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_page)
        findViewById<TextView>(R.id.pageTitle).text = "Settings"
        findViewById<TextView>(R.id.pageBody).text =
            "Settings screen — audio device selection, DTMF mode, and NAT keep-alive " +
            "interval would go here. Not wired up yet in this build."
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
    }
}
