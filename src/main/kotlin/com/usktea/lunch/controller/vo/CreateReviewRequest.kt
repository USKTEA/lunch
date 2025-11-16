package com.usktea.lunch.controller.vo

import java.net.URL
import java.util.UUID

data class CreateReviewRequest(
    val restaurantManagementNumber: String,
    val rating: Int,
    val content: String,
    val imageNames: List<UUID>,
    val imageUrls: List<URL>,
)
