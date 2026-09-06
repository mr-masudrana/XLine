package com.example.sipcaller

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.sipcaller.data.repository.ContactRepository

class ContactsFragment : Fragment() {
    private lateinit var list: LinearLayout
    private lateinit var empty: TextView
    private lateinit var scroll: ScrollView
    private lateinit var search: EditText
    private var allContacts: List<SipContact> = emptyList()
    private var contactRepository: ContactRepository? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_contacts, container, false) 
        com.example.sipcaller.ui.UiMotion.reveal(view)
        list = view.findViewById(R.id.contactsList)
        empty = view.findViewById(R.id.contactsEmptyText)
        scroll = view.findViewById(R.id.contactsScroll)
        search = view.findViewById(R.id.contactsSearch)
        search.addTextChangedListener(SimpleTextWatcher { render() })
        view.findViewById<Button>(R.id.addContactButton).setOnClickListener { showContactEditor(null) }

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
        contactRepository = ContactRepository(requireContext())
        allContacts = contactRepository?.allContacts().orEmpty()
        render()
    }

    private fun render() {
        val q = if (::search.isInitialized) search.text.toString().trim() else ""
        val contacts = contactRepository?.search(q) ?: allContacts.filter { it.name.contains(q, true) || it.number.contains(q, true) }
        list.removeAllViews()
        empty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        scroll.visibility = if (contacts.isEmpty()) View.GONE else View.VISIBLE
        empty.text = when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED -> "Contacts permission is required\nAllow permission to show phone contacts"
            q.isNotBlank() -> "No matching contacts"
            else -> "No contacts found"
        }
        contacts.forEach { contact ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(18, 16, 10, 16)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            val initial = TextView(requireContext()).apply {
                text = contact.name.take(1).uppercase().ifBlank { "?" }
                textSize = 20f; gravity = android.view.Gravity.CENTER
                background = context.getDrawable(R.drawable.bg_avatar)
                layoutParams = LinearLayout.LayoutParams(48, 48)
            }
            val info = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 0, 8, 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            info.addView(TextView(requireContext()).apply { text = contact.name; textSize = 17f; setTypeface(null, android.graphics.Typeface.BOLD) })
            info.addView(TextView(requireContext()).apply { text = contact.number; textSize = 13f; alpha = .68f })
            val callButton = Button(requireContext()).apply {
                text = "Call"
                contentDescription = "Call ${contact.name}"
                setOnClickListener { call(contact.number) }
            }
            row.addView(initial); row.addView(info); row.addView(callButton)
            row.setOnClickListener { showContactActions(contact) }
            list.addView(row)
            list.addView(View(requireContext()).apply { layoutParams = LinearLayout.LayoutParams(1, 1) })
        }
    }

    private fun showContactActions(contact: SipContact) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(contact.name)
            .setMessage(contact.number)
            .setPositiveButton("Call") { _, _ -> call(contact.number) }
            .setNegativeButton("Edit") { _, _ -> showContactEditor(contact) }
            .setNeutralButton("Delete") { _, _ ->
                ContactStore.delete(requireContext(), contact.number)
                loadContacts()
            }.show()
    }

    private fun showContactEditor(existing: SipContact?) {
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 12, 48, 0)
        }
        val name = EditText(requireContext()).apply { hint = "Name"; setText(existing?.name.orEmpty()) }
        val number = EditText(requireContext()).apply { hint = "SIP number"; inputType = android.text.InputType.TYPE_CLASS_PHONE; setText(existing?.number.orEmpty()) }
        box.addView(name); box.addView(number)
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(if (existing == null) "Add contact" else "Edit contact")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val n = name.text.toString().trim()
                val no = number.text.toString().trim()
                if (n.isNotBlank() && no.isNotBlank()) {
                    if (existing != null) ContactStore.delete(requireContext(), existing.number)
                    ContactStore.save(requireContext(), SipContact(n, no))
                    loadContacts()
                } else Toast.makeText(requireContext(), "Enter name and number", Toast.LENGTH_SHORT).show()
            }.show()
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
