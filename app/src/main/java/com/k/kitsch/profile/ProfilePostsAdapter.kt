package com.k.kitsch.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import com.k.kitsch.R
import com.k.kitsch.postCarp.PostItem

class ProfilePostsAdapter(
    private val context: Context,
    private val posts: List<PostItem>
) : RecyclerView.Adapter<ProfilePostsAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.postImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        try {
            val bitmap = post.postImageData?.let { base64 ->
                Base64.decode(base64, Base64.DEFAULT).let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }
            }
            holder.postImage.setImageBitmap(
                bitmap ?: ContextCompat.getDrawable(
                    context,
                    R.drawable.no_posts
                )?.toBitmap()
            )
        } catch (e: Exception) {
            holder.postImage.setImageResource(R.drawable.defaultpfpicon)
        }
    }

    override fun getItemCount() = posts.size
}