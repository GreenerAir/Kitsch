package com.k.kitsch.postCarp

import com.k.kitsch.postCarp.comments.Comment
import java.util.UUID

data class PostItem(
    val postId: String = UUID.randomUUID().toString(),
    val profileImage: Int? = null,
    val username: String = "",
    val isVerified: Boolean = false,
    val postImageData: String? = null,
    val likesCount: Int = 0,
    val caption: String = "",
    val likedBy: List<String> = emptyList(),
    var comments: List<Comment> = emptyList(),
    var showAllComments: Boolean = false
)

