package com.example.sipcaller

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.sipcaller.diagnostics.SipDiagnostics
import com.example.sipcaller.diagnostics.SipDiagnosticsStore

class DiagnosticsActivity : AppCompatActivity() {
    private lateinit var summary: TextView
    private lateinit var logView: TextView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); build(); refresh() }
    override fun onResume() { super.onResume(); if (::summary.isInitialized) refresh() }
    private fun build() {
        val scroll=ScrollView(this); val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(16),dp(20),dp(28)) }; scroll.addView(root)
        root.addView(LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; addView(Button(this@DiagnosticsActivity).apply{text="‹"; textSize=28f; setOnClickListener{finish()}},LinearLayout.LayoutParams(dp(56),dp(52))); addView(TextView(this@DiagnosticsActivity).apply{text="SIP Diagnostics"; textSize=24f}) })
        summary=TextView(this).apply { textSize=15f; setPadding(0,dp(18),0,dp(18)); setTextIsSelectable(true) }; root.addView(summary)
        val actions=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        actions.addView(Button(this).apply{text="Refresh";setOnClickListener{refresh()}},LinearLayout.LayoutParams(0,dp(52),1f))
        actions.addView(Button(this).apply{text="Reconnect";setOnClickListener{SipManager.stop(); SipManager.start(this@DiagnosticsActivity); Toast.makeText(this@DiagnosticsActivity,"SIP reconnect requested",Toast.LENGTH_SHORT).show()}},LinearLayout.LayoutParams(0,dp(52),1f))
        actions.addView(Button(this).apply{text="Copy report";setOnClickListener{copyReport()}},LinearLayout.LayoutParams(0,dp(52),1f)); root.addView(actions)
        root.addView(TextView(this).apply{text="Diagnostic log"; textSize=18f; setPadding(0,dp(22),0,dp(8))})
        logView=TextView(this).apply { textSize=12f; setTextIsSelectable(true); setPadding(dp(12),dp(12),dp(12),dp(12)); background=context.getDrawable(android.R.drawable.editbox_background) }; root.addView(logView)
        root.addView(Button(this).apply{text="Clear log";setOnClickListener{SipDiagnostics.clear();refresh()}})
        setContentView(scroll)
    }
    private fun refresh() {
        val s=SipDiagnosticsStore.current()
        val failure=com.example.sipcaller.diagnostics.SipFailureReporter.latest()
        summary.text="Registration: ${s.registrationState}\nNetwork: ${s.networkState}\nServer: ${s.server}\nLast SIP response: ${s.lastSipResponse}\nLast error: ${s.lastError}\nRetry count: ${s.retryCount}\nEngine registered: ${SipManager.isAccountRegistered()}"+
            (failure?.let{"\n\nLatest issue: ${it.title}\n${it.message}\nSuggested action: ${it.action}"} ?: "")
        logView.text=SipDiagnostics.exportText().ifBlank{"No diagnostic events yet."}
    }
    private fun copyReport(){ val report=summary.text.toString()+"\n\nLOG\n"+SipDiagnostics.exportText(); (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("SIP diagnostics",report)); Toast.makeText(this,"Diagnostic report copied",Toast.LENGTH_SHORT).show() }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
