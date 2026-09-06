package com.example.sipcaller

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MyAccountActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var error: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_sip_account)
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        status=findViewById(R.id.registrationStatus); error=findViewById(R.id.errorText)
        val prefs=SipPreferences(this); val creds=prefs.load()
        val name=findViewById<EditText>(R.id.accountName); val user=findViewById<EditText>(R.id.username); val pass=findViewById<EditText>(R.id.password); val domain=findViewById<EditText>(R.id.domain); val port=findViewById<EditText>(R.id.port); val proxy=findViewById<EditText>(R.id.proxy)
        name.setText(prefs.accountName()); user.setText(creds?.username.orEmpty()); pass.setText(creds?.password.orEmpty()); domain.setText(creds?.domain.orEmpty()); port.setText(creds?.port?.toString() ?: "5060"); proxy.setText(creds?.proxy.orEmpty())
        val spinner=findViewById<Spinner>(R.id.transport); spinner.adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("UDP", "TCP", "TLS"))
        findViewById<ImageButton>(R.id.passwordToggle).setOnClickListener { pass.inputType = if(pass.inputType==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD; pass.setSelection(pass.text.length) }
        findViewById<Button>(R.id.testButton).setOnClickListener { validate(user,pass,domain,port) }
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            if(!validate(user,pass,domain,port)) return@setOnClickListener
            val account=SipManager.SipCredentials(user.text.toString().trim(), pass.text.toString(), domain.text.toString().trim(), port.text.toString().toIntOrNull()?:5060, proxy.text.toString().trim().ifBlank { null })
            prefs.save(name.text.toString().trim(), account); prefs.setEnabled(true)
            error.visibility=View.VISIBLE; error.text="Saved. Re-registering with SIP server…"
            runCatching { SipManager.restoreAndRegister(this) }.onFailure { error.text="Saved, but re-registration failed: ${it.message ?: "Unknown error"}" }
            updateStatus()
        }
        updateStatus()
    }
    private fun validate(u:EditText,p:EditText,d:EditText,port:EditText):Boolean { val ok=u.text.isNotBlank()&&p.text.isNotBlank()&&d.text.isNotBlank()&&(port.text.toString().toIntOrNull()?:0) in 1..65535; error.visibility=if(ok) View.GONE else View.VISIBLE; if(!ok) error.text="Please enter username, password, server and a valid port (1–65535)."; return ok }
    override fun onResume(){super.onResume(); if(::status.isInitialized) updateStatus()}
    private fun updateStatus(){ status.text=if(SipManager.isAccountRegistered()) "● Registered and online" else "○ Offline / not registered" }
}
