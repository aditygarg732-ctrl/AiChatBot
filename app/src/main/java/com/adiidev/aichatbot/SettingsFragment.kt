package com.adiidev.aichatbot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val clearBtn = view.findViewById<ImageButton>(R.id.clear_history)
        clearBtn.setOnClickListener {
            // MainActivity ke clearChatFromSettings function ko call kar rahe hain
            (activity as? MainActivity)?.clearChatFromSettings()
            Toast.makeText(requireContext(), "Chat Cleared!", Toast.LENGTH_SHORT).show()
        }

        val logoutBtn = view.findViewById<android.widget.Button>(R.id.logoutBtn)
        logoutBtn.setOnClickListener {
            // MainActivity ke clearChatFromSettings function ko call kar rahe hain
            (activity as? MainActivity)?.logoutFromApp()
            Toast.makeText(requireContext(), "Logged out!", Toast.LENGTH_SHORT).show()
        }
        
        val feedbackBtn = view.findViewById<ImageButton>(R.id.feedback)
        feedbackBtn.setOnClickListener {
            val intent = Intent(requireContext(), ZFeedback::class.java)
            startActivity(intent)
        }

        // Yahan ImageButton use karna hoga kyunki XML mein ye ImageButton hai
        val privacyPolicyBtn = view.findViewById<ImageButton>(R.id.privacy_policy)
        privacyPolicyBtn.setOnClickListener {
            val intent = Intent(requireContext(), ZPrivacyPolicy::class.java)
            startActivity(intent)
        }

        val termsBtn = view.findViewById<ImageButton>(R.id.terms_of_service)
        termsBtn.setOnClickListener {
            val intent = Intent(requireContext(), ZTermsofService::class.java)
            startActivity(intent)
        }
    }
}
