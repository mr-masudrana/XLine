package com.example.sipcaller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    private lateinit var accountNameInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var serverInput: EditText
    private lateinit var statusText: TextView
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        accountNameInput = findViewById(R.id.accountNameInput)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        serverInput = findViewById(R.id.serverInput)
        statusText = findViewById(R.id.statusText)

        findViewById<TextView>(R.id.showPasswordToggle).setOnClickListener { togglePasswordVisibility() }
        findViewById<MaterialButton>(R.id.loginButton).setOnClickListener { onLoginClicked() }

        SipManager.init()
        requestRuntimePermissions()
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
        val toggle = findViewById<TextView>(R.id.showPasswordToggle)
        if (passwordVisible) {
            passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggle.text = "Hide"
        } else {
            passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggle.text = "Show"
        }
        passwordInput.setSelection(passwordInput.text.length)
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

    private fun onLoginClicked() {
        val accountName = accountNameInput.text.toString().trim()
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val server = serverInput.text.toString().trim()

        if (username.isEmpty() || password.isEmpty() || server.isEmpty()) {
            Toast.makeText(this, "SIP username, password এবং server দিন", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse "host:port" — bare host defaults to 5060
        val domain: String
        val port: Int
        if (server.contains(":")) {
            val parts = server.split(":")
            domain = parts[0]
            port = parts.getOrNull(1)?.toIntOrNull() ?: 5060
        } else {
            domain = server
            port = 5060
        }

        val creds = SipManager.SipCredentials(
            username = username,
            password = password,
            domain = domain,
            port = port
        )
        SessionStore.accountName = accountName.ifEmpty { username }
        SessionStore.username = username
        SessionStore.domain = domain

        statusText.text = "Registering…"
        SipManager.registerAccount(creds)

        val serviceIntent = Intent(this, SipCallService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        // Move to Home immediately; DialpadFragment reflects live registration status.
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}

/** Simple in-memory session info shared across screens (not persisted). */
object SessionStore {
    var accountName: String = ""
    var username: String = ""
    var domain: String = ""
}
