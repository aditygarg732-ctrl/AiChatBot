package com.adiidev.aichatbot

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ZFeedback : AppCompatActivity() {

    private lateinit var etMessage: EditText
    private lateinit var tvCharCount: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var btnSendFeedback: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zfeedback)

        // Initialize Views
        etMessage = findViewById(R.id.etMessage)
        tvCharCount = findViewById(R.id.tvCharCount)
        ratingBar = findViewById(R.id.ratingBar)
        btnSendFeedback = findViewById(R.id.btnSendFeedback)

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Character counter
        etMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                tvCharCount.text = "${s?.length ?: 0}/1000"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Submit button
        btnSendFeedback.setOnClickListener {
            val message = etMessage.text.toString().trim()
            val rating = ratingBar.rating

            // Validation
            if (message.isEmpty()) {
                Toast.makeText(this, "Please write your feedback!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (rating == 0f) {
                Toast.makeText(this, "Please give a rating!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Confirmation Dialog
            AlertDialog.Builder(this)
                .setTitle("Send Feedback?")
                .setMessage("Are you sure you want to submit your feedback?")
                .setPositiveButton("Yes, Send") { dialog, _ ->
                    dialog.dismiss()
                    sendFeedback(message, rating)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun sendFeedback(message: String, rating: Float) {
        val builder = AlertDialog.Builder(this)
        val progressBar = android.widget.ProgressBar(this)
        progressBar.setPadding(30, 30, 30, 30)
        builder.setView(progressBar)
        builder.setMessage("Sending feedback...")
        builder.setCancelable(false)
        val progressDialog = builder.create()
        progressDialog.show()

        // Get Current User Info
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userName = currentUser?.displayName ?: "App User"
        val userEmail = currentUser?.email ?: "Not Provided"

        val json = JSONObject().apply {
            put("service_id", "service_berq51m")
            put("template_id", "template_z5iio8d")
            put("user_id", "T287KwwALFDlztuCU")
            put("accessToken", BuildConfig.EMAILJS_PRIVATE_KEY)
            put("template_params", JSONObject().apply {
                put("user_name", userName)
                put("user_email", userEmail)
                put("message", message)
                put("rating", rating.toInt().toString())
                put("source", "Android App")
            })
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.emailjs.com/api/v1.0/email/send")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("FeedbackError", "Response Code: ${response.code}")
                Log.d("FeedbackError", "Response Body: $responseBody")

                runOnUiThread {
                    progressDialog.dismiss()
                    if (response.isSuccessful) {
                        showThankYouDialog()
                    } else {
                        Toast.makeText(this, "Failed: $responseBody", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showThankYouDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.thankyou_feedback)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Done button
        dialog.findViewById<ImageButton>(R.id.btnDialogOk).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.setCancelable(false)
        dialog.show()
    }
}
