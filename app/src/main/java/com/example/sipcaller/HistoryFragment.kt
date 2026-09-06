package com.example.sipcaller

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import java.util.Calendar
import java.util.Date

class HistoryFragment : Fragment() {
    private lateinit var list: LinearLayout
    private lateinit var empty: TextView
    private lateinit var scroll: ScrollView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        list = view.findViewById(R.id.historyList)
        empty = view.findViewById(R.id.historyEmptyText)
        scroll = view.findViewById(R.id.historyScroll)
        view.findViewById<Button>(R.id.clearHistoryButton).setOnClickListener {
            CallHistoryStore.clear(requireContext())
            render()
        }
        render()
        return view
    }

    override fun onResume() { super.onResume(); if (::list.isInitialized) render() }

    private fun render() {
        val items = CallHistoryStore.getAll(requireContext())
        list.removeAllViews()
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        scroll.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE

        var previousGroup = ""
        items.forEach { item ->
            val group = dateGroup(item.timestamp)
            if (group != previousGroup) {
                previousGroup = group
                list.addView(TextView(requireContext()).apply {
                    text = group
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(12, 24, 12, 8)
                })
            }
            addHistoryRow(item)
        }
    }

    private fun addHistoryRow(item: CallHistoryItem) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 22)
            setBackgroundResource(android.R.drawable.list_selector_background)
        }
        val name = ContactStore.findName(requireContext(), item.number) ?: item.number
        val directionLabel = when (item.direction) {
            "Incoming" -> "↓ Incoming"
            else -> "↑ Outgoing"
        }
        val result = item.result.ifBlank { "Unknown" }
        val time = DateFormat.format("h:mm a", Date(item.timestamp)).toString()
        val duration = if (item.durationSeconds > 0) " • ${formatDuration(item.durationSeconds)}" else ""

        row.addView(TextView(requireContext()).apply { text = name; textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD) })
        row.addView(TextView(requireContext()).apply { text = item.number; textSize = 14f })
        row.addView(TextView(requireContext()).apply { text = "$directionLabel • $result$duration • $time"; textSize = 13f })
        row.setOnClickListener { call(item.number) }
        row.setOnLongClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(name)
                .setMessage("$directionLabel\n$result\n${DateFormat.format("MMM d, yyyy h:mm a", Date(item.timestamp))}")
                .setPositiveButton("Call") { _, _ -> call(item.number) }
                .setNegativeButton("Delete") { _, _ -> CallHistoryStore.delete(requireContext(), item); render() }
                .show()
            true
        }
        list.addView(row)
    }

    private fun dateGroup(timestamp: Long): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        if (sameDay(target, today)) return "Today"
        today.add(Calendar.DAY_OF_YEAR, -1)
        if (sameDay(target, today)) return "Yesterday"
        return DateFormat.format("EEEE, MMM d", Date(timestamp)).toString()
    }

    private fun sameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun formatDuration(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    private fun call(number: String) {
        if (!SipManager.isAccountRegistered()) { Toast.makeText(requireContext(), "Account is offline", Toast.LENGTH_SHORT).show(); return }
        val call = SipManager.makeCall(number) ?: run { Toast.makeText(requireContext(), "Could not start call", Toast.LENGTH_SHORT).show(); return }
        CallActivity.pendingCall = call
        startActivity(Intent(requireContext(), CallActivity::class.java)
            .putExtra(CallActivity.EXTRA_INCOMING, false)
            .putExtra(CallActivity.EXTRA_REMOTE, number))
    }
}
