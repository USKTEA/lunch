package com.usktea.lunch.controller.vo

data class GetReviewsRatingResponse(
    val restaurantManagementNumber: String,
    val average: Double = 0.0,
    val totalReviews: Long = 0L,
    val rating5Count: Long = 0L,
    val rating4Count: Long = 0L,
    val rating3Count: Long = 0L,
    val rating2Count: Long = 0L,
    val rating1Count: Long = 0L,
) {
    companion object {
        fun empty(restaurantManagementNumber: String): GetReviewsRatingResponse {
            return GetReviewsRatingResponse(
                restaurantManagementNumber = restaurantManagementNumber,
            )
        }
    }
}
