package com.k.kitsch.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.k.kitsch.R
import com.k.kitsch.messages.model.ChatRoom

class UserMessagesAdapter(
    private var userList: List<ChatRoom>,
    private val onItemClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<UserMessagesAdapter.UserMessageViewHolder>() {

    inner class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.userName)
        private val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        private val userAvatar: ImageView = itemView.findViewById(R.id.userAvatar)

        fun bind(user: ChatRoom) {
            userName.text = user.chatName
            lastMessage.text = user.lastMessage
            userAvatar.setImageResource(user.imageChatRoom)
            itemView.setOnClickListener { onItemClick(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserMessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_message_item, parent, false)
        return UserMessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserMessageViewHolder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount(): Int = userList.size

    // Add this function to update the chat rooms list
    fun updateChatRooms(newChatRooms: List<ChatRoom>) {
        userList = newChatRooms
        notifyDataSetChanged()
    }
}