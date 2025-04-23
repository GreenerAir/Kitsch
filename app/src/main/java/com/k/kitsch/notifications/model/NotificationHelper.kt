import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.k.kitsch.R
import com.k.kitsch.notifications.model.NotificationItem
import com.k.kitsch.notifications.model.NotificationType
import kotlinx.coroutines.tasks.await
import java.util.UUID

object NotificationHelper {

    private val firestore = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()

    // Define your image resources for each notification type
    private val notificationTypeImages = mapOf(
        NotificationType.new_ate to R.drawable.ate_notification,
        NotificationType.comment to R.drawable.comment_notification,
        NotificationType.follow to R.drawable.follow_notification,
        NotificationType.unfollow to R.drawable.unfollow_notification,
        NotificationType.new_post to R.drawable.new_post_notification
    )

    suspend fun createAndSendNotification(
        idItem: String,
        recipientIds: List<String>,
        type: NotificationType,
        profileImage: Int,
        creatorUsername: String,
        notificationText: String,
        isRead: Boolean = false
    ): String? {
        val notificationId = UUID.randomUUID().toString()
        val rightImage = notificationTypeImages[type] ?: R.drawable.default_notification

        val notification = NotificationItem(
            id = notificationId,
            idItem = idItem,
            recipientId = recipientIds,
            type = type,
            profileImage = profileImage,
            creatorUsername = creatorUsername,
            time = System.currentTimeMillis(),
            notificationText = notificationText,
            rightImage = rightImage,
            isRead = isRead
        )

        return try {
            // 1. Save to Firestore
            firestore.collection("Notifications")
                .document(notificationId)
                .set(notification)
                .await()

            // 2. Send push notifications to all recipients
            sendPushNotificationsToRecipients(recipientIds, creatorUsername, notificationText, type)

            notificationId
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error creating notification", e)
            null
        }
    }

    private suspend fun sendPushNotificationsToRecipients(
        recipientIds: List<String>,
        title: String,
        message: String,
        type: NotificationType
    ) {
        // Get all FCM tokens for the recipients
        val tokens = getFcmTokensForUsers(recipientIds)

        if (tokens.isEmpty()) {
            Log.d("NotificationHelper", "No FCM tokens found for recipients")
            return
        }

        // Send to each token
        tokens.forEach { token ->
            sendPushNotification(
                token = token,
                title = title,
                message = message,
                type = type.name
            )
        }
    }

    private suspend fun getFcmTokensForUsers(userIds: List<String>): List<String> {
        return try {
            val tokens = mutableListOf<String>()

            // Batch get user documents
            val userDocs = firestore.collection("Users")
                .whereIn("id", userIds)
                .get()
                .await()

            userDocs.forEach { doc ->
                doc.getString("fcmToken")?.let { token ->
                    if (token.isNotBlank()) {
                        tokens.add(token)
                    }
                }
            }

            tokens
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error getting FCM tokens", e)
            emptyList()
        }
    }

    private fun sendPushNotification(
        token: String,
        title: String,
        message: String,
        type: String
    ) {
        try {
            // Create the notification payload
            val data = mapOf(
                "title" to title,
                "body" to message,
                "type" to type,
                "click_action" to "FLUTTER_NOTIFICATION_CLICK" // For Flutter apps
            )

            // Send the message
            messaging.send(
                RemoteMessage.Builder("$token@fcm.googleapis.com")
                    .setMessageId(UUID.randomUUID().toString())
                    .addData("title", title)
                    .addData("body", message)
                    .addData("type", type)
                    .build()
            )

            Log.d("NotificationHelper", "Push notification sent to token: ${token.take(5)}...")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error sending push notification", e)
        }
    }
}