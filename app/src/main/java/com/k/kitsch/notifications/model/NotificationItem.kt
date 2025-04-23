package com.k.kitsch.notifications.model

import java.util.UUID

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(), // ID of the Notification
    val idItem: String, // ID of the item to go
    val recipientId: List<String> = emptyList(), // ID's of the followers
    val type: NotificationType, // Notification types
    val profileImage: Int, // Profile image
    val creatorUsername: String,  // Username
    val time: Long = System.currentTimeMillis(), // Time
    val notificationText: String, // Notification text
    val rightImage: Int,    // Image for the Notification
    val isRead: Boolean = false // To track read status
)