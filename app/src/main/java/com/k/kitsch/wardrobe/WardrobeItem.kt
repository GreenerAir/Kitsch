package com.k.kitsch.wardrobe

import java.util.UUID

data class WardrobeItem(
    val idWardrobeItem: String = UUID.randomUUID().toString(),
    val wardrobeImageData: String? = null,
    val category: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
