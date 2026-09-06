package com.example.sipcaller

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MyAccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_page)
        findViewById<TextView>(R.id.pageTitle).text = "My Account"
        findViewById<TextView>(R.id.pageBody).text = buildString {
            append("Account name: ${SessionStore.accountName.ifEmpty { "—" }}\n\n")
            append("SIP username: ${SessionStore.username.ifEmpty { "—" }}\n\n")
            append("Server: ${SessionStore.domain.ifEmpty { "—" }}\n\n")
            append("Status: ${if (SipManager.isAccountRegistered()) "Online ✓" else "Offline"}")
        }
        findViewById<android.widget.ImageView>(R.id.backButton).setOnClickListener { finish() }
    }
}
