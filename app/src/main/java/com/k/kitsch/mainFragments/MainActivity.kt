package com.k.kitsch.mainFragments

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.k.kitsch.R
import com.k.kitsch.databinding.ActivityMainBinding
import com.k.kitsch.initiation.AuthManager
import com.k.kitsch.messages.MessagesFragment
import com.k.kitsch.notifications.NotificationsFragment
import com.k.kitsch.postCarp.NewPostFragment
import com.k.kitsch.profile.ProfileFragment
import com.k.kitsch.search.SearchUsersFragment
import com.k.kitsch.wardrobe.WardrobeFragment

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    lateinit var binding: ActivityMainBinding
    val currentUser = AuthManager.currentUser


    override fun onResume() { // Recharge Method
        super.onResume()

        updateDisplayName()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = findViewById(R.id.drawer_layout)

        val sharedPrefs by lazy {
            getSharedPreferences("AppPrefs", MODE_PRIVATE) // Acceso a preferencias compartidas
        }

        val isMusicEnabled = sharedPrefs.getBoolean("music_enabled", true)
        MusicService.isMusicEnabled = isMusicEnabled
        val savedVolume = sharedPrefs.getInt("music_volume", 100)
        MusicService.currentVolume = savedVolume / 100f

        if (isMusicEnabled) {
            startService(Intent(this, MusicService::class.java)) // Lanza MusicService
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.open_nav,
            R.string.close_nav
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set the initial fragment (HomeFragment) if no saved instance state exists
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment(), "slide_right")
            navigationView.setCheckedItem(R.id.nav_home)
        }

        updateDisplayName() // Set the name to the Username on the Database of FireStore
    }

    // This method updates the display name in the navigation drawer header
    fun updateDisplayName() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        val displayNameTextView = headerView.findViewById<TextView>(R.id.displayName)
        val displayImageImageVIew = headerView.findViewById<ImageView>(R.id.displayImage)
        val db = FirebaseFirestore.getInstance()

        db.collection("Users").document(currentUser.toString()).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val dataUsername = document.getString("username")
                    val datadPfpIconId = (document.get("pfpIconId") as? Long)?.toInt() ?: 0

                    dataUsername?.let { displayNameTextView.text = it }
                        ?: Log.e("getUsername", "Username field is null.")

                    displayImageImageVIew.setImageResource(profileImages[datadPfpIconId])

                } else {
                    Log.e("getUsername", "Document does not exist.")
                }
            }
    }

    private val profileImages = arrayOf(
        R.drawable.a1,
        R.drawable.a2,
        R.drawable.a3,
        R.drawable.a4,
        R.drawable.a5,
        R.drawable.a6,
        R.drawable.a7,
        R.drawable.a8,
        R.drawable.a9,
        R.drawable.a10,
        R.drawable.a11,
        R.drawable.a12
    )

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Add a slight scale animation when item is clicked
        item.actionView?.performClick()?.let {
            val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                it,
                PropertyValuesHolder.ofFloat("scaleX", 0.9f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 0.9f, 1f)
            )
            scaleDown.duration = 150
            scaleDown.start()
        }

        when (item.itemId) {
            R.id.nav_home -> replaceFragment(HomeFragment(), "slide_right")
            R.id.nav_messages -> replaceFragment(MessagesFragment(), "slide_right")
            R.id.nav_newpost -> replaceFragment(NewPostFragment(), "slide_right")
            R.id.nav_profile -> replaceFragment(ProfileFragment(), "slide_right")
            R.id.nav_wardrobe -> replaceFragment(WardrobeFragment(), "slide_right")
            R.id.nav_notifications -> replaceFragment(NotificationsFragment(), "slide_right")
            R.id.nav_search -> replaceFragment(SearchUsersFragment(), "slide_right")
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun replaceFragment(fragment: Fragment, animationType: String) {
        val transaction = supportFragmentManager.beginTransaction()

        // Set custom animations based on type
        when (animationType) {
            "slide_right" -> transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )

            "slide_left" -> transaction.setCustomAnimations(
                R.anim.slide_in_left,
                R.anim.slide_out_right,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        }

        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            // Add close animation
            val scaleDown = ObjectAnimator.ofFloat(
                drawerLayout,
                "translationX",
                0f,
                -drawerLayout.width.toFloat()
            )
            scaleDown.duration = 200
            scaleDown.start()
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}