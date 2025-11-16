package com.usktea.lunch.controller

import com.usktea.lunch.common.CursorBasedPage
import com.usktea.lunch.config.auth.CustomAuthenticationToken
import com.usktea.lunch.controller.vo.CreateReviewRequest
import com.usktea.lunch.controller.vo.GetReviewsRatingResponse
import com.usktea.lunch.controller.vo.ReviewVo
import com.usktea.lunch.service.api.ReviewApiService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ReviewController(
    private val reviewApiService: ReviewApiService,
) {
    @PostMapping("/api/reviews")
    fun createReview(
        authentication: CustomAuthenticationToken,
        @RequestBody request: CreateReviewRequest,
    ) {
        reviewApiService.createReview(authentication.userId, request)
    }

    @GetMapping("/api/reviews/{restaurant-management-number}")
    fun getReviews(
        @PathVariable("restaurant-management-number") restaurantManagementNumber: String,
        @RequestParam(required = true) size: Int,
        @RequestParam(required = false, defaultValue = "createdAt,desc") sort: String,
        @RequestParam(required = false) cursor: Long?,
    ): CursorBasedPage<ReviewVo> {
        return reviewApiService.getReviewsAfterCursor(restaurantManagementNumber, size, sort, cursor)
    }

    @GetMapping("/api/reviews/{restaurant-management-number}/rating")
    fun getReviewsRating(
        @PathVariable("restaurant-management-number") restaurantManagementNumber: String,
    ): GetReviewsRatingResponse {
        return reviewApiService.getReviewsRating(restaurantManagementNumber)
    }
}
