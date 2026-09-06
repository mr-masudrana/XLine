package com.example.sipcaller

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MyAccountActivity : AppCompatActivity() {
    private lateinit var body: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_page)
        findViewById<TextView>(R.id.pageTitle).text = "My Account"
        body = findViewById(R.id.pageBody)
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val prefs = SipPreferences(this)
        val creds = prefs.load()
        body.text = if (creds == null) "No SIP account configured." else buildString {
            append("Account name: ${prefs.accountName().ifBlank { creds.username }}\n\n")
            append("SIP username: ${creds.username}\n\n")
            append("Server: ${creds.domain}:${creds.port}\n\n")
            if (!creds.proxy.isNullOrBlank()) append("Proxy: ${creds.proxy}\n\n")
            append("Account: ${if (prefs.isEnabled()) "Enabled" else "Disabled"}\n\n")
            append("Registration: ${if (SipManager.isAccountRegistered()) "Online ✓" else "Offline"}")
        }
    }
}
