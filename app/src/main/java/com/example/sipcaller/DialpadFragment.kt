package com.example.sipcaller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class DialpadFragment : Fragment(), SipManager.SipCallListener {

    private lateinit var numberDisplay: TextView
    private lateinit var statusLabel: TextView
    private lateinit var statusDot: View
    private lateinit var myIdentityText: TextView
    private lateinit var contactPreviewText: TextView
    private var rawNumber: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dialpad, container, false)
        com.example.sipcaller.ui.UiMotion.reveal(view)

        numberDisplay = view.findViewById(R.id.numberDisplay)
        statusLabel = view.findViewById(R.id.statusLabel)
        statusDot = view.findViewById(R.id.statusDot)
        myIdentityText = view.findViewById(R.id.myIdentityText)
        contactPreviewText = view.findViewById(R.id.contactPreviewText)

        myIdentityText.text = SessionStore.username.ifEmpty { "Not signed in" }
        updateRegistrationUi(SipManager.isAccountRegistered())

        val keyIds = mapOf(
            R.id.key1 to "1", R.id.key2 to "2", R.id.key3 to "3",
            R.id.key4 to "4", R.id.key5 to "5", R.id.key6 to "6",
            R.id.key7 to "7", R.id.key8 to "8", R.id.key9 to "9",
            R.id.keyStar to "*", R.id.key0 to "0", R.id.keyHash to "#"
        )
        for ((id, digit) in keyIds) {
            view.findViewById<TextView>(id).setOnClickListener { appendDigit(digit) }
        }

        view.findViewById<ImageButton>(R.id.backspaceButton).setOnClickListener { removeLastDigit() }
        view.findViewById<ImageButton>(R.id.callButton).setOnClickListener { onCallClicked() }

        SipManager.addListener(this)
        return view
    }

    private fun appendDigit(digit: String) {
        rawNumber += digit
        updateNumberUi()
    }

    private fun removeLastDigit() {
        if (rawNumber.isNotEmpty()) {
            rawNumber = rawNumber.dropLast(1)
            updateNumberUi()
        }
    }

    private fun updateNumberUi() {
        numberDisplay.text = formatNumber(rawNumber)
        contactPreviewText.text = when {
            rawNumber.isEmpty() -> "Enter SIP number"
            rawNumber.length < 3 -> "Keep typing"
            else -> {
                val resolved = runCatching {
                    ContactStore.findName(requireContext(), rawNumber)
                }.getOrNull()
                resolved ?: "Ready to call"
            }
        }
    }

    private fun formatNumber(value: String): String {
        if (value.length <= 4) return value
        return value.chunked(3).joinToString(" ")
    }

    private fun onCallClicked() {
        val destination = rawNumber.trim()
        if (destination.isEmpty()) {
            Toast.makeText(requireContext(), "নম্বর দিন", Toast.LENGTH_SHORT).show()
            return
        }
        if (!SipManager.isAccountRegistered()) {
            Toast.makeText(requireContext(), "Account registered নেই — আগে login করুন", Toast.LENGTH_SHORT).show()
            return
        }
        val call = SipManager.makeCall(destination)
        if (call != null) {
            CallActivity.pendingCall = call
            val intent = android.content.Intent(requireContext(), CallActivity::class.java)
            intent.putExtra(CallActivity.EXTRA_INCOMING, false)
            intent.putExtra(CallActivity.EXTRA_REMOTE, destination)
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "কল শুরু করা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateRegistrationUi(isRegistered: Boolean) {
        if (isRegistered) {
            statusLabel.text = "Online"
            statusDot.setBackgroundResource(R.drawable.bg_status_dot_online)
        } else {
            statusLabel.text = "Offline"
            statusDot.setBackgroundResource(R.drawable.bg_status_dot_offline)
        }
    }

    override fun onDestroyView() {
        SipManager.removeListener(this)
        super.onDestroyView()
    }

    override fun onIncomingCall(call: SipCall) {
        activity?.runOnUiThread {
            CallActivity.pendingCall = call
            val intent = android.content.Intent(requireContext(), CallActivity::class.java)
            intent.putExtra(CallActivity.EXTRA_INCOMING, true)
            intent.putExtra(CallActivity.EXTRA_REMOTE, call.remoteUri)
            startActivity(intent)
        }
    }

    override fun onCallStateChanged(call: SipCall, state: String) {
        // Handled inside CallActivity while a call is active.
    }

    override fun onRegistrationStateChanged(isRegistered: Boolean, statusText: String) {
        activity?.runOnUiThread { updateRegistrationUi(isRegistered) }
    }
}
