package com.example.sipcaller

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_page)
        findViewById<TextView>(R.id.pageTitle).text = "Help"
        findViewById<TextView>(R.id.pageBody).text =
            "Registration failing? Double-check your SIP username, password, and " +
            "server address (host:port).\n\nCalls not connecting? Make sure microphone " +
            "permission is granted and you're registered (see the green dot on the " +
            "Dialpad screen).\n\nIncoming calls not arriving in the background? " +
            "Disable battery optimization for this app in your phone's settings."
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
    }
}
