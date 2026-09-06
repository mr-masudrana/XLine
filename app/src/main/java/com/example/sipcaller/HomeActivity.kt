package com.example.sipcaller

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        updateConnectionStatus()

        if (savedInstanceState == null) {
            showFragment(DialpadFragment(), true)
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dialpad -> DialpadFragment()
                R.id.nav_contacts -> ContactsFragment()
                R.id.nav_history -> HistoryFragment()
                R.id.nav_more -> MoreFragment()
                else -> DialpadFragment()
            }
            showFragment(fragment)
            updateHeader(item.itemId)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateConnectionStatus()
    }

    private fun updateHeader(itemId: Int) {
        val title = findViewById<TextView>(R.id.homeTitle)
        val subtitle = findViewById<TextView>(R.id.homeSubtitle)
        when (itemId) {
            R.id.nav_dialpad -> { title.text = "IP Dial"; subtitle.text = "Ready to call" }
            R.id.nav_contacts -> { title.text = "Contacts"; subtitle.text = "People you can call" }
            R.id.nav_history -> { title.text = "Recents"; subtitle.text = "Your recent calls" }
            R.id.nav_more -> { title.text = "More"; subtitle.text = "Settings and tools" }
        }
    }

    private fun updateConnectionStatus() {
        val pill = findViewById<TextView>(R.id.connectionStatusPill)
        pill.text = if (SipManager.isAccountRegistered()) "● Online" else "○ Offline"
        pill.alpha = if (SipManager.isAccountRegistered()) 1f else .7f
    }

    private fun showFragment(fragment: Fragment, immediate: Boolean = false) {
        if (isFinishing || supportFragmentManager.isStateSaved) return
        val tx = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
        if (immediate) tx.commitNow() else tx.commit()
    }
}
