package com.example.sipcaller

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class MoreFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_more, container, false)

        view.findViewById<MaterialCardView>(R.id.cardMyAccount).setOnClickListener {
            startActivity(Intent(requireContext(), MyAccountActivity::class.java))
        }
        view.findViewById<MaterialCardView>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        view.findViewById<MaterialCardView>(R.id.cardHelp).setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }
        view.findViewById<MaterialCardView>(R.id.cardAbout).setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }

        return view
    }
}
