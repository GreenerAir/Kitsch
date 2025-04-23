package com.k.kitsch.postCarp.comments

import java.util.UUID

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)