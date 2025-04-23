package com.k.kitsch.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.k.kitsch.R

class UserAdapter(
    private var users: List<UserItem>,
    private val userTagClick: (String) -> Unit,
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val profileImages = listOf(
        R.drawable.a1, R.drawable.a2, R.drawable.a3, R.drawable.a4,
        R.drawable.a5, R.drawable.a6, R.drawable.a7, R.drawable.a8,
        R.drawable.a9, R.drawable.a10, R.drawable.a11, R.drawable.a12
    )

    // ViewHolder que representa la vista de cada elemento de la lista de usuarios
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName = itemView.findViewById<TextView>(R.id.userName)
        val userImage = itemView.findViewById<ImageView>(R.id.pfpIcon)
        val userIdTag = itemView.findViewById<TextView>(R.id.userId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.userIdTag.text = "${user.userId}"
        holder.userName.text = "${user.username}"
        holder.userImage.setImageResource(profileImages[user.imageRes])
        holder.itemView.setOnClickListener {
            userTagClick(user.userId)
        }
    }

    // Retornar el número total de usuarios
    override fun getItemCount() = users.size

    // Actualizar la lista de usuarios en el adaptador
    fun updateUsers(newUsers: List<UserItem>) {
        users = newUsers
        notifyDataSetChanged() // Notificar que los datos han cambiado
    }
}
