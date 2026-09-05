package com.example.sipcaller

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CallActivity : AppCompatActivity() {

    private var call: SipCall? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        val remoteText = findViewById<TextView>(R.id.remoteUriText)
        val answerBtn = findViewById<Button>(R.id.answerButton)
        val declineBtn = findViewById<Button>(R.id.declineButton)
        val hangupBtn = findViewById<Button>(R.id.hangupButton)
        val holdBtn = findViewById<Button>(R.id.holdButton)

        val isIncoming = intent.getBooleanExtra(EXTRA_INCOMING, false)
        remoteText.text = intent.getStringExtra(EXTRA_REMOTE) ?: ""

        call = pendingIncomingCall
        pendingIncomingCall = null

        answerBtn.isEnabled = isIncoming
        declineBtn.isEnabled = isIncoming

        answerBtn.setOnClickListener { call?.answer() }
        declineBtn.setOnClickListener { call?.decline(); finish() }
        hangupBtn.setOnClickListener { call?.hangupCall(); finish() }
        holdBtn.setOnClickListener { call?.toggleHold() }
    }

    companion object {
        const val EXTRA_INCOMING = "extra_incoming"
        const val EXTRA_REMOTE = "extra_remote"
        // Simple hand-off for the demo; swap for a bound service or shared
        // repository if you need multiple simultaneous calls.
        var pendingIncomingCall: SipCall? = null
    }
}
