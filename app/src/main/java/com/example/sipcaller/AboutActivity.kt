package com.example.sipcaller

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_page)
        findViewById<TextView>(R.id.pageTitle).text = "About"
        findViewById<TextView>(R.id.pageBody).text =
            "XLine\nVersion 1.0\n\nA native Android SIP softphone built on PJSIP, " +
            "using UDP transport."
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
    }
}
