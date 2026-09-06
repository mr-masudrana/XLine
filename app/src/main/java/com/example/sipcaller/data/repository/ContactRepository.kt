package com.example.sipcaller.data.repository

import android.content.Context
import com.example.sipcaller.ContactStore
import com.example.sipcaller.SipContact

/**
 * Central contact access layer. It keeps number matching rules in one place so
 * Contacts, History and Call screens resolve the same SIP identity consistently.
 */
class ContactRepository(private val context: Context) {
    fun phoneContacts(): List<SipContact> = deduplicate(ContactStore.getPhoneContacts(context))

    fun savedContacts(): List<SipContact> = deduplicate(ContactStore.getAll(context))

    fun allContacts(): List<SipContact> = deduplicate(savedContacts() + phoneContacts())

    fun search(query: String): List<SipContact> {
        val q = query.trim()
        if (q.isEmpty()) return allContacts()
        val normalizedQuery = ContactStore.normalize(q)
        return allContacts().filter { contact ->
            contact.name.contains(q, ignoreCase = true) ||
                contact.number.contains(q, ignoreCase = true) ||
                (normalizedQuery.isNotEmpty() && ContactStore.normalize(contact.number).contains(normalizedQuery))
        }
    }

    fun findName(numberOrUri: String): String? {
        val key = ContactStore.normalize(numberOrUri)
        if (key.isBlank()) return null
        return allContacts().firstOrNull { ContactStore.normalize(it.number) == key }?.name
    }

    fun resolveDisplayName(numberOrUri: String): String =
        findName(numberOrUri) ?: ContactStore.normalize(numberOrUri).ifBlank { numberOrUri }

    private fun deduplicate(source: List<SipContact>): List<SipContact> {
        val unique = LinkedHashMap<String, SipContact>()
        source.forEach { contact ->
            val key = ContactStore.normalize(contact.number)
            if (key.isNotBlank() && !unique.containsKey(key)) unique[key] = contact
        }
        return unique.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
}
