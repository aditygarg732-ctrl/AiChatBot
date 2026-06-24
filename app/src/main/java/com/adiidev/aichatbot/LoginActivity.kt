package com.adiidev.aichatbot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager

    private lateinit var drawerLayout: DrawerLayout

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        credentialManager = CredentialManager.create(this)

        val emailEt = findViewById<EditText>(R.id.emailEt)
        val passwordEt = findViewById<EditText>(R.id.passwordEt)
        val emailEtReg = findViewById<EditText>(R.id.emailEtReg)
        val passwordEtReg = findViewById<EditText>(R.id.passwordEtReg)
        val passwordConfEtReg = findViewById<EditText>(R.id.passwordConfEtReg)
        val registerBtn = findViewById<Button>(R.id.toggleRegisterTv)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val googleBtn = findViewById<com.google.android.gms.common.SignInButton>(R.id.googleSignInBtn)

        drawerLayout = findViewById(R.id.drawerLayoutLogin)

        val loginBtnDrawer = findViewById<Button>(R.id.loginBtnOpenDrawer)
        loginBtnDrawer.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val registrationBtnDrawer = findViewById<Button>(R.id.toggleRegisterTvD)
        registrationBtnDrawer.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        registerBtn.setOnClickListener {
            val email = emailEtReg.text.toString()
            val password = passwordEtReg.text.toString()
            val passwordConf = passwordConfEtReg.text.toString()
            if(password != passwordConf){
                Toast.makeText(this, "password not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isEmpty() || password.isEmpty()) return@setOnClickListener

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    saveUserToFirestore()
                    Toast.makeText(this, "Registered", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
        }

        loginBtn.setOnClickListener {
            val email = emailEt.text.toString()
            val password = passwordEt.text.toString()
            if (email.isEmpty() || password.isEmpty()) return@setOnClickListener

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
        }

        googleBtn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false) // Physical device par account picker force karne ke liye
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                // Clear state carefully
                credentialManager.clearCredentialState(ClearCredentialStateRequest())

                val result = credentialManager.getCredential(this@LoginActivity, request)
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                // Exact error type logcat mein dikhega (e.g. Type: com.google.android.libraries.identity.googleid.TYPE_NO_CREDENTIAL)
                android.util.Log.e("LoginActivity", "Credential Manager Error Type: ${e.type}")
                android.util.Log.e("LoginActivity", "Credential Manager Error Message: ${e.message}")
                
                val errorMessage = when (e.type) {
                    "com.google.android.libraries.identity.googleid.TYPE_NO_CREDENTIAL" -> 
                        "No Google Account found. Please check your phone settings."
                    else -> "Google Sign-In Error: ${e.message}"
                }
                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.util.Log.e("LoginActivity", "General Error: ${e.message}")
                Toast.makeText(this@LoginActivity, "Unexpected Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignIn(result: androidx.credentials.GetCredentialResponse) {
        val credential = result.credential
        if (credential is com.google.android.libraries.identity.googleid.GoogleIdTokenCredential) {
            val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
            auth.signInWithCredential(firebaseCredential)
                .addOnSuccessListener {
                    saveUserToFirestore()
                    Toast.makeText(this, "Google Login Success", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserToFirestore() {
        val user = auth.currentUser ?: return
        val userMap = hashMapOf(
            "uid" to user.uid,
            "name" to user.displayName,
            "email" to user.email
        )
        firestore.collection("users")
            .document(user.uid)
            .set(userMap)
    }
}