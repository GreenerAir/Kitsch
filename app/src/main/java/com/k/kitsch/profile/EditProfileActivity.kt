package com.k.kitsch.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.k.kitsch.R
import com.k.kitsch.databinding.ActivityEditProfileBinding
import com.k.kitsch.initiation.AuthManager

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    val currentUser = AuthManager.currentUser

    private val bannerImages = arrayOf(
        R.drawable.b1, R.drawable.b2, R.drawable.b3,
        R.drawable.b4, R.drawable.b5, R.drawable.b6,
        R.drawable.b7, R.drawable.b8, R.drawable.b9,
        R.drawable.b10, R.drawable.b11, R.drawable.b12
    )

    private val profileImages = arrayOf(
        R.drawable.a1, R.drawable.a2, R.drawable.a3,
        R.drawable.a4, R.drawable.a5, R.drawable.a6,
        R.drawable.a7, R.drawable.a8, R.drawable.a9,
        R.drawable.a10, R.drawable.a11, R.drawable.a12
    )

    // Current selection indices
    private var currentBannerIndex = 0
    private var currentProfileIndex = 0

    private fun updateUserData(newUsername: String, newBanner: Int, newIcon: Int) {
        val db = FirebaseFirestore.getInstance()

        db.collection("Users").document(currentUser.toString())
            .update("username", newUsername)

        db.collection("Users").document(currentUser.toString())
            .update("pfpBannerId", newBanner)

        db.collection("Users").document(currentUser.toString())
            .update("pfpIconId", newIcon)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Get the current profile picture, and banner passed from ProfileFragment
        val initialBannerIndex = intent.getIntExtra("currentBannerIndex", 0)
        val initialProfileIndex = intent.getIntExtra("currentProfileIndex", 0)

        // Set initial values
        currentBannerIndex = initialBannerIndex
        currentProfileIndex = initialProfileIndex
        binding.pfpBanner.setImageResource(bannerImages[currentBannerIndex])
        binding.pfpIcon.setImageResource(profileImages[currentProfileIndex])

        // Set up the back button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Set up the "Change Banner" button
        findViewById<Button>(R.id.changeBannerButton).setOnClickListener {
            currentBannerIndex = (currentBannerIndex + 1) % bannerImages.size
            binding.pfpBanner.setImageResource(bannerImages[currentBannerIndex])
        }

        // Set up the "Change Profile Picture" button
        findViewById<Button>(R.id.changePfpButton).setOnClickListener {
            currentProfileIndex = (currentProfileIndex + 1) % profileImages.size
            binding.pfpIcon.setImageResource(profileImages[currentProfileIndex])
        }

        // Set up the save button
        binding.saveButton.setOnClickListener {
            val newName = binding.NewNameDisplay.text.toString()
            val resultIntent = Intent().apply {
                updateUserData(newName, currentBannerIndex, currentProfileIndex)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        // Adjust the layout for edge-to-edge support
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


    }
}