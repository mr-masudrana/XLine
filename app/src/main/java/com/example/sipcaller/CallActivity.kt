package com.example.sipcaller

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class CallActivity : AppCompatActivity(), SipManager.SipCallListener {

    private var call: SipCall? = null
    private lateinit var audioManager: AudioManager
    private var isMuted = false
    private var isSpeakerOn = false
    private var isIncomingCall = false
    private var historySaved = false
    private var remoteNumber = ""

    private lateinit var statusBadge: TextView
    private lateinit var muteButton: ImageButton
    private lateinit var speakerButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val remoteUri = intent.getStringExtra(EXTRA_REMOTE) ?: ""
        isIncomingCall = intent.getBooleanExtra(EXTRA_INCOMING, false)
        remoteNumber = remoteUri.substringBefore("@").removePrefix("sip:")

        call = pendingCall
        pendingCall = null

        val avatarInitial = findViewById<TextView>(R.id.avatarInitial)
        val remoteName = findViewById<TextView>(R.id.remoteNameText)
        val remoteUriText = findViewById<TextView>(R.id.remoteUriText)
        statusBadge = findViewById(R.id.callStatusBadge)
        muteButton = findViewById(R.id.muteButton)
        speakerButton = findViewById(R.id.speakerButton)
        val incomingRow = findViewById<android.widget.LinearLayout>(R.id.incomingActionsRow)
        val answerBtn = findViewById<MaterialButton>(R.id.answerButton)
        val declineBtn = findViewById<MaterialButton>(R.id.declineButton)
        val hangupBtn = findViewById<ImageButton>(R.id.hangupButton)

        val contactName = ContactStore.findName(this, remoteNumber)
        val displayName = contactName ?: remoteNumber.ifEmpty { "Unknown" }
        remoteName.text = displayName
        remoteUriText.text = if (isIncomingCall) "From  sip:$remoteNumber" else "To  sip:$remoteNumber@${SessionStore.domain}"
        avatarInitial.text = displayName.firstOrNull()?.uppercase() ?: "?"

        incomingRow.visibility = if (isIncomingCall) android.view.View.VISIBLE else android.view.View.GONE
        // Important: don't show fake Ringing. Actual PJSIP EARLY state will update this.
        statusBadge.text = if (isIncomingCall) "Incoming call" else "Calling…"

        answerBtn.setOnClickListener {
            call?.answer()
            incomingRow.visibility = android.view.View.GONE
            statusBadge.text = "Connecting…"
        }
        declineBtn.setOnClickListener { call?.decline(); saveHistory("Declined") }
        hangupBtn.setOnClickListener { call?.hangupCall(); saveHistory("Ended") }
        muteButton.setOnClickListener { toggleMute() }
        speakerButton.setOnClickListener { toggleSpeaker() }

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        SipManager.addListener(this)
    }

    private fun toggleMute() { isMuted = !isMuted; audioManager.isMicrophoneMute = isMuted; muteButton.isSelected = isMuted }
    @Suppress("DEPRECATION") private fun toggleSpeaker() { isSpeakerOn = !isSpeakerOn; audioManager.isSpeakerphoneOn = isSpeakerOn; speakerButton.isSelected = isSpeakerOn }
    @Suppress("DEPRECATION") private fun restoreAudioRouting() { audioManager.isSpeakerphoneOn = false; audioManager.isMicrophoneMute = false; audioManager.mode = AudioManager.MODE_NORMAL }

    override fun onCallStateChanged(call: SipCall, state: String) {
        runOnUiThread {
            // PJSIP stateText is the source of truth: CALLING -> EARLY (ringing) -> CONFIRMED.
            statusBadge.text = when {
                state.contains("EARLY", true) -> "Ringing…"
                state.contains("CALLING", true) -> "Calling…"
                state.contains("CONFIRMED", true) -> "Connected"
                state.contains("DISCONNECTED", true) -> "Call ended"
                else -> state
            }
            if (state.contains("DISCONNECTED", true)) {
                saveHistory("Completed")
                restoreAudioRouting()
                finish()
            }
        }
    }

    private fun saveHistory(result: String) {
        if (!historySaved && remoteNumber.isNotBlank()) {
            historySaved = true
            CallHistoryStore.add(this, remoteNumber, if (isIncomingCall) "Incoming" else "Outgoing", result)
        }
    }

    override fun onIncomingCall(call: SipCall) {}
    override fun onRegistrationStateChanged(isRegistered: Boolean, statusText: String) {}

    override fun onDestroy() {
        SipManager.removeListener(this)
        restoreAudioRouting()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_INCOMING = "extra_incoming"
        const val EXTRA_REMOTE = "extra_remote"
        var pendingCall: SipCall? = null
    }
}
