package com.usktea.lunch.service.entity

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.spring.data.jpa.extension.createQuery
import com.usktea.lunch.common.CursorBasedPage
import com.usktea.lunch.common.pageOf
import com.usktea.lunch.entity.ReviewEntity
import com.usktea.lunch.repository.ReviewRepository
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewEntityService(
    private val reviewRepository: ReviewRepository,
    private val rendererContext: JpqlRenderContext,
    private val entityManager: EntityManager,
) {
    @Transactional
    fun save(review: ReviewEntity): ReviewEntity {
        return reviewRepository.save(review)
    }

    fun findReviewsWithCursor(
        restaurantManagementNumber: String,
        size: Int,
        sort: Sort,
        cursor: Long?,
    ): CursorBasedPage<ReviewEntity> {
        val isDesc = sort.getOrderFor(ReviewEntity::createdAt.name)?.isDescending ?: true

        val results =
            reviewRepository.findAll(
                limit = size + 1,
            ) {
                select(entity(ReviewEntity::class))
                    .from(entity(ReviewEntity::class))
                    .whereAnd(
                        path(ReviewEntity::restaurantManagementNumber).equal(restaurantManagementNumber),
                        path(ReviewEntity::status).equal(ReviewEntity.ReviewStatus.CREATED),
                        cursor?.let {
                            if (isDesc) {
                                path(ReviewEntity::id).lessThan(it)
                            } else {
                                path(ReviewEntity::id).greaterThan(it)
                            }
                        },
                    )
                    .orderBy(
                        if (isDesc) {
                            path(ReviewEntity::createdAt).desc()
                        } else {
                            path(ReviewEntity::createdAt).asc()
                        },
                    )
            }.filterNotNull()

        val content = results.take(size)
        val hasNext = results.size > size

        return pageOf(content, hasNext) { it.id.toString() }
    }

    fun getReviewsRating(restaurantManagementNumber: String): RestaurantReviewRating? {
        val query =
            jpql {
                selectNew<RestaurantReviewRating>(
                    coalesce(avg(ReviewEntity::rating), 0.0),
                    coalesce(count(entity(ReviewEntity::class)), 0L),
                    coalesce(
                        sum(
                            cast(
                                caseValue(path(ReviewEntity::rating)).`when`(5).then("1").`else`("0"),
                            ).asLong(),
                        ),
                        0L,
                    ),
                    coalesce(
                        sum(
                            cast(
                                caseValue(path(ReviewEntity::rating)).`when`(4).then("1").`else`("0"),
                            ).asLong(),
                        ),
                        0L,
                    ),
                    coalesce(
                        sum(
                            cast(
                                caseValue(path(ReviewEntity::rating)).`when`(3).then("1").`else`("0"),
                            ).asLong(),
                        ),
                        0L,
                    ),
                    coalesce(
                        sum(
                            cast(
                                caseValue(path(ReviewEntity::rating)).`when`(2).then("1").`else`("0"),
                            ).asLong(),
                        ),
                        0L,
                    ),
                    coalesce(
                        sum(
                            cast(
                                caseValue(path(ReviewEntity::rating)).`when`(1).then("1").`else`("0"),
                            ).asLong(),
                        ),
                        0L,
                    ),
                ).from(entity(ReviewEntity::class))
                    .where(
                        path(ReviewEntity::restaurantManagementNumber).eq(restaurantManagementNumber).and(
                            path(ReviewEntity::status).eq(ReviewEntity.ReviewStatus.CREATED),
                        ),
                    )
            }

        return entityManager.createQuery(query, rendererContext).resultList.firstOrNull()
    }

    data class RestaurantReviewRating(
        val average: Double = 0.0,
        val totalReviews: Long,
        val rating5Count: Long,
        val rating4Count: Long,
        val rating3Count: Long,
        val rating2Count: Long,
        val rating1Count: Long,
    )
}
