package com.example.sipcaller

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.sipcaller.audio.SipAudioController
import com.example.sipcaller.audio.SipAudioRouter
import com.example.sipcaller.data.state.PersistentCallState
import com.example.sipcaller.data.repository.ContactRepository

class CallActivity : AppCompatActivity(), SipManager.SipCallListener {
    private var call: SipCall? = null
    private lateinit var audioController: SipAudioController
    private lateinit var audioRouter: SipAudioRouter
    private var isMuted = false
    private var isSpeakerOn = false
    private var isIncomingCall = false
    private var historySaved = false
    private var remoteNumber = ""
    private var wasConnected = false
    private var finishScheduled = false
    private var locallyDeclined = false
    private var isHeld = false
    private var keypadVisible = false
    private lateinit var persistentCallState: PersistentCallState
    private val mainHandler = Handler(Looper.getMainLooper())
    private var connectedAtMs = 0L
    private var hangupRequested = false
    private val hangupFallback = Runnable {
        if (!isFinishing && finishScheduled.not()) {
            statusBadge.text = "Call ended"
            mainHandler.removeCallbacks(durationTicker)
            persistentCallState.clear()
            restoreAudioRouting()
            scheduleFinish()
        }
    }

    private lateinit var statusBadge: TextView
    private lateinit var durationText: TextView
    private lateinit var muteButton: ImageButton
    private lateinit var speakerButton: ImageButton
    private lateinit var holdButton: ImageButton
    private lateinit var keypadButton: ImageButton
    private lateinit var keypadPanel: View
    private val durationTicker = object : Runnable {
        override fun run() {
            if (wasConnected && connectedAtMs > 0L && !isFinishing) {
                val seconds = ((System.currentTimeMillis() - connectedAtMs) / 1000L).coerceAtLeast(0L)
                durationText.text = formatDuration(seconds)
                mainHandler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        audioController = SipAudioController(this)
        audioRouter = SipAudioRouter(this)
        persistentCallState = PersistentCallState(this)
        audioController.startCallAudio()

        val remoteUri = intent.getStringExtra(EXTRA_REMOTE) ?: ""
        isIncomingCall = intent.getBooleanExtra(EXTRA_INCOMING, false)
        remoteNumber = remoteUri.substringBefore("@").removePrefix("sip:")
        if (remoteNumber.isNotBlank()) persistentCallState.begin(remoteNumber, isIncomingCall)
        call = pendingCall ?: SipManager.currentCall()
        pendingCall = null

        val avatarInitial = findViewById<TextView>(R.id.avatarInitial)
        val remoteName = findViewById<TextView>(R.id.remoteNameText)
        val remoteUriText = findViewById<TextView>(R.id.remoteUriText)
        statusBadge = findViewById(R.id.callStatusBadge)
        durationText = findViewById(R.id.callDurationText)
        muteButton = findViewById(R.id.muteButton)
        speakerButton = findViewById(R.id.speakerButton)
        holdButton = findViewById(R.id.holdButton)
        keypadButton = findViewById(R.id.keypadButton)
        keypadPanel = findViewById(R.id.dtmfPanel)
        val incomingRow = findViewById<android.widget.LinearLayout>(R.id.incomingActionsRow)
        val answerBtn = findViewById<MaterialButton>(R.id.answerButton)
        val declineBtn = findViewById<MaterialButton>(R.id.declineButton)
        val hangupBtn = findViewById<ImageButton>(R.id.hangupButton)

        val contactName = ContactRepository(this).findName(remoteNumber)
        val displayName = contactName ?: remoteNumber.ifEmpty { "Unknown" }
        remoteName.text = displayName
        remoteUriText.text = if (isIncomingCall) "From  sip:$remoteNumber" else "To  sip:$remoteNumber@${SessionStore.domain}"
        avatarInitial.text = displayName.firstOrNull()?.uppercase() ?: "?"

        incomingRow.visibility = if (isIncomingCall) View.VISIBLE else View.GONE
        statusBadge.text = if (isIncomingCall) "Incoming call" else "Calling…"
        durationText.text = ""

        answerBtn.setOnClickListener { SipIncomingCallController.answer(this); incomingRow.visibility = View.GONE; statusBadge.text = "Connecting…" }
        declineBtn.setOnClickListener { locallyDeclined = true; SipIncomingCallController.decline() }
        hangupBtn.setOnClickListener { requestHangup() }
        muteButton.setOnClickListener { toggleMute() }
        speakerButton.setOnClickListener { toggleSpeaker() }
        holdButton.setOnClickListener { toggleHold() }
        keypadButton.setOnClickListener { toggleKeypad() }
        bindDtmfKeys()

        SipManager.addListener(this)
        call?.let { syncState(it) }
    }

    private fun bindDtmfKeys() {
        val keys = mapOf(
            R.id.dtmf1 to "1", R.id.dtmf2 to "2", R.id.dtmf3 to "3",
            R.id.dtmf4 to "4", R.id.dtmf5 to "5", R.id.dtmf6 to "6",
            R.id.dtmf7 to "7", R.id.dtmf8 to "8", R.id.dtmf9 to "9",
            R.id.dtmfStar to "*", R.id.dtmf0 to "0", R.id.dtmfHash to "#"
        )
        keys.forEach { (id, tone) -> findViewById<View>(id).setOnClickListener { call?.sendDtmf(tone) } }
    }

    private fun requestHangup() {
        if (hangupRequested) return
        hangupRequested = true
        statusBadge.text = "Ending…"
        call?.hangupCall()
        // PJSIP should send DISCONNECTED shortly after hangup. This fallback prevents
        // the UI from being permanently stuck if a broken/native callback is lost.
        mainHandler.postDelayed(hangupFallback, 3_000L)
    }

    private fun toggleMute() { isMuted = audioController.setMuted(!audioController.isMuted()); muteButton.isSelected = isMuted }
    private fun toggleSpeaker() { isSpeakerOn = !audioController.isSpeakerOn(); audioRouter.select(if (isSpeakerOn) SipAudioRouter.Route.SPEAKER else SipAudioRouter.Route.EARPIECE); speakerButton.isSelected = isSpeakerOn }
    private fun toggleHold() { call?.toggleHold(); isHeld = !isHeld; holdButton.isSelected = isHeld; statusBadge.text = if (isHeld) "On hold" else if (wasConnected) "Connected" else "Connecting…" }
    private fun toggleKeypad() { keypadVisible = !keypadVisible; keypadPanel.visibility = if (keypadVisible) View.VISIBLE else View.GONE; keypadButton.isSelected = keypadVisible }
    private fun restoreAudioRouting() { audioController.stopCallAudio() }

    private fun syncState(sipCall: SipCall) { if (sipCall.lastState.isNotBlank()) onCallStateChanged(sipCall, sipCall.lastState) }

    override fun onCallStateChanged(sipCall: SipCall, state: String) {
        runOnUiThread {
            val disconnected = state.contains("DISCONN", true)
            val failedCode = sipCall.lastStatusCode >= 300
            statusBadge.text = when {
                state.contains("EARLY", true) -> "Ringing…"
                state.contains("CALLING", true) -> "Calling…"
                state.contains("CONFIRMED", true) -> {
                    if (!wasConnected) { wasConnected = true; connectedAtMs = System.currentTimeMillis(); persistentCallState.markConnected(); mainHandler.post(durationTicker) }
                    "Connected"
                }
                disconnected && failedCode -> "Failed: ${sipCall.lastStatusCode}"
                disconnected -> "Call ended"
                else -> state
            }
            if (disconnected) {
                mainHandler.removeCallbacks(durationTicker)
                mainHandler.removeCallbacks(hangupFallback)
                SipIncomingCallController.stop()
                val outcome = CallHistoryClassifier.classify(isIncomingCall, wasConnected, sipCall.lastStatusCode, sipCall.lastReason, locallyDeclined)
                saveHistory(outcome.result, sipCall.durationSeconds())
                persistentCallState.clear(); restoreAudioRouting(); scheduleFinish()
            }
        }
    }

    private fun formatDuration(total: Long): String = String.format("%02d:%02d", total / 60L, total % 60L)
    private fun scheduleFinish() { if (finishScheduled || isFinishing) return; finishScheduled = true; mainHandler.postDelayed({ if (!isFinishing) finish() }, 700L) }
    private fun saveHistory(result: String, durationSeconds: Long = 0L) { if (!historySaved && remoteNumber.isNotBlank()) { historySaved = true; CallHistoryStore.add(this, remoteNumber, if (isIncomingCall) "Incoming" else "Outgoing", result, durationSeconds) } }
    override fun onIncomingCall(call: SipCall) {}
    override fun onRegistrationStateChanged(isRegistered: Boolean, statusText: String) {}
    override fun onDestroy() {
        mainHandler.removeCallbacks(durationTicker)
        mainHandler.removeCallbacks(hangupFallback)
        SipManager.removeListener(this)
        restoreAudioRouting()
        super.onDestroy()
    }
    companion object { const val EXTRA_INCOMING = "extra_incoming"; const val EXTRA_REMOTE = "extra_remote"; var pendingCall: SipCall? = null }
}
