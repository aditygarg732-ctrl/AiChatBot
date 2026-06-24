package com.adiidev.aichatbot

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val sessionList = mutableListOf<ChatSession>()
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView setup
        recyclerView = view.findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistoryAdapter(sessionList) { session ->
            // Pass selected session back to MainActivity
            (activity as? MainActivity)?.loadChatFromHistory(session)
        }
        recyclerView.adapter = adapter

        loadHistory()
    }

    private fun loadHistory() {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (uid == null) {
            android.widget.Toast.makeText(requireContext(), "User not logged in", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(uid).collection("sessions")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    android.widget.Toast.makeText(requireContext(), "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    sessionList.clear()
                    for (doc in snapshots) {
                        val isHidden = doc.getBoolean("isHidden") ?: false
                        if (!isHidden) {
                            val title = doc.getString("title") ?: "No Title"
                            val timestamp = try {
                                doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                            } catch (ex: Exception) {
                                System.currentTimeMillis()
                            }
                            
                            // Extract messages from doc
                            @Suppress("UNCHECKED_CAST")
                            val messagesMap = doc.get("messages") as? List<Map<String, Any>> ?: emptyList()
                            val messages = messagesMap.map { 
                                Message(it["text"] as? String ?: "", it["isUser"] as? Boolean ?: false)
                            }
                            
                            sessionList.add(ChatSession(doc.id, title, timestamp, messages))
                        }
                    }
                    sessionList.sortByDescending { it.timestamp }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}