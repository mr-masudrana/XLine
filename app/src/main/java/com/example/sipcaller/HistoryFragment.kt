package com.example.sipcaller

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import java.util.Date

class HistoryFragment : Fragment() {
    private lateinit var list: LinearLayout
    private lateinit var empty: TextView
    private lateinit var scroll: ScrollView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        list = view.findViewById(R.id.historyList); empty = view.findViewById(R.id.historyEmptyText); scroll = view.findViewById(R.id.historyScroll)
        view.findViewById<Button>(R.id.clearHistoryButton).setOnClickListener { CallHistoryStore.clear(requireContext()); render() }
        render(); return view
    }

    override fun onResume() { super.onResume(); if (::list.isInitialized) render() }

    private fun render() {
        val items = CallHistoryStore.getAll(requireContext())
        list.removeAllViews(); empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE; scroll.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        items.forEach { item ->
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 20, 16, 20); setBackgroundResource(android.R.drawable.list_selector_background) }
            val name = ContactStore.findName(requireContext(), item.number) ?: item.number
            val arrow = if (item.direction == "Incoming") "↓ Incoming" else "↑ Outgoing"
            row.addView(TextView(requireContext()).apply { text = name; textSize = 18f; setTypeface(null, 1) })
            row.addView(TextView(requireContext()).apply { text = "$arrow • ${item.result} • ${DateFormat.format("MMM d, h:mm a", Date(item.timestamp))}"; textSize = 13f })
            row.setOnClickListener { call(item.number) }
            list.addView(row)
        }
    }

    private fun call(number: String) {
        if (!SipManager.isAccountRegistered()) { Toast.makeText(requireContext(), "Account is offline", Toast.LENGTH_SHORT).show(); return }
        val call = SipManager.makeCall(number) ?: run { Toast.makeText(requireContext(), "Could not start call", Toast.LENGTH_SHORT).show(); return }
        CallActivity.pendingCall = call
        startActivity(Intent(requireContext(), CallActivity::class.java).putExtra(CallActivity.EXTRA_INCOMING, false).putExtra(CallActivity.EXTRA_REMOTE, number))
    }
}
