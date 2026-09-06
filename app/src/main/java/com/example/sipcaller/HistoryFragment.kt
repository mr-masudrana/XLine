package com.example.sipcaller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class HistoryFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_empty_state, container, false)
        view.findViewById<ImageView>(R.id.emptyStateIcon).setImageResource(R.drawable.ic_history)
        view.findViewById<TextView>(R.id.emptyStateTitle).text = "No call history"
        view.findViewById<TextView>(R.id.emptyStateSubtitle).text =
            "Calls you make and receive will be listed here."
        return view
    }
}
