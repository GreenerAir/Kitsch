package com.k.kitsch.notifications

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.k.kitsch.R
import com.k.kitsch.initiation.AuthManager
import com.k.kitsch.mainFragments.HomeFragment
import com.k.kitsch.notifications.model.NotificationItem
import com.k.kitsch.notifications.model.NotificationType
import com.k.kitsch.notifications.model.NotificationsAdapter
import com.k.kitsch.search.OthersPovUtilities.ProfileOtherPovActivity
import java.util.UUID

class NotificationsFragment : Fragment() {

    private lateinit var notificationsAdapter: NotificationsAdapter
    private val notifications = mutableListOf<NotificationItem>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = AuthManager.currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("NotificationsFragment", "Creating view")
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        setupRecyclerView(view)
        loadUserNotifications()

        return view
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rviewNotifs)
        if (recyclerView == null) {
            Log.e("NotificationsFragment", "RecyclerView with ID rviewNotifs not found")
        } else {
            Log.d("NotificationsFragment", "RecyclerView found, setting up")

            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            notificationsAdapter = NotificationsAdapter(notifications) { id, type ->
                handleNotificationClick(id, type)
            }
            recyclerView.adapter = notificationsAdapter
        }
    }

    private fun loadUserNotifications() {
        val currentUserEmail = currentUser.toString()

        db.collection("Users")
            .document(currentUserEmail)
            .get()
            .addOnSuccessListener { userDocument ->
                val currentUserId = userDocument.getString("id") ?: ""

                // Now load notifications where current user is in recipientId array
                loadFollowingNotifications(currentUserId)
            }
            .addOnFailureListener { e ->
                Log.e("Notifications", "Error fetching user ID", e)
                loadDefaultNotifications()
            }
    }

    private fun loadFollowingNotifications(recipientId: String) {
        db.collection("Notifications")
            .whereArrayContains(
                "recipientId",
                recipientId
            ) // Only notifications from followed users
            .orderBy("time")
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    Log.e("NotificationsFragment", "Listen failed", it)
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val newNotifications = mutableListOf<NotificationItem>()
                    for (document in querySnapshot.documents) {
                        try {
                            newNotifications.add(
                                NotificationItem(
                                    id = document.id,
                                    idItem = document.getString("idItem") ?: "",
                                    recipientId = document.get("recipientId") as? List<String>
                                        ?: emptyList(),
                                    type = when (document.getString("type")) {
                                        "follow" -> NotificationType.follow
                                        "unfollow" -> NotificationType.unfollow
                                        "new_post" -> NotificationType.new_post
                                        "comment" -> NotificationType.comment
                                        "new_ate" -> NotificationType.new_ate
                                        else -> NotificationType.new_ate // default
                                    },
                                    profileImage = (document.getLong("profileImage")
                                        ?: 1L).toInt() + 1,
                                    creatorUsername = document.getString("creatorUsername")
                                        ?: "anonymous",
                                    time = document.getLong("time") ?: System.currentTimeMillis(),
                                    notificationText = document.getString("notificationText") ?: "",
                                    rightImage = (document.getLong("rightImage") ?: 1L).toInt() + 1,
                                    isRead = document.getBoolean("isRead") ?: false
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("NotificationsFragment", "Error parsing notification document", e)
                        }
                    }
                    updateNotificationsList(newNotifications)
                }
            }
    }

    private fun updateNotificationsList(newNotifications: List<NotificationItem>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notificationsAdapter.notifyDataSetChanged()
    }

    private fun loadDefaultNotifications() {
        notifications.addAll(
            listOf(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    idItem = "3852bba7-5cbe-4efd-83bd-0a45409b96ed",
                    recipientId = emptyList(),
                    type = NotificationType.new_ate,
                    profileImage = 7,
                    creatorUsername = "kitschuser",
                    time = 8738923,
                    notificationText = "has liked your post",
                    rightImage = 1,
                    isRead = false
                ),
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    idItem = "3852bba7-5cbe-4efd-83bd-0a45409b96ed",
                    recipientId = emptyList(),
                    type = NotificationType.comment,
                    profileImage = 2,
                    creatorUsername = "kitschuser",
                    time = System.currentTimeMillis(),
                    notificationText = "has commented on your post",
                    rightImage = 1,
                    isRead = false
                )
            )
        )
        notificationsAdapter.notifyDataSetChanged()
    }

    private fun handleNotificationClick(id: String, type: NotificationType) {
        when (type) {
            NotificationType.comment, NotificationType.new_post, NotificationType.new_ate -> {
                // Navigate to post detail for post-related notifications
                val fragment = HomeFragment().apply {
                    arguments = Bundle().apply {
                        putString("postId", id)
                    }
                }
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }

            NotificationType.follow, NotificationType.unfollow -> {
                val intent = Intent(requireContext(), ProfileOtherPovActivity::class.java).apply {
                    putExtra("UsernameView", id)
                }
                startActivity(intent)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = NotificationsFragment()
    }
}