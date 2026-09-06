package com.example.sipcaller

import android.widget.GridLayout
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.LinearLayout
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
    private lateinit var audioDeviceMonitor: com.example.sipcaller.audio.SipAudioDeviceMonitor
    private lateinit var audioRouteButton: ImageButton
    private lateinit var callQualityText: TextView
    private val qualityHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var qualityRunnable: Runnable? = null
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
    private var holdOperationInFlight = false
    private var dtmfLocked = false
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_call)
        com.example.sipcaller.ui.UiMotion.reveal(findViewById(android.R.id.content))
        setupActiveControls()
        audioController = SipAudioController(this)
        audioRouter = SipAudioRouter(this)
        audioDeviceMonitor = com.example.sipcaller.audio.SipAudioDeviceMonitor(this) { routes ->
            runOnUiThread { if (::audioRouteButton.isInitialized) audioRouteButton.isEnabled = routes.size > 1 }
        }
        audioDeviceMonitor.start()
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
        audioRouteButton = findViewById(R.id.audioRouteButton)
        startAvatarPulse()
        callQualityText = findViewById(R.id.callQualityText)
        holdButton = findViewById(R.id.holdButton)
        keypadButton = findViewById(R.id.keypadButton)
        keypadPanel = findViewById(R.id.dtmfPanel)
        val incomingRow = findViewById<android.widget.LinearLayout>(R.id.incomingActionsRow)
        val activeControls = findViewById<android.widget.LinearLayout>(R.id.activeControls)
        val avatarPulse = findViewById<View>(R.id.avatarPulseContainer)
        val answerBtn = findViewById<MaterialButton>(R.id.answerButton)
        val declineBtn = findViewById<MaterialButton>(R.id.declineButton)
        val hangupBtn = findViewById<ImageButton>(R.id.hangupButton)

        val contactName = ContactRepository(this).findName(remoteNumber)
        val displayName = contactName ?: remoteNumber.ifEmpty { "Unknown" }
        remoteName.text = displayName
        remoteUriText.text = remoteNumber.ifBlank { "Unknown number" }
        avatarInitial.text = displayName.firstOrNull()?.uppercase() ?: "?"

        incomingRow.visibility = if (isIncomingCall) View.VISIBLE else View.GONE
        activeControls.visibility = if (isIncomingCall) View.GONE else View.VISIBLE
        statusBadge.text = if (isIncomingCall) "Incoming call" else "Calling…"
        if (isIncomingCall) {
            val pulse = AlphaAnimation(0.55f, 1f).apply {
                duration = 900L
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
            avatarPulse.startAnimation(pulse)
        }
        durationText.text = ""

        answerBtn.setOnClickListener {
            avatarPulse.clearAnimation()
            SipIncomingCallController.answer(this)
            incomingRow.visibility = View.GONE
            activeControls.visibility = View.VISIBLE
            statusBadge.text = "Connecting…"
        }
        declineBtn.setOnClickListener { locallyDeclined = true; SipIncomingCallController.decline() }
        hangupBtn.setOnClickListener { requestHangup() }
        muteButton.setOnClickListener { toggleMute() }
        speakerButton.setOnClickListener { toggleSpeaker() }
        startQualityMonitoring()
        audioRouteButton.setOnClickListener { showAudioRouteChooser() }
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
        keys.forEach { (id, tone) ->
            findViewById<View>(id).setOnClickListener {
                val c = call ?: return@setOnClickListener
                if (dtmfLocked || c.isDisconnected()) return@setOnClickListener
                dtmfLocked = true
                it.isEnabled = false
                c.sendDtmf(tone)
                it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                mainHandler.postDelayed({ dtmfLocked = false; it.isEnabled = true }, 120L)
            }
        }
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

    private fun toggleMute() {
        isMuted = audioController.setMuted(!audioController.isMuted())
        muteButton.isSelected = isMuted
        muteButton.contentDescription = if (isMuted) "Unmute microphone" else "Mute microphone"
    }
    private fun toggleSpeaker() {
        val target = if (audioRouter.currentRoute() == SipAudioRouter.Route.SPEAKER) SipAudioRouter.Route.EARPIECE else SipAudioRouter.Route.SPEAKER
        if (audioRouter.select(target)) {
            isSpeakerOn = target == SipAudioRouter.Route.SPEAKER
            speakerButton.isSelected = isSpeakerOn
            speakerButton.contentDescription = if (isSpeakerOn) "Turn speaker off" else "Turn speaker on"
        }
    }
    private fun startAvatarPulse() {
        val avatar = findViewById<View>(R.id.avatarPulseContainer)
        avatar.animate().scaleX(1.04f).scaleY(1.04f).setDuration(900).withEndAction {
            avatar.animate().scaleX(1f).scaleY(1f).setDuration(900).start()
        }.start()
    }

    private fun startQualityMonitoring() {
        qualityRunnable = object : Runnable {
            override fun run() {
                val c = currentCall
                if (c != null && !c.isDisconnected()) {
                    val q = com.example.sipcaller.diagnostics.SipCallQualityMonitor.snapshot(this@CallActivity, c)
                    callQualityText.text = "Quality: ${q.grade.name.lowercase().replaceFirstChar { it.uppercase() }} • ${q.network}"
                    qualityHandler.postDelayed(this, 3000L)
                }
            }
        }
        qualityHandler.post(qualityRunnable!!)
    }

    private fun showAudioRouteChooser() {
        val routes = audioRouter.availableRoutes().toList()
        val labels = routes.map { when (it) {
            SipAudioRouter.Route.EARPIECE -> "Earpiece"
            SipAudioRouter.Route.SPEAKER -> "Speaker"
            SipAudioRouter.Route.WIRED -> "Wired headset"
            SipAudioRouter.Route.BLUETOOTH -> "Bluetooth"
        }}.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Audio output")
            .setSingleChoiceItems(labels, routes.indexOf(audioRouter.currentRoute()).coerceAtLeast(0)) { dialog, which ->
                if (audioRouter.select(routes[which])) {
                    isSpeakerOn = routes[which] == SipAudioRouter.Route.SPEAKER
                    speakerButton.isSelected = isSpeakerOn
                }
                dialog.dismiss()
            }.show()
    }

    private fun toggleHold() {
        val c = call ?: return
        if (holdOperationInFlight || c.isDisconnected()) return
        holdOperationInFlight = true
        holdButton.isEnabled = false
        val targetHeld = !isHeld
        statusBadge.text = if (targetHeld) "Holding…" else "Resuming…"
        c.toggleHold()
        mainHandler.postDelayed({
            isHeld = targetHeld
            holdButton.isSelected = isHeld
            holdButton.contentDescription = if (isHeld) "Resume call" else "Hold call"
            statusBadge.text = if (isHeld) "On hold" else if (wasConnected) "Connected" else "Connecting…"
            holdButton.isEnabled = true
            holdOperationInFlight = false
        }, 350L)
    }
    private fun toggleKeypad() { keypadVisible = !keypadVisible; keypadPanel.visibility = if (keypadVisible) View.VISIBLE else View.GONE; keypadButton.isSelected = keypadVisible }
    private fun restoreAudioRouting() {
        if (::audioDeviceMonitor.isInitialized) audioDeviceMonitor.stop()
        audioRouter.clearCommunicationRoute()
        audioController.stopCallAudio()
    }

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
                disconnected && failedCode -> "Call failed"
                disconnected -> "Call ended"
                else -> when {
                    state.contains("CALLING", true) -> "Calling…"
                    state.contains("EARLY", true) || state.contains("RINGING", true) -> "Ringing…"
                    else -> state
                }
            }
            if (state.contains("CONFIRMED", true) || state.contains("CONNECTED", true)) {
                findViewById<View>(R.id.incomingActionsRow).visibility = View.GONE
                findViewById<View>(R.id.activeControls).visibility = View.VISIBLE
                findViewById<View>(R.id.avatarPulseContainer).clearAnimation()
                findViewById<TextView>(R.id.callStatusBadge).text = "Connected"
                startCallTimer()
            }
            if (disconnected) {
                muteButton.isEnabled = false
                speakerButton.isEnabled = false
                holdButton.isEnabled = false
                keypadButton.isEnabled = false
                audioRouteButton.isEnabled = false
                stopCallTimer()
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
        qualityRunnable?.let { qualityHandler.removeCallbacks(it) }
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { SipManager.removeListener(this) }
        if (::audioDeviceMonitor.isInitialized) runCatching { audioDeviceMonitor.stop() }
        mainHandler.removeCallbacks(durationTicker)
        mainHandler.removeCallbacks(hangupFallback)
        SipManager.removeListener(this)
        restoreAudioRouting()
        super.onDestroy()
    }
    companion object { const val EXTRA_INCOMING = "extra_incoming"; const val EXTRA_REMOTE = "extra_remote"; var pendingCall: SipCall? = null }

    private val callTimerRunnable = object : Runnable {
        override fun run() {
            if (!timerRunning) return
            val elapsed = (System.currentTimeMillis() - callStartedAt) / 1000L
            val h = elapsed / 3600
            val m = (elapsed % 3600) / 60
            val sec = elapsed % 60
            findViewById<TextView>(R.id.callDurationText).text =
                if (h > 0) String.format("%02d:%02d:%02d", h, m, sec)
                else String.format("%02d:%02d", m, sec)
            callTimerHandler.postDelayed(this, 1000L)
        }
    }

    private fun startCallTimer() {
        if (timerRunning) return
        timerRunning = true
        callStartedAt = System.currentTimeMillis()
        callTimerHandler.post(callTimerRunnable)
    }

    private fun stopCallTimer() {
        timerRunning = false
        callTimerHandler.removeCallbacks(callTimerRunnable)
    }

    private fun setControlSelected(button: ImageButton, selected: Boolean) {
        button.isSelected = selected
        button.alpha = if (selected) 1f else 0.75f
    }

    private fun setupActiveControls() {
        val muteButton = findViewById<ImageButton>(R.id.muteButton)
        val speakerButton = findViewById<ImageButton>(R.id.speakerButton)
        val holdButton = findViewById<ImageButton>(R.id.holdButton)
        val keypadButton = findViewById<ImageButton>(R.id.keypadButton)
        val hangupButton = findViewById<ImageButton>(R.id.hangupButton)
        val dtmfPanel = findViewById<View>(R.id.dtmfPanel)

        muteButton.setOnClickListener {
            muted = !muted
            try { CallControlBridge.mute(muted) } catch (_: Exception) {}
            setControlSelected(muteButton, muted)
        }
        speakerButton.setOnClickListener {
            speakerEnabled = !speakerEnabled
            try { CallControlBridge.speaker(speakerEnabled) } catch (_: Exception) {}
            setControlSelected(speakerButton, speakerEnabled)
        }
        holdButton.setOnClickListener {
            held = !held
            try { CallControlBridge.hold(held) } catch (_: Exception) {}
            setControlSelected(holdButton, held)
            findViewById<TextView>(R.id.callStatusBadge).text = if (held) "On hold" else "Connected"
        }
        keypadButton.setOnClickListener {
            dtmfPanel.visibility = if (dtmfPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        hangupButton.setOnClickListener {
            stopCallTimer()
            try { CallControlBridge.hangup() } catch (_: Exception) {}
            finish()
        }

        val digits = mapOf(
            R.id.dtmf0 to "0", R.id.dtmf1 to "1", R.id.dtmf2 to "2",
            R.id.dtmf3 to "3", R.id.dtmf4 to "4", R.id.dtmf5 to "5",
            R.id.dtmf6 to "6", R.id.dtmf7 to "7", R.id.dtmf8 to "8",
            R.id.dtmf9 to "9", R.id.dtmfStar to "*", R.id.dtmfHash to "#"
        )
        digits.forEach { (id, digit) ->
            findViewById<View>(id).setOnClickListener {
                try { CallControlBridge.dtmf(digit) } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        qualityRunnable?.let { qualityHandler.removeCallbacks(it) }
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { SipManager.removeListener(this) }
        if (::audioDeviceMonitor.isInitialized) runCatching { audioDeviceMonitor.stop() }
        stopCallTimer()
        super.onDestroy()
    }

}
