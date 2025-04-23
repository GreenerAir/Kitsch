package com.k.kitsch.mainFragments

import NotificationHelper
import PostsAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.k.kitsch.R
import com.k.kitsch.databinding.FragmentHomeBinding
import com.k.kitsch.initiation.AuthManager
import com.k.kitsch.notifications.model.NotificationType
import com.k.kitsch.postCarp.PostItem
import com.k.kitsch.postCarp.comments.Comment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class HomeFragment : Fragment() {

    private lateinit var postsAdapter: PostsAdapter
    private val posts = mutableListOf<PostItem>()
    private val db = FirebaseFirestore.getInstance()
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    val currentUser = AuthManager.currentUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeBasicAdapter()

        // Then load data and update the adapter
        loadUserDataAndSetupAdapter()
        loadFollowingPosts()
    }

    override fun onResume() {
        super.onResume()
        loadFollowingPosts()
    }

    private fun initializeBasicAdapter() { // For not Collapse
        postsAdapter = PostsAdapter(
            mutableListOf(),
            currentUsername = "loading",
            onCommentAdded = { position, _, comment ->
                if (position in posts.indices) {
                    addComment(position, comment)
                }
            },
            onShowMoreComments = { position ->
                if (position in posts.indices) {
                    posts[position] = posts[position].copy(showAllComments = true)
                    postsAdapter.notifyItemChanged(position)
                }
            },
            onLikeClicked = { position ->
                if (position in posts.indices) {
                    toggleLike(position)
                }
            }
        )

        binding.rViewPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postsAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadUserDataAndSetupAdapter() {
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(currentUser.toString())
            .get()
            .addOnSuccessListener { document ->
                val currentUsername = document.getString("id") ?: "anonymous"

                // Update the adapter with the correct username
                postsAdapter = PostsAdapter(
                    posts,
                    currentUsername = currentUsername,
                    onCommentAdded = { position, _, comment ->
                        addComment(position, comment)
                    },
                    onShowMoreComments = { position ->
                        posts[position] = posts[position].copy(showAllComments = true)
                        postsAdapter.notifyItemChanged(position)
                    },
                    onLikeClicked = { position ->
                        toggleLike(position)
                    }
                )
                binding.rViewPosts.adapter = postsAdapter
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error fetching user data", e)
                Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addComment(position: Int, commentText: String) {
        if (position !in posts.indices) return
        if (commentText.isBlank()) return

        val post = posts[position]

        // Get user data first before creating the comment
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(currentUser.toString())
            .get()
            .addOnSuccessListener { document ->
                val username = document.getString("id") ?: "anonymous"

                val comment = Comment(
                    id = UUID.randomUUID().toString(),
                    userId = username,
                    text = commentText,
                    timestamp = System.currentTimeMillis()
                )

                // Add to Firestore first
                db.collection("Posts").document(post.postId)
                    .collection("Comments").document(comment.id)
                    .set(comment)
                    .addOnSuccessListener {
                        hideKeyboard()
                    }
                    .addOnFailureListener { e ->
                        Log.e("HomeFragment", "Error adding comment", e)
                        Toast.makeText(context, "Failed to post comment", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error fetching user data", e)
                Toast.makeText(context, "Failed to verify user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadFollowingPosts() {
        showLoading(true)
        db.collection("Users")
            .document(currentUser.toString())
            .get()
            .addOnSuccessListener { userDocument ->
                val following = userDocument.get("following") as? List<String> ?: emptyList()
                val currentUserId = userDocument.get("id")

                // Include the current user's own posts
                val usersToShowPosts = following + currentUserId

                db.collection("Posts")
                    .whereIn("username", usersToShowPosts)
                    .addSnapshotListener { snapshot, error ->
                        showLoading(false)
                        error?.let {
                            Log.e("HomeFragment", "Listen failed", it)
                            loadDefaultPosts()
                            return@addSnapshotListener
                        }

                        snapshot?.let { querySnapshot ->
                            val newPosts = mutableListOf<PostItem>()
                            for (document in querySnapshot.documents) {
                                try {
                                    newPosts.add(
                                        PostItem(
                                            postId = document.id,
                                            profileImage = (document.getLong("profileImage")
                                                ?: 1L).toInt() + 1,
                                            username = document.getString("username")
                                                ?: "anonymous",
                                            isVerified = document.getBoolean("isVerified") == true,
                                            postImageData = document.getString("postImageData"),
                                            likesCount = document.getLong("likesCount")?.toInt()
                                                ?: 0,
                                            caption = document.getString("caption") ?: "",
                                            likedBy = document.get("likedBy") as? List<String>
                                                ?: emptyList()
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e("HomeFragment", "Error parsing document", e)
                                }
                            }
                            updatePostsList(newPosts)
                        }
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e("HomeFragment", "Error fetching following list", e)
                Toast.makeText(context, "Failed to load following list", Toast.LENGTH_SHORT).show()
                loadDefaultPosts()
            }
    }


    private fun updatePostsList(newPosts: List<PostItem>) {
        posts.clear()
        posts.addAll(newPosts)
        postsAdapter.notifyDataSetChanged()
        loadCommentsForPosts()
    }

    private fun loadCommentsForPosts() {
        posts.forEachIndexed { index, post ->
            db.collection("Posts").document(post.postId)
                .collection("Comments")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    snapshot?.let { querySnapshot ->
                        val newComments = querySnapshot.toObjects(Comment::class.java)
                        posts[index] = post.copy(comments = newComments)
                        postsAdapter.notifyItemChanged(index)
                    }
                }
        }
    }

    private fun showLoading(show: Boolean) {
        if (!isAdded) return
        try {
            binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
            binding.rViewPosts.visibility = if (show) View.GONE else View.VISIBLE
        } catch (e: IllegalStateException) {
            Log.e("HomeFragment", "Fragment not attached", e)
        }
    }

    private fun loadDefaultPosts() {
        posts.addAll(
            listOf(
                PostItem(
                    postId = "default1",
                    profileImage = 1,
                    username = "kitschuser",
                    isVerified = true,
                    postImageData = R.drawable.jennierubyjane2.toString(),
                    likesCount = 1200,
                    caption = "This is a cool post!",
                    likedBy = emptyList(),
                ),
                PostItem(
                    postId = "default2",
                    profileImage = 2,
                    username = "anotheruser",
                    isVerified = false,
                    postImageData = R.drawable.post.toString(),
                    likesCount = 50,
                    caption = "Another great post!",
                    likedBy = emptyList(),
                )
            )
        )
        postsAdapter.notifyDataSetChanged()
    }

    private fun hideKeyboard() {
        ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun toggleLike(position: Int) {
        val post = posts[position]

        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(currentUser.toString())
            .get()
            .addOnSuccessListener { document ->
                val currentUserId = document.getString("id") ?: "anonymous"
                val currentUsername = document.getString("username") ?: "anonymous"
                val currentProfileIndex = (document.getLong("pfpIconId") ?: 1L).toInt()

                updateLikeState(position, post, currentUserId, currentUsername, currentProfileIndex)
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error fetching user data", e)
                Toast.makeText(context, "Failed to verify user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLikeState(
        position: Int,
        post: PostItem,
        currentUserId: String,
        currentUsername: String,
        currentProfileIndex: Int
    ) {
        val isCurrentlyLiked = post.likedBy.contains(currentUserId)

        val newLikedBy = if (isCurrentlyLiked) {
            post.likedBy - currentUserId
        } else {
            post.likedBy + currentUserId
        }

        val newLikesCount = if (isCurrentlyLiked) {
            post.likesCount - 1
        } else {
            post.likesCount + 1
        }

        // Update local state
        posts[position] = post.copy(
            likedBy = newLikedBy,
            likesCount = newLikesCount
        )
        postsAdapter.notifyItemChanged(position)

        db.collection("Posts").document(post.postId).update(
            mapOf(
                "likedBy" to newLikedBy,
                "likesCount" to newLikesCount
            )
        ).addOnSuccessListener {
            // Only send notification when liking (not unliking)
            if (!isCurrentlyLiked) {
                // Get post owner's data to send notification
                db.collection("Users").whereEqualTo("id", post.username).get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    NotificationHelper.createAndSendNotification(
                                        idItem = post.postId,
                                        recipientIds = listOf("", post.username),
                                        type = NotificationType.new_ate,
                                        profileImage = currentProfileIndex,
                                        creatorUsername = currentUsername,
                                        notificationText = "$currentUsername liked your post"
                                    )
                                } catch (e: Exception) {
                                    Log.e("HomeFragment", "Failed to send like notification", e)
                                }
                            }
                        }
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("HomeFragment", "Error updating like", e)
            // Rollback if failed
            posts[position] = post
            postsAdapter.notifyItemChanged(position)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}