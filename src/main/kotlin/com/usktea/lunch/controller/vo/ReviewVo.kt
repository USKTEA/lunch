package com.usktea.lunch.controller.vo

import java.net.URL
import java.time.OffsetDateTime

data class ReviewVo(
    val restaurantManagementNumber: String,
    val reviewerId: Long,
    val reviewerNickname: String,
    val rating: Int,
    val content: String,
    val imageUrls: List<URL>,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
