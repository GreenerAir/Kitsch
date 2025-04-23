package com.k.kitsch.initiation

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.k.kitsch.R
import com.k.kitsch.databinding.LoginLayoutBinding
import com.k.kitsch.mainFragments.MainActivity

class LogIn : AppCompatActivity() {
    private lateinit var binding: LoginLayoutBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.signupButton.setOnClickListener {
            startActivity(Intent(this, SignIn::class.java))
            finish()
        }

        binding.forgotpssw.setOnClickListener { popUpPasswordUpdate() }

        binding.loginButton.setOnClickListener {
            val email = binding.email.text.toString()
            val pssw = binding.pssw.text.toString()

            if (email.isNotEmpty() && pssw.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, pssw)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val user = firebaseAuth.currentUser
                            if (user != null) {
                                if (user.isEmailVerified) {
                                    // Email is verified, proceed to main app
                                    AuthManager.saveLoginState(email)
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "You are no diva! We do not do drugs either discreet shit, you must verify you cunt!",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // Optionally, offer to resend verification email
                                    user.sendEmailVerification()
                                        .addOnCompleteListener { verificationTask ->
                                            if (verificationTask.isSuccessful) {
                                                Toast.makeText(
                                                    this,
                                                    "Verification email resent to $email",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }

                                    firebaseAuth.signOut()
                                }
                            }
                        } else {
                            Toast.makeText(this, authTask.exception.toString(), Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            } else {
                Toast.makeText(this, "Show me your papers, you mayor cunt!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun popUpPasswordUpdate() {
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_forgot_pssw, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            isOutsideTouchable = true
            elevation = 10f
        }

        val send = popupView.findViewById<Button>(R.id.sendEmail)
        val nevermindButton = popupView.findViewById<Button>(R.id.nevermindButton)
        val emailEditText = popupView.findViewById<EditText>(R.id.email)

        send.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isNotEmpty()) {
                firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this@LogIn,
                                "Password reset email sent.",
                                Toast.LENGTH_LONG
                            ).show()
                            popupWindow.dismiss()
                        } else {
                            Toast.makeText(
                                this@LogIn,
                                task.exception?.message ?: "Failed to send reset email",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this@LogIn, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }

        nevermindButton.setOnClickListener {
            popupView.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(300)
                .withEndAction { popupWindow.dismiss() }
                .start()
        }

        popupView.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
}