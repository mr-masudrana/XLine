package com.ipdial.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Contact(
    val id: String,
    val name: String,
    val numbers: List<String>,
    val photoUri: Uri? = null,
    val isFavorite: Boolean = false
)
