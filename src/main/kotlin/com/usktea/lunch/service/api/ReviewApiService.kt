package com.usktea.lunch.service.api

import com.usktea.lunch.common.CursorBasedPage
import com.usktea.lunch.common.logger
import com.usktea.lunch.controller.vo.CreateReviewRequest
import com.usktea.lunch.controller.vo.GetReviewsRatingResponse
import com.usktea.lunch.controller.vo.ReviewVo
import com.usktea.lunch.entity.ReviewEntity
import com.usktea.lunch.service.client.S3ClientService
import com.usktea.lunch.service.entity.ImageEntityService
import com.usktea.lunch.service.entity.ReviewEntityService
import com.usktea.lunch.service.entity.UserEntityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewApiService(
    private val s3ClientService: S3ClientService,
    private val imageEntityService: ImageEntityService,
    private val reviewEntityService: ReviewEntityService,
    private val userEntityService: UserEntityService,
) {
    @Transactional
    fun createReview(
        userId: Long,
        request: CreateReviewRequest,
    ) {
        val review =
            ReviewEntity(
                restaurantManagementNumber = request.restaurantManagementNumber,
                reviewerId = userId,
                rating = request.rating,
                content = request.content,
                imageUrls = request.imageUrls,
            )

        val savedReview = reviewEntityService.save(review)
        val images = imageEntityService.findAllByNames(request.imageNames)

        runBlocking(Dispatchers.IO) {
            try {
                images.map {
                    async {
                        s3ClientService.markAsAttached(it.objectKey)
                    }
                }.awaitAll()
            } catch (ex: Exception) {
                logger.error("Failed to change object attached tag value to true. Request: {}", request, ex)
                throw ex
            }
        }

        images.forEach { image -> image.attached(savedReview.id) }
    }

    fun getReviewsAfterCursor(
        restaurantManagementNumber: String,
        size: Int,
        sort: String,
        cursor: Long?,
    ): CursorBasedPage<ReviewVo> {
        val reviews = reviewEntityService.findReviewsWithCursor(restaurantManagementNumber, size, parseSort(sort), cursor)
        val users = userEntityService.findAllById(reviews.content.mapTo(mutableSetOf()) { it.reviewerId }).associateBy { it.id }

        return reviews.map { review ->
            ReviewVo(
                restaurantManagementNumber = review.restaurantManagementNumber,
                reviewerId = review.reviewerId,
                reviewerNickname = users[review.reviewerId]?.nickname ?: "익명의 사용자",
                rating = review.rating,
                content = review.content,
                imageUrls = review.imageUrls,
                createdAt = review.createdAt,
            )
        }
    }

    fun getReviewsRating(restaurantManagementNumber: String): GetReviewsRatingResponse {
        val reviewRating = reviewEntityService.getReviewsRating(restaurantManagementNumber)

        if (reviewRating == null) {
            return GetReviewsRatingResponse.empty(restaurantManagementNumber)
        }

        return GetReviewsRatingResponse(
            restaurantManagementNumber = restaurantManagementNumber,
            average = reviewRating.average,
            totalReviews = reviewRating.totalReviews,
            rating5Count = reviewRating.rating5Count,
            rating4Count = reviewRating.rating4Count,
            rating3Count = reviewRating.rating3Count,
            rating2Count = reviewRating.rating2Count,
            rating1Count = reviewRating.rating1Count,
        )
    }

    private fun parseSort(sortString: String): Sort {
        val params = sortString.split(",")
        val property = params.first().trim()
        val orderBy = params.getOrNull(1)?.trim() ?: "desc"

        return Sort.by(Sort.Direction.fromString(orderBy), property)
    }
}
