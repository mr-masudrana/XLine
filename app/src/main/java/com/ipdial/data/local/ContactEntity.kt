package com.ipdial.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ipdial.data.model.Contact
import androidx.core.net.toUri

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val numbers: List<String>,
    val photoUri: String?,
    val isFavorite: Boolean
) {
    fun toContact(): Contact = Contact(
        id = id,
        name = name,
        numbers = numbers,
        photoUri = photoUri?.toUri(),
        isFavorite = isFavorite
    )

    companion object {
        fun fromContact(contact: Contact): ContactEntity = ContactEntity(
            id = contact.id,
            name = contact.name,
            numbers = contact.numbers,
            photoUri = contact.photoUri?.toString(),
            isFavorite = contact.isFavorite
        )
    }
}
