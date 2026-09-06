package com.example.sipcaller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class ContactsFragment : Fragment() {
    private lateinit var list: LinearLayout
    private lateinit var empty: TextView
    private lateinit var scroll: ScrollView
    private lateinit var search: EditText
    private var allContacts: List<SipContact> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_contacts, container, false)
        list = view.findViewById(R.id.contactsList)
        empty = view.findViewById(R.id.contactsEmptyText)
        scroll = view.findViewById(R.id.contactsScroll)
        search = view.findViewById(R.id.contactsSearch)
        search.addTextChangedListener(SimpleTextWatcher { render() })

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), CONTACTS_PERMISSION)
        } else loadContacts()
        return view
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CONTACTS_PERMISSION) loadContacts()
    }

    override fun onResume() { super.onResume(); if (::list.isInitialized && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) loadContacts() }

    private fun loadContacts() {
        allContacts = ContactStore.getPhoneContacts(requireContext())
        render()
    }

    private fun render() {
        val q = if (::search.isInitialized) search.text.toString().trim() else ""
        val contacts = allContacts.filter { it.name.contains(q, true) || it.number.contains(q, true) }
        list.removeAllViews()
        empty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        scroll.visibility = if (contacts.isEmpty()) View.GONE else View.VISIBLE
        empty.text = when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED -> "Contacts permission is required\nAllow permission to show phone contacts"
            q.isNotBlank() -> "No matching contacts"
            else -> "No phone contacts found"
        }
        contacts.forEach { contact ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 22, 24, 22)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            row.addView(TextView(requireContext()).apply { text = contact.name; textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD) })
            row.addView(TextView(requireContext()).apply { text = contact.number; textSize = 15f })
            row.setOnClickListener { call(contact.number) }
            list.addView(row)
        }
    }

    private fun call(number: String) {
        if (!SipManager.isAccountRegistered()) { Toast.makeText(requireContext(), "Account is offline", Toast.LENGTH_SHORT).show(); return }
        val call = SipManager.makeCall(number) ?: run { Toast.makeText(requireContext(), "Could not start call", Toast.LENGTH_SHORT).show(); return }
        CallActivity.pendingCall = call
        startActivity(Intent(requireContext(), CallActivity::class.java)
            .putExtra(CallActivity.EXTRA_INCOMING, false)
            .putExtra(CallActivity.EXTRA_REMOTE, number))
    }

    companion object { private const val CONTACTS_PERMISSION = 301 }
}
