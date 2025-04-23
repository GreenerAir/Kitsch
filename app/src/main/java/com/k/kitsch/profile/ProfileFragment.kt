package com.k.kitsch.profile

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.k.kitsch.R
import com.k.kitsch.databinding.FragmentProfileBinding
import com.k.kitsch.initiation.AuthManager
import com.k.kitsch.postCarp.PostItem

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val posts = mutableListOf<PostItem>()
    private lateinit var postsAdapter: ProfilePostsAdapter
    val currentUser = AuthManager.currentUser

    // Image resources
    private val profileImages = listOf(
        R.drawable.a1, R.drawable.a2, R.drawable.a3, R.drawable.a4,
        R.drawable.a5, R.drawable.a6, R.drawable.a7, R.drawable.a8,
        R.drawable.a9, R.drawable.a10, R.drawable.a11, R.drawable.a12
    )

    private val bannerImages = listOf(
        R.drawable.b1, R.drawable.b2, R.drawable.b3, R.drawable.b4,
        R.drawable.b5, R.drawable.b6, R.drawable.b7, R.drawable.b8,
        R.drawable.b9, R.drawable.b10, R.drawable.b11, R.drawable.b12
    )

    private val auraBanners = listOf(
        R.drawable.c1, R.drawable.c2, R.drawable.c3, R.drawable.c4,
        R.drawable.c5, R.drawable.c6, R.drawable.c7, R.drawable.c8,
        R.drawable.c9, R.drawable.c10, R.drawable.c11, R.drawable.c12,
        R.drawable.c13, R.drawable.c14
    )

    private var currentBannerIndex = 0
    private var currentProfileIndex = 0
    private var currentUserId: String? = null

    override fun onResume() {
        super.onResume()
        loadUserData() // This will trigger posts loading when user data is ready
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.auraScores.setOnClickListener { showAuraPointsPopup() }

        setupPostsGrid()
        setupButtons()
        loadUserData() // Initial load
    }

    private fun setupButtons() {
        binding.editPfButton.setOnClickListener {
            startActivity(Intent(activity, EditProfileActivity::class.java))
        }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(activity, ProfileSettingsList::class.java))
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

    private fun loadUserData() {
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(currentUser.toString())
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUserId = document.getString("id") ?: document.id
                    binding.userName.text = currentUserId

                    // Updating Title Value
                    var counter = document.getLong("auraPoints")?.toInt()
                    auraCalculator(counter)

                    val followersCount = document.getLong("followersCounter") ?: 0L
                    val followingCount = document.getLong("followingCounter") ?: 0L

                    binding.totalPosts.text = document.getLong("postCounter").toString()
                    binding.displayName.text = document.getString("username") ?: ""

                    binding.totalFollowers.text = followersCount.toString()
                    binding.totalFollowing.text = followingCount.toString()

                    val bannerIndex = (document.getLong("pfpBannerId") ?: 0L).toInt()
                        .coerceIn(bannerImages.indices)
                    val profileIndex = (document.getLong("pfpIconId") ?: 0L).toInt()
                        .coerceIn(profileImages.indices)
                    val auraIndex = ((document.getLong("auraTitleId") ?: 1L).toInt() - 1).coerceIn(
                        auraBanners.indices
                    )


                    binding.pfpBanner.setImageResource(bannerImages[bannerIndex])
                    binding.pfpIcon.setImageResource(profileImages[profileIndex])
                    binding.auraScores.setImageResource(auraBanners[auraIndex])


                    currentBannerIndex = bannerIndex
                    currentProfileIndex = profileIndex

                    loadUserPosts() // Load posts after we have the user ID
                } else {
                    Log.e("ProfileFragment", "User document not found")
                    loadDefaultPosts()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error loading user data", e)
                loadDefaultPosts()
            }
    }

    private fun setupPostsGrid() {
        postsAdapter = ProfilePostsAdapter(requireContext(), posts)
        binding.postsGrid.adapter = postsAdapter
    }

    private fun loadUserPosts() {
        currentUserId?.let { userId ->
            posts.clear()
            FirebaseFirestore.getInstance()
                .collection("Posts")
                .whereEqualTo("username", userId)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        loadDefaultPosts()
                    } else {
                        processFirestoreDocuments(documents)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Error loading posts", e)
                    loadDefaultPosts()
                }
        } ?: run {
            Log.w("ProfileFragment", "No user ID available to load posts")
            loadDefaultPosts()
        }
    }

    private fun processFirestoreDocuments(documents: QuerySnapshot) {
        try {
            posts.clear()
            documents.forEach { document ->
                val profileImageIndex = (document.getLong("profileImageId") ?: 1L).toInt() - 1
                val safeIndex = profileImageIndex.coerceIn(0, 11) // 0-11 for 12 images

                posts.add(
                    PostItem(
                        postId = "",
                        profileImage = safeIndex,
                        username = document.getString("UserId") ?: "anonymous",
                        isVerified = document.getBoolean("isVerified") ?: false,
                        postImageData = document.getString("postImageData"),
                        likesCount = document.getLong("likesCounter")?.toInt() ?: 0,
                        caption = document.getString("Caption") ?: "",
                        likedBy = emptyList(),
                    )
                )
            }
            postsAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error processing documents", e)
            loadDefaultPosts()
        }
    }

    private fun showAuraPointsPopup() {
        val popupView =
            LayoutInflater.from(requireContext()).inflate(R.layout.popup_aurapoints_userlevel, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            isOutsideTouchable = true
        }

        // Get references to views
        val auraScores = popupView.findViewById<TextView>(R.id.auraScores)
        val auraTitle = popupView.findViewById<ImageView>(R.id.auraTitle)
        val rankProgressBar = popupView.findViewById<ProgressBar>(R.id.rankProgressBar)
        val btnClosePopup = popupView.findViewById<Button>(R.id.btnClosePopup)
        val levelsContainer = popupView.findViewById<LinearLayout>(R.id.levelsContainer)

        // Level thresholds from your table
        val levelThresholds = listOf(
            0,       // Level 1
            1000,    // Level 2
            2000,    // Level 3
            4000,    // Level 4
            8000,    // Level 5
            16000,   // Level 6
            24000,   // Level 7
            30000,   // Level 8
            40000,   // Level 9
            50000,   // Level 10
            60000,   // Level 11
            75000,   // Level 12
            95000,   // Level 13
            101000   // Level 14
        )

        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(currentUser.toString())
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val auraPoints = document.getLong("auraPoints")?.toInt() ?: 0
                    val currentTitleId = (document.getLong("auraTitleId") ?: 1L).toInt()
                    val currentLevelIndex = currentTitleId - 1 // Convert to 0-based index

                    // Update current level display
                    val currentLevelResId = when (currentTitleId) {
                        in 1..14 -> auraBanners[currentLevelIndex]
                        else -> auraBanners[0]
                    }
                    auraTitle.setImageResource(currentLevelResId)
                    auraScores.text = "$auraPoints pts"

                    // Calculate progress to next level
                    if (currentTitleId < 14) {
                        val currentThreshold = levelThresholds[currentLevelIndex]
                        val nextThreshold = levelThresholds[currentLevelIndex + 1]
                        val progress =
                            ((auraPoints - currentThreshold).toFloat() / (nextThreshold - currentThreshold)) * 100
                        rankProgressBar.progress = progress.toInt()
                    } else {
                        // Max level reached
                        rankProgressBar.progress = 100
                    }

                    // Highlight current level in the scroll view
                    highlightCurrentLevel(levelsContainer, currentTitleId)
                }
            }

        btnClosePopup.setOnClickListener {
            popupView.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(300)
                .withEndAction { popupWindow.dismiss() }
                .start()
        }

        // Animation for showing the popup
        popupView.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            popupWindow.showAtLocation(this, Gravity.CENTER, 0, 0)
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun highlightCurrentLevel(levelsContainer: LinearLayout, currentTitleId: Int) {
        // Wait for layout to be ready
        levelsContainer.post {
            for (i in 0 until levelsContainer.childCount) {
                val levelView = levelsContainer.getChildAt(i) as? LinearLayout
                levelView?.let {
                    val isCurrentLevel = (i + 1) == currentTitleId

                    // Update background based on current level
                    it.background = ContextCompat.getDrawable(
                        requireContext(),
                        if (isCurrentLevel) R.drawable.current_level_background else R.drawable.level_item_background
                    )

                    // Make current level more prominent
                    if (isCurrentLevel) {
                        it.scaleX = 1.05f
                        it.scaleY = 1.05f
                        it.elevation = 8f
                    } else {
                        it.scaleX = 1f
                        it.scaleY = 1f
                        it.elevation = 0f
                    }
                }
            }

            // Scroll to current level
            val scrollView = levelsContainer.parent as ScrollView
            val currentLevelView = levelsContainer.getChildAt(currentTitleId - 1)
            scrollView.smoothScrollTo(0, currentLevelView.top - 100) // 100px padding from top
        }
    }

    private fun loadDefaultPosts() {
        posts.clear()
        posts.addAll(getDefaultPosts())
        postsAdapter.notifyDataSetChanged()
    }

    private fun getDefaultPosts(): List<PostItem> {
        return listOf(
            PostItem(
                postId = "ugoiy33o87r",
                profileImage = 3,
                username = "@Kitsch_app",
                isVerified = true,
                postImageData = R.drawable.no_posts.toString(),
                likesCount = 0,
                caption = "add some pics gurl, no ghosts",
                likedBy = emptyList(),
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}