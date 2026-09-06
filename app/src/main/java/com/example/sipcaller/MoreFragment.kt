package com.example.sipcaller

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import android.widget.TextView

class MoreFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_more, container, false)
        com.example.sipcaller.ui.UiMotion.reveal(view)
        view.findViewById<TextView>(R.id.moreStatusText).text =
            if (SipManager.isAccountRegistered()) "SIP account online • Settings and tools" else "SIP account offline • Check account settings"

        view.findViewById<MaterialCardView>(R.id.cardMyAccount).setOnClickListener {
            com.example.sipcaller.ui.UiMotion.press(view.findViewById(R.id.cardMyAccount))
            startActivity(Intent(requireContext(), MyAccountActivity::class.java))
        }
        view.findViewById<MaterialCardView>(R.id.cardSettings).setOnClickListener {
            com.example.sipcaller.ui.UiMotion.press(view.findViewById(R.id.cardSettings))
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        view.findViewById<MaterialCardView>(R.id.cardHelp).setOnClickListener {
            com.example.sipcaller.ui.UiMotion.press(view.findViewById(R.id.cardHelp))
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }
        view.findViewById<MaterialCardView>(R.id.cardAbout).setOnClickListener {
            com.example.sipcaller.ui.UiMotion.press(view.findViewById(R.id.cardAbout))
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
        view.findViewById<MaterialCardView>(R.id.cardLogout).setOnClickListener {
            com.example.sipcaller.ui.UiMotion.press(view.findViewById(R.id.cardLogout))
            AccountSessionManager.signOut(requireContext())
            val intent = Intent(requireContext(), LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }

        return view
    }
}
