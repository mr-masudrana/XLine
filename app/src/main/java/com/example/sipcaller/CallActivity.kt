package com.example.sipcaller

import android.content.Context
import android.media.AudioManager
import android.os.Build
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

    private lateinit var statusBadge: TextView
    private lateinit var muteButton: ImageButton
    private lateinit var speakerButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val remoteUri = intent.getStringExtra(EXTRA_REMOTE) ?: ""
        val isIncoming = intent.getBooleanExtra(EXTRA_INCOMING, false)

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

        // No contact book yet — display the raw destination and its first
        // character as the avatar initial.
        val displayName = remoteUri.substringBefore("@").substringAfter(":")
        remoteName.text = displayName.ifEmpty { "Unknown" }
        remoteUriText.text = if (remoteUri.contains("@")) "From  $remoteUri" else "To  sip:$remoteUri@${SessionStore.domain}"
        avatarInitial.text = displayName.firstOrNull()?.uppercase() ?: "?"

        incomingRow.visibility = if (isIncoming) android.view.View.VISIBLE else android.view.View.GONE
        statusBadge.text = if (isIncoming) "Incoming call" else "Ringing…"

        answerBtn.setOnClickListener {
            call?.answer()
            incomingRow.visibility = android.view.View.GONE
        }
        declineBtn.setOnClickListener {
            call?.decline()
            finish()
        }
        hangupBtn.setOnClickListener {
            call?.hangupCall()
            restoreAudioRouting()
            finish()
        }
        muteButton.setOnClickListener { toggleMute() }
        speakerButton.setOnClickListener { toggleSpeaker() }

        // Route audio for a voice call while this screen is up.
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        SipManager.addListener(this)
    }

    private fun toggleMute() {
        isMuted = !isMuted
        setMicMuted(isMuted)
        muteButton.isSelected = isMuted
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        setSpeakerphoneOn(isSpeakerOn)
        speakerButton.isSelected = isSpeakerOn
    }

    @Suppress("DEPRECATION")
    private fun setMicMuted(muted: Boolean) {
        audioManager.isMicrophoneMute = muted
    }

    @Suppress("DEPRECATION")
    private fun setSpeakerphoneOn(on: Boolean) {
        audioManager.isSpeakerphoneOn = on
    }

    @Suppress("DEPRECATION")
    private fun restoreAudioRouting() {
        audioManager.isSpeakerphoneOn = false
        audioManager.isMicrophoneMute = false
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    override fun onCallStateChanged(call: SipCall, state: String) {
        runOnUiThread {
            statusBadge.text = state
            if (state.contains("DISCONNECTED", ignoreCase = true)) {
                restoreAudioRouting()
                finish()
            }
        }
    }

    override fun onIncomingCall(call: SipCall) {
        // A second incoming call while one is already active isn't handled
        // in this minimal UI — extend with a call-waiting flow if needed.
    }

    override fun onRegistrationStateChanged(isRegistered: Boolean, statusText: String) {
        // Not relevant on the in-call screen.
    }

    override fun onDestroy() {
        restoreAudioRouting()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_INCOMING = "extra_incoming"
        const val EXTRA_REMOTE = "extra_remote"
        // Simple hand-off from DialpadFragment/incoming-call callback;
        // swap for a bound service or shared repository if you need to
        // support multiple simultaneous calls.
        var pendingCall: SipCall? = null
    }
}
