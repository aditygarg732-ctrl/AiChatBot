package com.adiidev.aichatbot

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Adapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth
import org.w3c.dom.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.adiidev.aichatbot.BuildConfig

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")

    private lateinit var recycleView : RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var messageList : MutableList<Message>

    private lateinit var editText: EditText

    private lateinit var welcomeImage: ImageView

    private lateinit var helloText: TextView

    private lateinit var startingText: TextView

    private lateinit var drawerLayout: DrawerLayout

    private lateinit var thinkingHand: ImageView

    private var currentSessionId: String? = null
    
    private val typingHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var typingRunnable: Runnable? = null

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Safety check: if somehow user reaches here without login
        if (FirebaseAuth.getInstance().currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            // Hum bottom padding ko keyboard ki height (ime.bottom) ke barabar set karenge.
            // Agar keyboard band hai, toh navigation bar (systemBars.bottom) jitna padding rahega.
            val bottomPadding = if (ime.bottom > 0) ime.bottom else systemBars.bottom
            
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }
        val menuBtn = findViewById<ImageButton>(R.id.menuButton)
        val newChatBtn = findViewById<ImageButton>(R.id.newChatButton)
        val newChatBtn2 = findViewById<ImageButton>(R.id.newChatMenu)

        val sendBtn = findViewById<ImageButton>(R.id.sendBtn)

        drawerLayout = findViewById(R.id.drawerLayout)
        val historyOption = findViewById<ImageButton>(R.id.historyOption)
        val settingOption = findViewById<ImageButton>(R.id.settingsOption)


        welcomeImage = findViewById(R.id.ImageView)
        helloText = findViewById(R.id.hello)
        startingText = findViewById(R.id.StartText)
        thinkingHand = findViewById(R.id.thinkingHand)


        menuBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
            supportFragmentManager.beginTransaction().replace(
                R.id.drawerFragmentContainer,
                HistoryFragment()
            ).commit()
            historyOption.setImageResource(R.drawable.red_history)
            historyOption.scaleX = 1.2f
            historyOption.scaleY = 1.2f
            
            settingOption.setImageResource(R.drawable.settings_btn)
            settingOption.scaleX = 1.0f
            settingOption.scaleY = 1.0f
        }
        newChatBtn.setOnClickListener {
            startNewChat()
        }

        editText = findViewById(R.id.message_edittext)

        sendBtn.setOnClickListener {
            val text = editText.text.toString()
            if (text.isNotEmpty()) {
                // Show and rotate small hand icon
                thinkingHand.visibility = View.VISIBLE 
                startThinkingAnimation()
                
                welcomeImage.visibility = View.GONE
                helloText.visibility = View.GONE
                startingText.visibility = View.GONE
                recycleView.visibility = View.VISIBLE

                messageList.add(Message(text, true))
                adapter.notifyItemInserted(messageList.size - 1)
                recycleView.scrollToPosition(messageList.size - 1)
                
                // Save immediately to get an ID and show in History
                saveChatSessionToFirestore()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = GeminiRequest(
                            contents = listOf(
                                Content(
                                    parts = listOf(
                                        Part(text)
                                    )
                                )
                            )
                        )

                        val response = RetrofitClient.api.generateContent(
                            BuildConfig.GEMINI_API_KEY,
                            request
                        )

                        if (response.isSuccessful) {
                            val aiReply = response.body()
                                ?.candidates
                                ?.firstOrNull()
                                ?.content
                                ?.parts
                                ?.firstOrNull()
                                ?.text ?: "No response from AI"

                            withContext(Dispatchers.Main) {
                                stopThinkingAnimation()
                                thinkingHand.visibility = View.GONE

                                // Call the new smooth typewriter effect
                                simulateTypewriterEffect(aiReply)
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            withContext(Dispatchers.Main) {
                                stopThinkingAnimation()
                                thinkingHand.visibility = View.GONE
                                val errorCode = response.code()
                                val userMessage = when(errorCode) {
                                    503 -> "AI Server is busy. Please try again in a moment."
                                    429 -> "Too many requests. Please wait a bit."
                                    else -> "Error $errorCode: Something went wrong"
                                }
                                messageList.add(Message(userMessage, false))
                                adapter.notifyItemInserted(messageList.size - 1)
                                android.util.Log.e("GeminiAPI", "Error Code: $errorCode, Body: $errorBody")
                            }
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            messageList.add(
                                Message("Error: ${e.message}", false)
                            )
                            adapter.notifyItemInserted(messageList.size - 1)
                            stopThinkingAnimation()
                            thinkingHand.visibility = View.GONE
                        }
                    }
                }
                editText.text.clear()
            }
        }
        
        historyOption.setOnClickListener {
            supportFragmentManager.beginTransaction().replace(
                R.id.drawerFragmentContainer,
                HistoryFragment()
            ).commit()
            historyOption.setImageResource(R.drawable.red_history)
            historyOption.scaleX = 1.2f
            historyOption.scaleY = 1.2f
            
            settingOption.setImageResource(R.drawable.settings_btn)
            settingOption.scaleX = 1.0f
            settingOption.scaleY = 1.0f
        }

        settingOption.setOnClickListener {
            supportFragmentManager.beginTransaction().replace(
                R.id.drawerFragmentContainer,
                SettingsFragment()
            ).commit()
            historyOption.setImageResource(R.drawable.history_btn)
            historyOption.scaleX = 1.0f
            historyOption.scaleY = 1.0f
            
            settingOption.setImageResource(R.drawable.red_settings)
            settingOption.scaleX = 1.2f
            settingOption.scaleY = 1.2f
        }

        recycleView = findViewById(R.id.chatRecyclerView)
        messageList = mutableListOf()
        adapter = MessageAdapter(messageList)

        recycleView.adapter = adapter
        recycleView.layoutManager = LinearLayoutManager(this)


        newChatBtn.setOnClickListener {
            startNewChat()
        }

        newChatBtn2.setOnClickListener {
            startNewChat()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    fun clearChatFromSettings() {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid
        if (uid != null) {
            // 1. Firestore mein saari sessions ko "hidden" mark karein
            db.collection("users").document(uid).collection("sessions")
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        db.collection("users").document(uid).collection("sessions").document(doc.id)
                            .update("isHidden", true)
                    }
                }
        }

        // 2. Current screen clear karein
        messageList.clear()
        adapter.notifyDataSetChanged()

        welcomeImage.visibility = View.VISIBLE
        helloText.visibility = View.VISIBLE
        startingText.visibility = View.VISIBLE
        recycleView.visibility = View.GONE
        editText.text.clear()
        
        drawerLayout.closeDrawer(GravityCompat.START)
    }
    
    fun logoutFromApp() {
        FirebaseAuth.getInstance().signOut()
        val credentialManager = CredentialManager.create(this)
        lifecycleScope.launch {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    fun loadChatFromHistory(session: ChatSession) {
        // Agar wahi chat hai jo abhi chal rahi hai, toh stop mat karo
        if (session.id == currentSessionId) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }
        
        stopTypewriterEffect() // Stop only if switching to a DIFFERENT chat
        
        currentSessionId = session.id
        messageList.clear()
        messageList.addAll(session.messages)
        adapter.notifyDataSetChanged()

        welcomeImage.visibility = View.GONE
        helloText.visibility = View.GONE
        startingText.visibility = View.GONE
        recycleView.visibility = View.VISIBLE
        
        recycleView.scrollToPosition(messageList.size - 1)
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun startNewChat() {
        stopTypewriterEffect() // Stop any ongoing typing
        
        currentSessionId = null
        messageList.clear()
        adapter.notifyDataSetChanged()

        welcomeImage.visibility = View.VISIBLE
        helloText.visibility = View.VISIBLE
        startingText.visibility = View.VISIBLE
        recycleView.visibility = View.GONE
        
        editText.text.clear()
    }

    private fun stopTypewriterEffect() {
        typingRunnable?.let { typingHandler.removeCallbacks(it) }
        typingRunnable = null
        stopThinkingAnimation()
        thinkingHand.visibility = View.GONE
    }

    private fun saveChatSessionToFirestore() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (messageList.isEmpty()) return

        val uid = user.uid
        val firstUserMessage = messageList.firstOrNull { it.isUser }?.text ?: "New Chat"

        val sessionData = hashMapOf(
            "title" to if (firstUserMessage.length > 30) firstUserMessage.take(30) + "..." else firstUserMessage,
            "messages" to messageList.map { mapOf("text" to it.text, "isUser" to it.isUser) },
            "timestamp" to FieldValue.serverTimestamp(),
            "isHidden" to false
        )

        val sessionsRef = db.collection("users").document(uid).collection("sessions")
        
        if (currentSessionId == null) {
            sessionsRef.add(sessionData)
                .addOnSuccessListener { docRef ->
                    currentSessionId = docRef.id
                    // Toast.makeText(this, "Session created", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            sessionsRef.document(currentSessionId!!).set(sessionData)
        }
    }

    private fun simulateTypewriterEffect(fullText: String) {
        val message = Message("", false)
        messageList.add(message)
        val position = messageList.size - 1
        adapter.notifyItemInserted(position)

        var charIndex = 0

        typingRunnable = object : Runnable {
            override fun run() {
                if (charIndex <= fullText.length) {
                    val currentText = fullText.substring(0, charIndex)
                    val displayText = if (charIndex < fullText.length) "$currentText▎" else currentText
                    
                    // Check if index is still valid to avoid crash
                    if (position < messageList.size) {
                        messageList[position] = Message(displayText, false)
                        
                        val viewHolder = recycleView.findViewHolderForAdapterPosition(position) as? MessageAdapter.MessageViewHolder
                        if (viewHolder != null) {
                            viewHolder.messageText.text = displayText
                        } else {
                            adapter.notifyItemChanged(position)
                        }

                        if (recycleView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                            val layoutManager = recycleView.layoutManager as? LinearLayoutManager
                            if (layoutManager != null && layoutManager.findLastVisibleItemPosition() >= position - 1) {
                                recycleView.scrollToPosition(position)
                            }
                        }
                    }
                    
                    charIndex++
                    typingHandler.postDelayed(this, 20)
                } else {
                    typingRunnable = null
                    saveChatSessionToFirestore()
                }
            }
        }
        typingHandler.post(typingRunnable!!)
    }

    private lateinit var animator: ObjectAnimator

    private fun startThinkingAnimation() {
        animator = ObjectAnimator.ofFloat(
            thinkingHand,
            "rotationY",
            0f,
            360f
        ).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun stopThinkingAnimation() {
        thinkingHand.clearAnimation()
    }
}