package com.k.kitsch.messages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.k.kitsch.R
import com.k.kitsch.databinding.FragmentMessagesBinding
import com.k.kitsch.initiation.AuthManager
import com.k.kitsch.messages.chatModel.ChatActivity
import com.k.kitsch.messages.model.ChatRoom

class MessagesFragment : Fragment() {
    private lateinit var binding: FragmentMessagesBinding
    private lateinit var adapter: UserMessagesAdapter
    private val db = FirebaseFirestore.getInstance()
    private var chatRoomsListener: ListenerRegistration? = null
    private val currentUser = AuthManager.currentUser
    private val ChatRooms = mutableListOf<ChatRoom>()


    // Default chat rooms - only shown when there's an error
    private val defaultChatRooms = listOf(
        ChatRoom(
            "1",
            "Kitsch",
            "This is a local Function, check your connection",
            R.drawable.local_fucntion
        ),
    )

    override fun onResume() {
        super.onResume()
        loadChatRooms()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapter with empty list
        adapter = UserMessagesAdapter(emptyList()) { chatRoom ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_USER_ID, chatRoom.id)
                putExtra(ChatActivity.EXTRA_USER_NAME, chatRoom.chatName)
            }
            startActivity(intent)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadChatRooms()
        }

        binding.userMessagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MessagesFragment.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        binding.loadMoreButton.setOnClickListener {
            it.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            loadChatRooms(showAll = true)
        }

        // Show empty state initially
        showEmptyState(true)
        loadChatRooms()
    }

    private fun showLoadingState(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.userMessagesRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        binding.emptyStateView.visibility =
            if (show) View.GONE else binding.emptyStateView.visibility
        binding.loadMoreButton.visibility =
            if (show) View.GONE else binding.loadMoreButton.visibility
        if (!binding.swipeRefreshLayout.isRefreshing) {
            binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
        binding.userMessagesRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        binding.emptyStateView.visibility =
            if (show) View.GONE else binding.emptyStateView.visibility
        binding.loadMoreButton.visibility =
            if (show) View.GONE else binding.loadMoreButton.visibility
    }


    private fun loadChatRooms(showAll: Boolean = false) {

        binding.swipeRefreshLayout.isRefreshing = false


        if (currentUser == null) {
            showErrorState()
            return
        }

        // Clear previous listener to avoid multiple listeners
        chatRoomsListener?.remove()

        // Get current user ID first
        db.collection("Users").document(currentUser.toString()).get()
            .addOnSuccessListener { document ->
                val userId = document.getString("id") ?: run {
                    showErrorState()
                    return@addOnSuccessListener
                }

                // Base query without limit if showing all
                var query = db.collection("ChatRooms")
                    .whereArrayContains("members", userId)

                // Apply limit only if not showing all chats
                if (!showAll) {
                    query = query.limit(20)
                }

                // Show loading state
                showLoadingState(true)

                // Listen for chat rooms
                chatRoomsListener = query.addSnapshotListener { snapshot, error ->
                    showLoadingState(false)

                    if (error != null) {
                        showErrorState()
                        return@addSnapshotListener
                    }

                    snapshot?.let { querySnapshot ->

                        ChatRooms.clear()

                        if (querySnapshot.isEmpty) {
                            showEmptyState(true)
                            return@let
                        }

                        querySnapshot.documents.forEach { document ->
                            ChatRooms.add(
                                ChatRoom(
                                    id = document.getString("id") ?: "",
                                    chatName = document.getString("chatName") ?: "Unknown",
                                    lastMessage = document.getString("lastMessage") ?: "",
                                    imageChatRoom = document.getLong("imageChatRoom")?.toInt()
                                        ?: R.drawable.chat_room_deafult
                                )
                            )
                        }

                        // Single update to adapter after all documents processed
                        adapter.updateChatRooms(ChatRooms)
                        showEmptyState(false)

                        // Show "Load More" button if we're paginating and there might be more chats
                        binding.loadMoreButton.visibility =
                            if (!showAll && querySnapshot.size() >= 20) View.VISIBLE else View.GONE
                    }
                }
            }
            .addOnFailureListener {
                showLoadingState(false)
                showErrorState()
            }
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            // Show empty state (image or message)
            binding.emptyStateView.visibility = View.VISIBLE
            binding.userMessagesRecyclerView.visibility = View.GONE
        } else {
            // Hide empty state
            binding.emptyStateView.visibility = View.GONE
            binding.userMessagesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showErrorState() {
        adapter.updateChatRooms(defaultChatRooms)
        binding.emptyStateView.visibility = View.GONE
        binding.userMessagesRecyclerView.visibility = View.VISIBLE
        Snackbar.make(binding.root, "Error loading chats", Snackbar.LENGTH_LONG)
            .setAction("Retry") { loadChatRooms() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatRoomsListener?.remove()
    }
}