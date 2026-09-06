package com.example.sipcaller

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment

class ContactsFragment : Fragment() {
    private lateinit var list: LinearLayout
    private lateinit var empty: TextView
    private lateinit var scroll: ScrollView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_contacts, container, false)
        list = view.findViewById(R.id.contactsList)
        empty = view.findViewById(R.id.contactsEmptyText)
        scroll = view.findViewById(R.id.contactsScroll)
        view.findViewById<Button>(R.id.addContactButton).setOnClickListener { showAddDialog() }
        render()
        return view
    }

    override fun onResume() { super.onResume(); if (::list.isInitialized) render() }

    private fun render() {
        val contacts = ContactStore.getAll(requireContext())
        list.removeAllViews()
        empty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        scroll.visibility = if (contacts.isEmpty()) View.GONE else View.VISIBLE
        contacts.forEach { contact ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL; setPadding(16, 20, 16, 20)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            row.addView(TextView(requireContext()).apply { text = contact.name; textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD) })
            row.addView(TextView(requireContext()).apply { text = contact.number; textSize = 15f })
            row.setOnClickListener { call(contact.number) }
            row.setOnLongClickListener {
                AlertDialog.Builder(requireContext()).setTitle(contact.name).setItems(arrayOf("Call", "Delete")) { _, which ->
                    if (which == 0) call(contact.number) else { ContactStore.delete(requireContext(), contact.number); render() }
                }.show(); true
            }
            list.addView(row)
        }
    }

    private fun showAddDialog() {
        val box = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 0) }
        val name = EditText(requireContext()).apply { hint = "Name" }
        val number = EditText(requireContext()).apply { hint = "SIP number"; inputType = InputType.TYPE_CLASS_PHONE }
        box.addView(name); box.addView(number)
        AlertDialog.Builder(requireContext()).setTitle("Add Contact").setView(box).setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val n = name.text.toString().trim(); val p = number.text.toString().trim()
                if (n.isNotEmpty() && p.isNotEmpty()) { ContactStore.save(requireContext(), SipContact(n, p)); render() }
            }.show()
    }

    private fun call(number: String) {
        if (!SipManager.isAccountRegistered()) { Toast.makeText(requireContext(), "Account is offline", Toast.LENGTH_SHORT).show(); return }
        val call = SipManager.makeCall(number) ?: run { Toast.makeText(requireContext(), "Could not start call", Toast.LENGTH_SHORT).show(); return }
        CallActivity.pendingCall = call
        startActivity(Intent(requireContext(), CallActivity::class.java).putExtra(CallActivity.EXTRA_INCOMING, false).putExtra(CallActivity.EXTRA_REMOTE, number))
    }
}
