package com.usktea.lunch.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.usktea.lunch.entity.ReviewEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRepository : JpaRepository<ReviewEntity, Long>, KotlinJdslJpqlExecutor {
    fun deleteByRestaurantManagementNumber(restaurantManagementNumber: String)
}
