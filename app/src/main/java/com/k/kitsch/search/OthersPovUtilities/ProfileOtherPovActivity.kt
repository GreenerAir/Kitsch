package com.k.kitsch.search.OthersPovUtilities

import NotificationHelper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.k.kitsch.R
import com.k.kitsch.databinding.ActivityProfileOtherPovBinding
import com.k.kitsch.initiation.AuthManager
import com.k.kitsch.messages.chatModel.ChatActivity
import com.k.kitsch.messages.model.ChatHelper
import com.k.kitsch.notifications.model.NotificationType
import com.k.kitsch.postCarp.PostItem
import com.k.kitsch.profile.ProfilePostsAdapter
import com.k.kitsch.wardrobeotherpov.WardrobeOtherPovActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileOtherPovActivity : AppCompatActivity() {

    private var _binding: ActivityProfileOtherPovBinding? = null
    private val binding get() = _binding!!
    private val posts = mutableListOf<PostItem>()
    private lateinit var postsAdapter: ProfilePostsAdapter
    private lateinit var usernameIdView: String
    val currentUser = AuthManager.currentUser
    private val db = FirebaseFirestore.getInstance()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityProfileOtherPovBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usernameIdView = intent.getStringExtra("UsernameView") ?: ""
        if (usernameIdView.isEmpty()) {
            finish() // Close activity if no username provided
            return
        }

        loadUserData()
        setupButtons()
        setupPostsGrid()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun setupButtons() {
        updateFollowButtonInitialState()

        binding.followButton.setOnClickListener {
            db.collection("Users").whereEqualTo("id", usernameIdView).get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        Log.e("ProfileOtherPov", "Target user not found")
                        return@addOnSuccessListener
                    }

                    val targetUserDoc = querySnapshot.documents[0]
                    val targetUserEmail = targetUserDoc.id

                    db.collection("Users").document(currentUser.toString()).get()
                        .addOnSuccessListener { currentUserDoc ->
                            val following =
                                currentUserDoc.get("following") as? List<String> ?: emptyList()
                            val userId = currentUserDoc.getString("id")
                            val isFollowing = following.any { it == usernameIdView }

                            val batch = db.batch()
                            val currentUserRef =
                                db.collection("Users").document(currentUser.toString())
                            val targetUserRef = db.collection("Users").document(targetUserEmail)

                            if (isFollowing) {
                                // Unfollow logic
                                batch.update(
                                    currentUserRef,
                                    "following",
                                    FieldValue.arrayRemove(usernameIdView)
                                )
                                batch.update(
                                    currentUserRef,
                                    "followingCounter",
                                    FieldValue.increment(-1)
                                )
                                batch.update(
                                    targetUserRef,
                                    "followers",
                                    FieldValue.arrayRemove(userId)
                                )
                                batch.update(
                                    targetUserRef,
                                    "followersCounter",
                                    FieldValue.increment(-1)
                                )

                                // Send unfollow notification
                                sendFollowNotification(usernameIdView, false)
                            } else {
                                // Follow logic
                                batch.update(
                                    currentUserRef,
                                    "following",
                                    FieldValue.arrayUnion(usernameIdView)
                                )
                                batch.update(
                                    currentUserRef,
                                    "followingCounter",
                                    FieldValue.increment(1)
                                )
                                batch.update(
                                    targetUserRef,
                                    "followers",
                                    FieldValue.arrayUnion(userId)
                                )
                                batch.update(
                                    targetUserRef,
                                    "followersCounter",
                                    FieldValue.increment(1)
                                )

                                // Send follow notification
                                sendFollowNotification(usernameIdView, true)
                            }

                            batch.commit()
                                .addOnSuccessListener {
                                    updateFollowButtonImage(!isFollowing)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ProfileOtherPov", "Error updating follow status", e)
                                }
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileOtherPov", "Error getting target user", e)
                }
        }
        binding.messageButton.setOnClickListener {
            // Check if chat already exists between these users
            checkExistingChat { existingChatId ->
                if (existingChatId != null) {
                    // Open existing chat
                    openChatActivity(existingChatId)
                } else {
                    // Check if both users follow each other
                    checkMutualFollow { isMutualFollow ->
                        if (isMutualFollow) {
                            // Create new chat automatically
                            createNewChat()
                        } else {
                            // Show message that both need to follow each other
                            Toast.makeText(
                                this@ProfileOtherPovActivity,
                                "You both need to follow each other to start chatting",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
        binding.wardrobeButton.setOnClickListener {
            val intent = Intent(this, WardrobeOtherPovActivity::class.java)
            startActivity(intent)
        }
        binding.backToSearchBtn.setOnClickListener {
            finish()
        }
    }

    private fun sendFollowNotification(targetUserId: String, isFollowing: Boolean) {

        // Get current user's username for the notification
        db.collection("Users").document(currentUser.toString()).get()
            .addOnSuccessListener { currentUserDoc ->
                val currentUsername = currentUserDoc.getString("id") ?: ""
                val currentProfileIndex = (currentUserDoc.getLong("pfpIconId") ?: 0L).toInt()

                // Get target user's document to send notification
                db.collection("Users").whereEqualTo("id", targetUserId).get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot.isEmpty) return@addOnSuccessListener

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val notificationType =
                                    if (isFollowing) NotificationType.follow else NotificationType.unfollow
                                val notificationText = if (isFollowing)
                                    "$currentUsername started following you"
                                else
                                    "$currentUsername unfollowed you" // Fixed typo in "unfollowed"

                                NotificationHelper.createAndSendNotification(
                                    idItem = System.currentTimeMillis().toString(),
                                    recipientIds = listOf(usernameIdView),
                                    type = notificationType,
                                    profileImage = currentProfileIndex,
                                    creatorUsername = currentUsername,
                                    notificationText = notificationText
                                )
                            } catch (e: Exception) {
                                Log.e("ProfileOtherPov", "Error sending notification", e)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileOtherPov", "Error getting target user for notification", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileOtherPov", "Error getting current user data for notification", e)
            }
    }

    private fun updateFollowButtonInitialState() {
        val currentUserEmail = currentUser.toString()

        db.collection("Users").document(currentUserEmail).get()
            .addOnSuccessListener { document ->
                val following = document.get("following") as? List<String> ?: emptyList()
                val isFollowing = following.contains(usernameIdView)
                updateFollowButtonImage(isFollowing)
            }
            .addOnFailureListener { e ->
                Log.e("ProfileOtherPov", "Error checking follow status", e)
            }
    }

    private fun updateFollowButtonImage(isFollowing: Boolean) {
        binding.followButton.setImageResource(
            if (isFollowing) R.drawable.unfollow_button else R.drawable.follow_button
        )
    }

    private fun auraCalculator(auras: Int?): Int? {
        return auras?.let {
            when {
                it == 0 -> 1
                it < 1_000 -> 2
                it < 2_000 -> 3
                it < 4_000 -> 4
                it < 8_000 -> 5
                it < 16_000 -> 6
                it < 24_000 -> 7
                it < 30_000 -> 8
                it < 40_000 -> 9
                it < 50_000 -> 10
                it < 60_000 -> 11
                it < 75_000 -> 12
                it < 95_000 -> 13
                it < 101_000 -> 14
                else -> null
            }
        }
    }

    private fun loadUserData() {
        FirebaseFirestore.getInstance()
            .collection("Users")
            .whereEqualTo("id", usernameIdView)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]

                    val followersCount = document.getLong("followersCounter") ?: 0L
                    val followingCount = document.getLong("followingCounter") ?: 0L

                    usernameIdView = document.getString("id") ?: ""
                    binding.userName.text = document.getString("username") ?: ""
                    binding.totalFollowers.text = followersCount.toString()
                    binding.totalFollowing.text = followingCount.toString()
                    binding.displayName.text = usernameIdView

                    val auraPoints = document.getLong("auraPoints")?.toInt()
                    val auraTitleId = auraCalculator(auraPoints)

                    binding.totalPosts.text = document.getLong("postCounter")?.toString() ?: "0"

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

                    loadUserPosts()
                } else {
                    loadDefaultPosts()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileOtherPov", "Error loading user data", e)
                loadDefaultPosts()
            }
    }

    private fun setupPostsGrid() {
        postsAdapter = ProfilePostsAdapter(this, posts)
        binding.postsGrid.adapter = postsAdapter
    }

    private fun loadUserPosts() {
        usernameIdView.let { userId ->
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
                    Log.e("ProfileOtherPov", "Error loading posts", e)
                    loadDefaultPosts()
                }
        }
    }

    private fun processFirestoreDocuments(documents: QuerySnapshot) {
        try {
            posts.clear()
            documents.forEach { document ->
                val profileImageIndex = ((document.getLong("profileImageId") ?: 1L).toInt() - 1)
                    .coerceIn(0, 11)

                posts.add(
                    PostItem(
                        postId = document.id,
                        profileImage = profileImageIndex,
                        username = document.getString("username") ?: "anonymous",
                        isVerified = document.getBoolean("isVerified") ?: false,
                        postImageData = document.getString("postImageData"),
                        likesCount = document.getLong("likesCounter")?.toInt() ?: 0,
                        caption = document.getString("caption") ?: "",
                        likedBy = document.get("likedBy") as? List<String> ?: emptyList()
                    )
                )
            }
            postsAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("ProfileOtherPov", "Error processing documents", e)
            loadDefaultPosts()
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
                postId = "",
                profileImage = 0,
                username = "@Kitsch_app",
                isVerified = true,
                postImageData = R.drawable.no_otherspovs.toString(),
                likesCount = 0,
                caption = "no spice, no flavour!",
                likedBy = emptyList(),
            )
        )
    }

    private fun checkExistingChat(callback: (String?) -> Unit) {

        db.collection("Users").document(currentUser.toString()).get()
            .addOnSuccessListener { currentUserDoc ->
                val currentUserId = currentUserDoc.getString("id") ?: ""

                // Query chat rooms where both users are members
                db.collection("ChatRooms")
                    .whereArrayContains("members", currentUserId)
                    .whereArrayContains("members", usernameIdView)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        callback(querySnapshot.documents.firstOrNull()?.id)
                    }
                    .addOnFailureListener {
                        callback(null)
                    }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    private fun checkMutualFollow(callback: (Boolean) -> Unit) {
        val currentUserEmail = currentUser.toString()

        db.collection("Users").document(currentUserEmail).get()
            .addOnSuccessListener { currentUserDoc ->
                val currentUserFollowing =
                    currentUserDoc.get("following") as? List<String> ?: emptyList()
                val currentUserId = currentUserDoc.getString("id") ?: ""

                db.collection("Users").whereEqualTo("id", usernameIdView).get()
                    .addOnSuccessListener { targetUserSnapshot ->
                        if (targetUserSnapshot.isEmpty) {
                            callback(false)
                            return@addOnSuccessListener
                        }

                        val targetUserDoc = targetUserSnapshot.documents[0]
                        val targetUserFollowing =
                            targetUserDoc.get("following") as? List<String> ?: emptyList()

                        // Check if both follow each other
                        val isMutualFollow = currentUserFollowing.contains(usernameIdView) &&
                                targetUserFollowing.contains(currentUserId)
                        callback(isMutualFollow)
                    }
                    .addOnFailureListener {
                        callback(false)
                    }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun createNewChat() {

        db.collection("Users").document(currentUser.toString()).get()
            .addOnSuccessListener { currentUserDoc ->
                val currentUserId = currentUserDoc.getString("id") ?: run {
                    Toast.makeText(this, "Failed to get current user ID", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val currentUsername = currentUserDoc.getString("username") ?: "User"

                db.collection("Users").whereEqualTo("id", usernameIdView).get()
                    .addOnSuccessListener { targetUserSnapshot ->
                        if (targetUserSnapshot.isEmpty) {
                            Toast.makeText(this, "Target user not found", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val targetUserDoc = targetUserSnapshot.documents[0]
                        val targetUsername = targetUserDoc.getString("username") ?: "User"
                        val targetUserId = targetUserDoc.getString("id") ?: run {
                            Toast.makeText(this, "Failed to get target user ID", Toast.LENGTH_SHORT)
                                .show()
                            return@addOnSuccessListener
                        }

                        // Create chat with both users
                        val chatName = "$currentUsername and $targetUsername"
                        val members = listOf(
                            currentUserId,
                            targetUserId
                        ) // Use targetUserId instead of usernameIdView

                        CoroutineScope(Dispatchers.IO).launch {
                            try {

                                val chatId = ChatHelper.createChatRoom(
                                    userIds = members,
                                    chatName = chatName,
                                )

                                withContext(Dispatchers.Main) {
                                    if (chatId != null) {
                                        Log.d("ChatCreation", "Successfully created chat: $chatId")
                                        openChatActivity(chatId)
                                    } else {
                                        Log.e("ChatCreation", "Chat creation returned null")
                                        Toast.makeText(
                                            this@ProfileOtherPovActivity,
                                            "Failed to create chat (null returned)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ChatCreation", "Error creating chat", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@ProfileOtherPovActivity,
                                        "Error: ${e.localizedMessage}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatCreation", "Error getting target user", e)
                        Toast.makeText(
                            this@ProfileOtherPovActivity,
                            "Failed to get target user: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ChatCreation", "Error getting current user", e)
                Toast.makeText(
                    this@ProfileOtherPovActivity,
                    "Failed to get current user: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun openChatActivity(chatId: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("otherUserId", usernameIdView)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}