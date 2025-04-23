package com.k.kitsch.profile.Settings

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.k.kitsch.R
import com.k.kitsch.databinding.AccountDataBinding
import com.k.kitsch.initiation.AuthManager


class AccountDataSettings : AppCompatActivity() {

    private lateinit var binding: AccountDataBinding
    val currentUser = AuthManager.currentUser

    private val auraBanners = listOf(
        R.drawable.c1, R.drawable.c2, R.drawable.c3, R.drawable.c4,
        R.drawable.c5, R.drawable.c6, R.drawable.c7, R.drawable.c8,
        R.drawable.c9, R.drawable.c10, R.drawable.c11, R.drawable.c12,
        R.drawable.c13, R.drawable.c14
    )

    override fun onResume() {
        super.onResume()
        enableEdgeToEdge()

        Data()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = AccountDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Going back to the settings menu
        binding.backToProfileButton.setOnClickListener {
            finish()
        }

        Data()

    }

    private fun Data() {
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(currentUser.toString())
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {

                    // Updating Title Value
                    var counter = document.getLong("auraPoints")?.toInt()
                    auraCalculator(counter)
                    val auraIndex = ((document.getLong("auraTitleId") ?: 1L).toInt() - 1).coerceIn(
                        auraBanners.indices
                    )

                    binding.auraScores.text = document.getLong("auraPoints").toString()
                    binding.auraTitle.setImageResource(auraBanners[auraIndex])

                    binding.accountEmail.text = document.getString("email") ?: ""
                    binding.accountId.text = document.getString("id") ?: ""
                    binding.accountUsername.text = document.getString("username") ?: ""
                    binding.verifiedBadge.visibility =
                        if (document.getBoolean("isVerified") == true) View.VISIBLE else View.GONE

                    binding.totalPosts.text = document.getLong("postCounter").toString()
                    binding.totalWardrobe.text = document.getLong("wardrobeItemCount").toString()

                    val followersList = document.get("followers") as? List<*>
                    binding.totalFollowers.text = document.getLong("followersCounter").toString()
                    binding.followersList.text = followersList?.joinToString(", ") ?: "No followers"

                    val followingList = document.get("following") as? List<*>
                    binding.totalFollowing.text = document.getLong("followingCounter").toString()
                    binding.followingList.text = followingList?.joinToString(", ") ?: "No following"

                } else {
                    Log.e("User data", "User document not found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("No enough data", "Error loading user data", e)
            }
    }

    private fun auraCalculator(auras: Int?) {
        val db = FirebaseFirestore.getInstance()

        var Ptitle: Int? = null
        if (auras != null) {
            Ptitle = when {
                auras == 0 -> 1
                auras < 1000 -> 1
                auras < 2000 -> 2
                auras < 4000 -> 3
                auras < 8000 -> 4
                auras < 16000 -> 5
                auras < 24000 -> 6
                auras < 30000 -> 7
                auras < 40000 -> 8
                auras < 50000 -> 9
                auras < 60000 -> 10
                auras < 75000 -> 11
                auras < 95000 -> 12
                auras < 101000 -> 13
                auras >= 101000 -> 14
                else -> null
            }
        }
        db.collection("Users").document(currentUser.toString()).update("auraTitleId", Ptitle)
    }

}