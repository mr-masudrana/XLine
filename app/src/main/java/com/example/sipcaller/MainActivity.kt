package com.example.sipcaller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), SipManager.SipCallListener {

    private lateinit var domainInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var destinationInput: EditText
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        domainInput = findViewById(R.id.domainInput)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        destinationInput = findViewById(R.id.destinationInput)
        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.registerButton).setOnClickListener { onRegisterClicked() }
        findViewById<Button>(R.id.callButton).setOnClickListener { onCallClicked() }

        SipManager.callListener = this
        requestRuntimePermissions()
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val toRequest = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 100)
        }
    }

    private fun onRegisterClicked() {
        val domain = domainInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (domain.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "সব ফিল্ড পূরণ করুন", Toast.LENGTH_SHORT).show()
            return
        }

        val creds = SipManager.SipCredentials(
            username = username,
            password = password,
            domain = domain
        )
        SipManager.registerAccount(creds)
        statusText.text = "Registering..."

        // Start the foreground service so registration survives backgrounding.
        val serviceIntent = Intent(this, SipCallService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun onCallClicked() {
        val destination = destinationInput.text.toString().trim()
        if (destination.isEmpty()) {
            Toast.makeText(this, "নম্বর দিন", Toast.LENGTH_SHORT).show()
            return
        }
        if (!SipManager.isAccountRegistered()) {
            Toast.makeText(this, "Account registered নেই — আগে Register করুন", Toast.LENGTH_SHORT).show()
            return
        }
        val call = SipManager.makeCall(destination)
        if (call != null) {
            val intent = Intent(this, CallActivity::class.java)
            intent.putExtra(CallActivity.EXTRA_REMOTE, destination)
            startActivity(intent)
        } else {
            Toast.makeText(this, "কল শুরু করা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onIncomingCall(call: SipCall) {
        runOnUiThread {
            val intent = Intent(this, CallActivity::class.java)
            intent.putExtra(CallActivity.EXTRA_INCOMING, true)
            intent.putExtra(CallActivity.EXTRA_REMOTE, call.remoteUri)
            CallActivity.pendingIncomingCall = call
            startActivity(intent)
        }
    }

    override fun onCallStateChanged(call: SipCall, state: String) {
        runOnUiThread { statusText.text = "Call: $state" }
    }

    override fun onRegistrationStateChanged(isRegistered: Boolean, statusText: String) {
        runOnUiThread {
            this.statusText.text = if (isRegistered) "Registered ✓ ($statusText)" else "Registration failed: $statusText"
        }
    }
}
