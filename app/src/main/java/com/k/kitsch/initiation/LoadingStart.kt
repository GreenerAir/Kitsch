package com.k.kitsch.initiation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.k.kitsch.R
import com.k.kitsch.mainFragments.MainActivity

class LoadingStart : AppCompatActivity() {
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loading_layout)

        // Initialize AuthManager
        AuthManager.initialize(this)

        handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (AuthManager.isLoggedIn()) {
                // User is logged in, go to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } else {
                // User is not logged in, go to LogIn
                startActivity(Intent(this, LogIn::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            finish()
        }, 2000)
    }
}