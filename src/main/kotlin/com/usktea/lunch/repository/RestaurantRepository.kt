package com.usktea.lunch.repository

import com.usktea.lunch.entity.RestaurantEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RestaurantRepository : JpaRepository<RestaurantEntity, String> {
    @Query(
        value = """
        SELECT *
        FROM lunch.restaurant
        WHERE h3_indices && CAST(:h3CellIndices AS text[]) AND status = 'OPEN'
    """,
        nativeQuery = true,
    )
    fun findAllRestaurantsH3IndicesInAndStatusIsOpen(h3CellIndices: Array<String>): List<RestaurantEntity>
}
