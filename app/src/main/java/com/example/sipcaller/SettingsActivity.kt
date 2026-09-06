package com.example.sipcaller

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.sipcaller.data.repository.AppSettingsRepository

class SettingsActivity : AppCompatActivity() {
    private lateinit var settings: AppSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettingsRepository(this)
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
        }
        scroll.addView(root)

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val back = Button(this).apply { text = "‹"; textSize = 28f; setOnClickListener { finish() } }
        val title = TextView(this).apply { text = "Settings"; textSize = 24f; setPadding(dp(12), 0, 0, 0) }
        header.addView(back, LinearLayout.LayoutParams(dp(56), dp(52)))
        header.addView(title)
        root.addView(header)

        section(root, "Connection")
        toggle(root, "Auto-start SIP service", "Start and restore SIP service automatically", settings.autoStart) {
            settings.autoStart = it
        }
        toggle(root, "Auto reconnect", "Reconnect after network changes or registration loss", settings.autoReconnect) {
            settings.autoReconnect = it
        }
        toggle(root, "NAT keep-alive", "Keep the SIP NAT binding active", settings.keepAliveEnabled) {
            settings.keepAliveEnabled = it
        }
        numberSetting(root, "Keep-alive interval", "Seconds (10–300)", settings.keepAliveSeconds) { settings.keepAliveSeconds = it }

        section(root, "Call & Audio")
        toggle(root, "Vibrate for incoming calls", "Use vibration together with ringtone", settings.incomingVibration) {
            settings.incomingVibration = it
        }
        toggle(root, "Speaker by default", "Start connected calls on speaker", settings.speakerByDefault) {
            settings.speakerByDefault = it
        }

        section(root, "SIP Account")
        val account = TextView(this).apply {
            val p = SipPreferences(this@SettingsActivity)
            text = if (p.load() != null) "Account configured: ${p.accountName().ifBlank { "SIP Account" }}" else "No SIP account configured"
            textSize = 16f
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(account)
        TextView(this).apply {
            text = "Account credentials are managed from My Account. Passwords are not shown here."
            textSize = 13f
        }.also(root::addView)

        setContentView(scroll)
    }

    private fun section(root: LinearLayout, text: String) {
        root.addView(TextView(this).apply {
            this.text = text
            textSize = 18f
            setPadding(0, dp(24), 0, dp(8))
        })
    }

    private fun toggle(root: LinearLayout, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, dp(8)) }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val label = TextView(this).apply { text = title; textSize = 16f }
        val sw = Switch(this).apply { isChecked = checked }
        row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(sw)
        box.addView(row)
        box.addView(TextView(this).apply { text = subtitle; textSize = 13f })
        sw.setOnCheckedChangeListener { _, value -> onChange(value) }
        root.addView(box)
    }

    private fun numberSetting(root: LinearLayout, title: String, hint: String, value: Int, onSave: (Int) -> Unit) {
        root.addView(TextView(this).apply { text = title; textSize = 16f; setPadding(0, dp(12), 0, dp(4)) })
        val input = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER; this.hint = hint; setText(value.toString()) }
        input.setOnFocusChangeListener { _, focused -> if (!focused) input.text.toString().toIntOrNull()?.let(onSave) }
        root.addView(input)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
