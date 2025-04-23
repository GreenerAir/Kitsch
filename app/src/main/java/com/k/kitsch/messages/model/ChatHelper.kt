package com.k.kitsch.messages.model

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.k.kitsch.R
import com.k.kitsch.messages.chatModel.Message
import kotlinx.coroutines.tasks.await
import java.util.Random
import java.util.UUID

object ChatHelper {

    private val db = FirebaseFirestore.getInstance()
    private val random = Random()

    // Default chat images
    private val defaultChatImages = listOf(
        R.drawable.chat_room_pic1,
        R.drawable.chat_room_pic2,
        R.drawable.chat_room_pic3,
        R.drawable.chat_room_pic4,
        R.drawable.chat_room_pic5,
    )

    suspend fun createChatRoom(
        userIds: List<String>,
        chatName: String
    ): String? {
        return try {
            val chatId = UUID.randomUUID().toString()
            val randomImage = defaultChatImages[random.nextInt(defaultChatImages.size)]

            val chatRoom = mapOf(
                "id" to chatId,
                "chatName" to chatName,
                "lastMessage" to "Chat created",
                "imageChatRoom" to randomImage,
                "members" to userIds,
                "lastMessageTime" to FieldValue.serverTimestamp()
            )

            // Create the chat room document only
            db.collection("ChatRooms")
                .document(chatId)
                .set(chatRoom)
                .await()

            chatId
        } catch (e: Exception) {
            Log.e("ChatHelper", "Error creating chat room", e)
            null
        }
    }

    fun listenForMessages(
        chatId: String,
        onMessagesReceived: (List<Message>?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("ChatRooms")
            .document(chatId)
            .collection("Messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    onError(it)
                    onMessagesReceived(null)
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        onMessagesReceived(null)
                    } else {
                        val messages = querySnapshot.documents.mapNotNull { doc ->
                            try {
                                Message(
                                    id = doc.getString("id") ?: "",
                                    text = doc.getString("text"),
                                    audioPath = doc.getString("audioPath"),
                                    timestamp = doc.getLong("timestamp") ?: 0L,
                                    senderId = doc.getString("senderId") ?: "",
                                    receiverId = doc.getString("receiverId") ?: "",
                                    isPlaying = false
                                )
                            } catch (e: Exception) {
                                Log.e("ChatHelper", "Error parsing message", e)
                                null
                            }
                        }
                        onMessagesReceived(if (messages.isEmpty()) null else messages)
                    }
                }
            }
    }

    suspend fun getChatRoomsForUser(userId: String): List<ChatRoom>? {
        return try {
            val snapshot = db.collection("ChatRooms")
                .whereArrayContains("members", userId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .get()
                .await()

            if (snapshot.isEmpty) {
                null
            } else {
                snapshot.documents.mapNotNull { doc ->
                    try {
                        ChatRoom(
                            id = doc.getString("id") ?: "",
                            chatName = doc.getString("chatName") ?: "Unnamed Chat",
                            lastMessage = doc.getString("lastMessage") ?: "",
                            imageChatRoom = doc.getLong("imageChatRoom")?.toInt()
                                ?: R.drawable.chat_room_deafult
                        )
                    } catch (e: Exception) {
                        Log.e("ChatHelper", "Error parsing chat room", e)
                        null
                    }
                }.takeIf { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            Log.e("ChatHelper", "Error getting chat rooms", e)
            null
        }
    }

    fun cleanupListener(registration: ListenerRegistration?) {
        registration?.remove()
    }
}