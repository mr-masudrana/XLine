package com.example.sipcaller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class ChatFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_empty_state, container, false)
        view.findViewById<ImageView>(R.id.emptyStateIcon).setImageResource(R.drawable.ic_chat)
        view.findViewById<TextView>(R.id.emptyStateTitle).text = "Chat coming soon"
        view.findViewById<TextView>(R.id.emptyStateSubtitle).text =
            "SIP messaging (SIMPLE/MESSAGE) isn't wired up yet."
        return view
    }
}
