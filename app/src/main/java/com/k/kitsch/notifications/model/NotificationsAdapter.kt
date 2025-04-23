package com.k.kitsch.notifications.model

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.k.kitsch.R
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationsAdapter(
    private val notifications: List<NotificationItem>,
    private val onItemClick: (String, NotificationType) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    // Predefined profile images
    private val profileImages = listOf(
        R.drawable.a1, R.drawable.a2, R.drawable.a3, R.drawable.a4,
        R.drawable.a5, R.drawable.a6, R.drawable.a7, R.drawable.a8,
        R.drawable.a9, R.drawable.a10, R.drawable.a11, R.drawable.a12
    )

    // Mapping of notification types to their corresponding icons
    private val notificationTypeIcons = mapOf(
        NotificationType.new_ate to R.drawable.ate_notification,
        NotificationType.comment to R.drawable.comment_notification,
        NotificationType.follow to R.drawable.follow_notification,
        NotificationType.unfollow to R.drawable.unfollow_notification,
        NotificationType.new_post to R.drawable.new_post_notification
    )

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profileImage)
        val username: TextView = itemView.findViewById(R.id.usernameNotif)
        val time: TextView = itemView.findViewById(R.id.time)
        val notificationText: TextView = itemView.findViewById(R.id.notificationText)
        val rightImage: ImageView = itemView.findViewById(R.id.rightImage)
        val badge: ConstraintLayout = itemView.findViewById(R.id.badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_notification_item, parent, false)
        return NotificationViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        // Set profile image safely
        val profileImageIndex = (notification.profileImage ?: 1) - 1 // Convert to 0-based
        val safeProfileIndex = profileImageIndex.coerceIn(0, profileImages.lastIndex)
        holder.profileImage.setImageResource(profileImages[safeProfileIndex])

        // Set right image based on notification type
        val rightImageRes =
            notificationTypeIcons[notification.type] ?: R.drawable.default_notification
        holder.rightImage.setImageResource(rightImageRes)

        // Set text content
        holder.username.text = notification.creatorUsername
        holder.notificationText.text = notification.notificationText

        // Set formatted time
        holder.time.text = formatNotificationTime(notification.time)

        // Set badge color
        val badgeColor = when (notification.type) {
            NotificationType.follow -> R.color.notification_follow
            NotificationType.unfollow -> R.color.notification_unfollow
            NotificationType.new_post -> R.color.notification_new_post
            NotificationType.comment -> R.color.notification_comment
            NotificationType.new_ate -> R.color.notification_like
        }
        holder.badge.setBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, badgeColor)
        )

        holder.itemView.setOnClickListener {
            onItemClick(notification.idItem, notification.type)
        }
    }

    private fun formatNotificationTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val elapsedMillis = now - timestamp

        return when {
            elapsedMillis < 60_000 -> "Just now"
            elapsedMillis < 3_600_000 -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)
                "$minutes m ago"  // Removed plural check
            }

            elapsedMillis < 86_400_000 -> {
                val hours = TimeUnit.MILLISECONDS.toHours(elapsedMillis)
                "$hours h ago"    // Removed plural check
            }

            elapsedMillis < 604_800_000 -> {
                val days = TimeUnit.MILLISECONDS.toDays(elapsedMillis)
                "$days d ago"     // Removed plural check
            }

            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }

    override fun getItemCount(): Int = notifications.size
}