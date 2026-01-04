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

    @Query(
        value = """
        SELECT r.management_number as managementNumber,
               r.name,
               r.main_category as mainCategory,
               r.detail_category as detailCategory,
               r.address,
               ST_DistanceSphere(r.location, ST_SetSRID(ST_MakePoint(:centerLon, :centerLat), 4326))::int as distance,
               COALESCE(AVG(rv.rating), 0) as averageRating,
               COUNT(rv.id)::int as reviewCount,
               COALESCE(r.minimum, 0) as priceRangeMinimum,
               COALESCE(r.maximum, 0) as priceRangeMaximum,
               ST_Y(r.location) as latitude,
               ST_X(r.location) as longitude
        FROM lunch.restaurant r
        LEFT JOIN lunch.review rv ON r.management_number = rv.restaurant_management_number AND rv.status = 'CREATED'
        WHERE 
          r.h3_indices && CAST(:h3Cells AS text[])
          AND ST_DistanceSphere(r.location, ST_SetSRID(ST_MakePoint(:centerLon, :centerLat), 4326)) <= :maxDistance
          AND r.status = 'OPEN'
          AND (:keyword IS NULL OR r.name ILIKE '%' || :keyword || '%')
          AND (:mainCategory IS NULL OR r.main_category = :mainCategory)
        GROUP BY r.management_number
        ORDER BY
            CASE WHEN :sortBy = 'distance' THEN ST_DistanceSphere(r.location, ST_SetSRID(ST_MakePoint(:centerLon, :centerLat), 4326)) END ASC,
            CASE WHEN :sortBy = 'rating' THEN COALESCE(AVG(rv.rating), 0) END DESC,
            CASE WHEN :sortBy = 'reviewCount' THEN COUNT(rv.id) END DESC
        LIMIT :limit
    """,
        nativeQuery = true,
    )
    fun searchPlaces(
        h3Cells: Array<String>,
        centerLat: Double,
        centerLon: Double,
        maxDistance: Int,
        keyword: String?,
        mainCategory: String?,
        sortBy: String,
        limit: Int,
    ): List<PlaceSearchProjection>
}

interface PlaceSearchProjection {
    val managementNumber: String
    val name: String
    val mainCategory: String?
    val detailCategory: String?
    val address: String?
    val distance: Int
    val averageRating: Double
    val reviewCount: Int
    val priceRangeMinimum: Int?
    val priceRangeMaximum: Int?
    val latitude: Double
    val longitude: Double
}
