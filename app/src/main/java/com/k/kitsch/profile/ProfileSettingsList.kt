package com.k.kitsch.profile

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.k.kitsch.R
import com.k.kitsch.databinding.ActivitySettingsListBinding
import com.k.kitsch.initiation.AuthManager
import com.k.kitsch.initiation.LogIn
import com.k.kitsch.mainFragments.MusicService
import com.k.kitsch.profile.Settings.AccountDataSettings
import com.k.kitsch.profile.Settings.ProtocolsSettings

class ProfileSettingsList : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsListBinding
    private val sharedPrefs by lazy {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backToProfileButton.setOnClickListener {
            finish()
        }

        binding.account.setOnClickListener {
            startActivity(Intent(this, AccountDataSettings::class.java))
        }

        val areNotificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
        binding.notificationSwitch.isChecked = areNotificationsEnabled
        binding.notificationType.visibility =
            if (areNotificationsEnabled) View.VISIBLE else View.GONE

        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.notificationType.visibility = if (isChecked) View.VISIBLE else View.GONE
            sharedPrefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            Log.d("ProfileSettingsList", "Notifications enabled: $isChecked")
            if (!isChecked) {
                binding.newPostsSwitch.isChecked = false
                binding.ateSwitch.isChecked = false
                binding.commentSwitch.isChecked = false
                binding.followSwitch.isChecked = false
                binding.unfollowSwitch.isChecked = false
            }
        }

        binding.newPostsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("new_posts_notification", isChecked).apply()
            Log.d("ProfileSettingsList", "New posts notification: $isChecked")
        }
        binding.ateSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("ate_notification", isChecked).apply()
            Log.d("ProfileSettingsList", "Ate notification: $isChecked")
        }
        binding.commentSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("comment_notification", isChecked).apply()
            Log.d("ProfileSettingsList", "Comment notification: $isChecked")
        }
        binding.followSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("follow_notification", isChecked).apply()
            Log.d("ProfileSettingsList", "Follow notification: $isChecked")
        }
        binding.unfollowSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("unfollow_notification", isChecked).apply()
            Log.d("ProfileSettingsList", "Unfollow notification: $isChecked")
        }

        binding.newPostsSwitch.isChecked = sharedPrefs.getBoolean("new_posts_notification", true)
        binding.ateSwitch.isChecked = sharedPrefs.getBoolean("ate_notification", true)
        binding.commentSwitch.isChecked = sharedPrefs.getBoolean("comment_notification", true)
        binding.followSwitch.isChecked = sharedPrefs.getBoolean("follow_notification", true)
        binding.unfollowSwitch.isChecked = sharedPrefs.getBoolean("unfollow_notification", true)

        val isMusicEnabled = sharedPrefs.getBoolean("music_enabled", true)
        binding.musicSwitch.isChecked = isMusicEnabled
        MusicService.isMusicEnabled = isMusicEnabled
        Log.d("ProfileSettingsList", "Music enabled: $isMusicEnabled")
        binding.musicSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d("ProfileSettingsList", "Music switch: isChecked=$isChecked")
            MusicService.isMusicEnabled = isChecked
            sharedPrefs.edit().putBoolean("music_enabled", isChecked).apply()
            val musicIntent = Intent(this, MusicService::class.java).apply {
                action = if (isChecked) MusicService.ACTION_PLAY else MusicService.ACTION_STOP
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(musicIntent)
                } else {
                    startService(musicIntent)
                }
                Log.d("ProfileSettingsList", "MusicService action: ${musicIntent.action}")
            } catch (e: Exception) {
                Log.e(
                    "ProfileSettingsList",
                    "Error sending MusicService action: ${musicIntent.action}",
                    e
                )
            }
        }

        val savedVolume = sharedPrefs.getInt("music_volume", 100)
        binding.volumeSeekBar.progress = savedVolume
        MusicService.currentVolume = savedVolume / 100f
        Log.d("ProfileSettingsList", "Volume loaded: $savedVolume")
        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val volumeLevel = progress / 100f
                    Log.d("ProfileSettingsList", "Volume changed: $volumeLevel")
                    MusicService.currentVolume = volumeLevel
                    sharedPrefs.edit().putInt("music_volume", progress).apply()
                    val volumeIntent =
                        Intent(this@ProfileSettingsList, MusicService::class.java).apply {
                            action = MusicService.ACTION_SET_VOLUME
                            putExtra(MusicService.EXTRA_VOLUME_LEVEL, volumeLevel)
                        }
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(volumeIntent)
                        } else {
                            startService(volumeIntent)
                        }
                        Log.d("ProfileSettingsList", "Volume intent sent: $volumeLevel")
                    } catch (e: Exception) {
                        Log.e("ProfileSettingsList", "Error setting volume", e)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val musicOptions = arrayOf("All Night", "Higher")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, musicOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.musicSelectionSpinner.adapter = adapter

        val savedSong = sharedPrefs.getString("selected_song", "allnight")
        val selectedIndex = when (savedSong) {
            "allnight" -> 0
            "higher" -> 1
            else -> 0
        }
        binding.musicSelectionSpinner.setSelection(selectedIndex)
        Log.d("ProfileSettingsList", "Song loaded: $savedSong")

        binding.musicSelectionSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedSong = when (position) {
                        0 -> "allnight"
                        1 -> "higher"
                        else -> "allnight"
                    }
                    Log.d("ProfileSettingsList", "Song selected: $selectedSong")
                    sharedPrefs.edit().putString("selected_song", selectedSong).apply()
                    if (MusicService.isMusicEnabled) {
                        val musicIntent =
                            Intent(this@ProfileSettingsList, MusicService::class.java).apply {
                                action = MusicService.ACTION_PLAY
                            }
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(musicIntent)
                            } else {
                                startService(musicIntent)
                            }
                            Log.d("ProfileSettingsList", "Song change intent sent: $selectedSong")
                        } catch (e: Exception) {
                            Log.e(
                                "ProfileSettingsList",
                                "Error restarting MusicService for song change",
                                e
                            )
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        binding.protocols.setOnClickListener {
            startActivity(Intent(this, ProtocolsSettings::class.java))
        }

        binding.LogOutButt.setOnClickListener {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(AuthManager.currentUser.toString())
                .update("fcmToken", null)
                .addOnCompleteListener {
                    FirebaseAuth.getInstance().signOut()
                    AuthManager.clearLogin()
                    startActivity(Intent(this, LogIn::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileSettingsList", "Failed to clear FCM token", e)
                    FirebaseAuth.getInstance().signOut()
                    AuthManager.clearLogin()
                    startActivity(Intent(this, LogIn::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
        }

        binding.DeleteAccButt.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete") { dialog, _ ->
                    deleteUserAccount()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    private fun deleteUserAccount() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                AuthManager.clearLogin()
                startActivity(Intent(this, LogIn::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Account deletion failed: ${task.exception?.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}