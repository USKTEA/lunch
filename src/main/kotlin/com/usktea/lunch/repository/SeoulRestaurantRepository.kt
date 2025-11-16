package com.usktea.lunch.repository

import com.usktea.lunch.entity.SeoulRestaurantEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SeoulRestaurantRepository : JpaRepository<SeoulRestaurantEntity, Long> {
    fun findFirstByOrderByIdDesc(): SeoulRestaurantEntity?
}
