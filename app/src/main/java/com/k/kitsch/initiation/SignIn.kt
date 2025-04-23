package com.k.kitsch.initiation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.k.kitsch.R
import com.k.kitsch.databinding.SigninLayoutBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignIn : AppCompatActivity() {

    private lateinit var loginButton: Button
    lateinit var binding: SigninLayoutBinding
    lateinit var firebaseAuth: FirebaseAuth
    val db = FirebaseFirestore.getInstance()

    fun String.pssValid(): Boolean {
        val hasMay = this.any { it.isUpperCase() }
        val hasMin = this.any { it.isLowerCase() }
        val hasDig = this.any { it.isDigit() }
        val hasEsp = this.any { "!@#$%^&*()-_=+[]{}|;:'\",.<>?/".contains(it) }
        return hasMay && hasMin && hasDig && hasEsp
    }

    fun String.emailValid(): Boolean {
        val has_a = this.any { "@".contains(it) }
        val has_dot = this.any { ".".contains(it) }
        return has_a && has_dot
    }

    suspend fun emailVerificationMethod(email: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            val document = db.collection("Users").document(email).get().await()
            document.exists()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun userIdVerificationMethod(userId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            val querySnapshot = db.collection("Users")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SigninLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loginButton = findViewById(R.id.loginButton)
        firebaseAuth = FirebaseAuth.getInstance()

        binding.signupButton.setOnClickListener {
            val username = binding.username.text.toString()
            val email = binding.email.text.toString()
            val pssw = binding.pssw.text.toString()
            val psswC2 = binding.psswC2.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && pssw.isNotEmpty() && psswC2.isNotEmpty()) {
                if (email.emailValid()) {
                    lifecycleScope.launch {
                        val rep_email = emailVerificationMethod(email)
                        if (!rep_email) {
                            val rep_id = userIdVerificationMethod(username)
                            if (!rep_id) {
                                if (pssw.pssValid()) {
                                    if (pssw == psswC2) {
                                        firebaseAuth.createUserWithEmailAndPassword(email, pssw)
                                            .addOnCompleteListener { authTask ->
                                                if (authTask.isSuccessful) {
                                                    val user = firebaseAuth.currentUser
                                                    user?.sendEmailVerification()
                                                        ?.addOnCompleteListener { verificationTask ->
                                                            if (verificationTask.isSuccessful) {
                                                                createUserInfo(username, email)
                                                                Toast.makeText(
                                                                    this@SignIn,
                                                                    "Verification email sent to $email. Please be a diva! and verify your email before logging in.",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                                startActivity(
                                                                    Intent(
                                                                        this@SignIn,
                                                                        LogIn::class.java
                                                                    )
                                                                )
                                                                finish()
                                                            } else {
                                                                Toast.makeText(
                                                                    this@SignIn,
                                                                    "No email for you, this gurl is busy: ${verificationTask.exception?.message}",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                } else {
                                                    Toast.makeText(
                                                        this@SignIn,
                                                        authTask.exception.toString(),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                    } else {
                                        Toast.makeText(
                                            this@SignIn,
                                            "Gurl, Math ain't mathing, babe!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        this@SignIn,
                                        "Gurl, don't be a hoe, a valid password",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    this@SignIn,
                                    "Gurl, the username already exists",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@SignIn,
                                "Gurl, the email already exists",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Gurl, don't be a hoe put an email", Toast.LENGTH_LONG)
                        .show()
                }
            } else {
                Toast.makeText(this, "Gurl, show your papers, you cunt!", Toast.LENGTH_LONG).show()
            }
        }

        binding.loginButton.setOnClickListener {
            startActivity(Intent(this@SignIn, LogIn::class.java))
            finish()
        }
    }

    private fun createUserInfo(username: String, email: String) {
        val db = FirebaseFirestore.getInstance()
        val userId = "@" + username
        var followers = emptyList<String>()
        var following = listOf("@Kitsch_oficial")
        val pfpBannerId = 0
        val pfpIconId = 0
        val isVerified = false
        val postCounter = 0
        val auraPoints = 0
        val auraTitleId = 0


        val usersData = hashMapOf(
            "email" to email,
            "id" to userId,
            "username" to username,
            "followers" to followers.toList(),
            "following" to following.toList(),
            "pfpBannerId" to pfpBannerId,
            "pfpIconId" to pfpIconId,
            "isVerified" to isVerified,
            "postCounter" to postCounter,
            "auraPoints" to auraPoints,
            "auraTitleId" to auraTitleId,
            "musicVolume" to 80,
            "selectedSong" to "allnight",
        )
        db.collection("Users").document("projectaura360@gmail.com")
            .update("followersCounter", FieldValue.increment(1))
        db.collection("Users").document(email).set(usersData)
    }
}