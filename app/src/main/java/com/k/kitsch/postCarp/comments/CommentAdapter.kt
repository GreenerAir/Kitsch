package com.k.kitsch.comments

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.k.kitsch.R
import com.k.kitsch.postCarp.comments.Comment

class CommentAdapter(private val comments: MutableList<Comment> = mutableListOf()) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.commentUsername)
        val commentText: TextView = itemView.findViewById(R.id.commentText)
    }

    // Add single comment (without duplicate check)
    fun addComment(comment: Comment) {
        comments.add(comment)
        notifyItemInserted(comments.size - 1)
    }

    // Add multiple comments
    fun addComments(newComments: List<Comment>) {
        val startPosition = comments.size
        comments.addAll(newComments)
        notifyItemRangeInserted(startPosition, newComments.size)
    }

    // Update all comments
    fun updateComments(newComments: List<Comment>) {
        val oldSize = comments.size
        comments.clear()
        comments.addAll(newComments)

        if (newComments.isEmpty()) {
            notifyItemRangeRemoved(0, oldSize)
        } else if (oldSize == 0) {
            notifyItemRangeInserted(0, newComments.size)
        } else {
            notifyDataSetChanged()
        }
    }

    fun clearComments() {
        comments.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.username.text = "${comment.userId}: "
        holder.commentText.text = comment.text

        // Safe margin adjustment
        (holder.itemView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            val margin = if (position == comments.size - 1) 16 else 8
            params.bottomMargin = margin.dpToPx(holder.itemView.context)
            holder.itemView.layoutParams = params
        }

        holder.itemView.post {
            val params = holder.itemView.layoutParams
            params.height = LinearLayout.LayoutParams.WRAP_CONTENT
            holder.itemView.layoutParams = params
        }

    }

    override fun getItemCount(): Int = comments.size

    // Improved dpToPx using Context
    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    companion object {
        fun setupRecyclerView(recyclerView: RecyclerView, adapter: CommentAdapter) {
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context).apply {
                    stackFromEnd = true // This makes new comments appear at bottom
                }
                setHasFixedSize(false)
                isNestedScrollingEnabled = false
                this.adapter = adapter
            }
        }
    }
}
