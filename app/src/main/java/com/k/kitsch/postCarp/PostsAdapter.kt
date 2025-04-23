import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.k.kitsch.R
import com.k.kitsch.comments.CommentAdapter
import com.k.kitsch.postCarp.PostItem
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

class PostsAdapter(
    val posts: MutableList<PostItem>,
    var currentUsername: String,
    private val onCommentAdded: (Int, String, String) -> Unit,
    private val onShowMoreComments: (Int) -> Unit,
    private val onLikeClicked: (Int) -> Unit,
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private val profileImages = listOf(
        R.drawable.a1, R.drawable.a2, R.drawable.a3, R.drawable.a4,
        R.drawable.a5, R.drawable.a6, R.drawable.a7, R.drawable.a8,
        R.drawable.a9, R.drawable.a10, R.drawable.a11, R.drawable.a12
    )

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pfpIcon: CircleImageView = itemView.findViewById(R.id.pfpIcon)
        val verifiedBadge: ImageView = itemView.findViewById(R.id.verified_badge)
        val username: TextView = itemView.findViewById(R.id.username)
        val usernameOnCaption: TextView = itemView.findViewById(R.id.usernameOnCaption)
        val postImage: ImageView = itemView.findViewById(R.id.post_image)
        val likeButton: ImageButton = itemView.findViewById(R.id.like_button)
        val likesCount: TextView = itemView.findViewById(R.id.likesCount)
        val caption: TextView = itemView.findViewById(R.id.captionTextView)
        val commentInput: EditText = itemView.findViewById(R.id.commentInput)
        val postCommentButton: ImageButton = itemView.findViewById(R.id.postCommentButton)
        val commentsRecyclerView: RecyclerView = itemView.findViewById(R.id.commentsRecyclerView)
        private val showMoreCommentsBtn: Button = itemView.findViewById(R.id.showMoreBtn)
        private val MAX_VISIBLE_COMMENTS = 3

        private val commentAdapter = CommentAdapter()

        init {
            commentsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context).apply {
                    stackFromEnd = true
                }
                adapter = commentAdapter
                setHasFixedSize(true)
                isNestedScrollingEnabled = false
            }
        }

        fun updateLikeButton(post: PostItem, currentUserId: String) {
            val isLiked = post.likedBy.contains(currentUserId)
            likeButton.setImageResource(
                if (isLiked) R.drawable.likebutton_active else R.drawable.likebutton
            )
            likesCount.text = formatLikesCount(post.likesCount)
        }

        fun bindComments(post: PostItem) {
            val commentsToShow = if (post.showAllComments) {
                post.comments
            } else {
                post.comments.take(MAX_VISIBLE_COMMENTS)
            }

            commentAdapter.updateComments(commentsToShow)

            showMoreCommentsBtn.visibility =
                if (post.comments.size > MAX_VISIBLE_COMMENTS && !post.showAllComments) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            showMoreCommentsBtn.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onShowMoreComments(adapterPosition)
                }
            }

            if (commentsToShow.isNotEmpty()) {
                commentsRecyclerView.post {
                    (commentsRecyclerView.layoutManager as? LinearLayoutManager)?.let { manager ->
                        val lastVisible = manager.findLastCompletelyVisibleItemPosition()
                        val shouldScroll =
                            lastVisible == -1 || lastVisible >= commentsToShow.size - 2
                        if (shouldScroll) {
                            commentsRecyclerView.smoothScrollToPosition(commentsToShow.size - 1)
                        }
                    }
                }
            }
        }
    } // This closing brace was missing in your original code

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Set profile image
        val safeIndex = (post.profileImage?.minus(1) ?: 0).coerceIn(0, profileImages.size - 1)
        holder.pfpIcon.setImageResource(profileImages[safeIndex])

        // Set verified badge
        holder.verifiedBadge.visibility = if (post.isVerified) View.VISIBLE else View.GONE

        // Set usernames
        holder.username.text = post.username
        holder.usernameOnCaption.text = "${post.username} :"

        // Set Ates
        holder.updateLikeButton(post, currentUsername)


        // Set post image
        when {
            !post.postImageData.isNullOrEmpty() -> {
                try {
                    val bitmap = post.postImageData.base64ToBitmap()
                    holder.postImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    holder.postImage.setImageResource(R.drawable.defaultpfpicon)
                    Log.e("PostsAdapter", "Error loading Base64 image", e)
                }
            }

            else -> {
                holder.postImage.setImageResource(R.drawable.defaultpfpicon)
            }
        }

        // Set like button click listener
        holder.likeButton.setOnClickListener {
            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                onLikeClicked(holder.adapterPosition)
            }
        }

        // Set caption
        holder.caption.text = "- ${post.caption}"

        // Bind comments
        holder.bindComments(post)

        // Handle comment posting
        holder.postCommentButton.setOnClickListener {
            val commentText = holder.commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                onCommentAdded(position, post.username, commentText)
                holder.commentInput.text.clear()
            }
        }
    }

    override fun getItemCount() = posts.size

    private fun formatLikesCount(count: Int): String {
        return when {
            count == 0 -> "No one Ate"
            count < 1_000 -> "$count Ate"
            count < 1_000_000 -> "%.1fk Ate".format(count / 1_000f)
            else -> "%.1fm Ate".format(count / 1_000_000f)
        }
    }

    private fun String.base64ToBitmap(): Bitmap {
        val decodedBytes = Base64.decode(this, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun Bitmap.bitmapToBase64(): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}