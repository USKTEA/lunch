package com.usktea.lunch.service.entity

import com.usktea.lunch.entity.RestaurantEntity
import com.usktea.lunch.repository.RestaurantRepository
import org.locationtech.jts.geom.Point
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class RestaurantEntityService(
    private val jdbcTemplate: JdbcTemplate,
    private val restaurantRepository: RestaurantRepository,
) {
    fun insert(restaurants: List<RestaurantEntity>) {
        if (restaurants.isEmpty()) {
            return
        }

        jdbcTemplate.batchUpdate(batchInsertSql, restaurants, restaurants.size) { ps, restaurant ->
            ps.setString(1, restaurant.managementNumber)
            ps.setString(2, restaurant.name)
            ps.setString(3, restaurant.contact)
            ps.setString(4, restaurant.sido)
            ps.setString(5, restaurant.sigungu)
            ps.setString(6, restaurant.dongmyun)
            ps.setString(7, restaurant.ri)
            ps.setString(8, restaurant.road)
            ps.setString(9, restaurant.buildingNumber)
            ps.setString(10, restaurant.address)
            ps.setString(11, toWKT(restaurant.location))
            ps.setString(12, restaurant.status.name)
            ps.setArray(13, ps.connection.createArrayOf("text", restaurant.h3Indices))
        }
    }

    fun findAllRestaurantsH3IndicesIn(h3CellIndices: List<String>): List<RestaurantEntity> {
        return restaurantRepository.findAllRestaurantsH3IndicesInAndStatusIsOpen(h3CellIndices.toTypedArray())
    }

    fun findAllByManagementNumbers(restaurantManagementNumbers: Set<String>): List<RestaurantEntity> {
        return restaurantRepository.findAllById(restaurantManagementNumbers)
    }

    fun findByManagementNumber(restaurantManagementNumber: String): RestaurantEntity? {
        return restaurantRepository.findByIdOrNull(restaurantManagementNumber)
    }

    private fun toWKT(point: Point?): String? {
        if (point == null) {
            return null
        }
        return "SRID=4326;POINT(${point.x} ${point.y})"
    }

    companion object {
        private val batchInsertSql =
            """
            INSERT INTO lunch.restaurant
            (management_number, name, contact, sido, sigungu, dongmyun, ri, road, building_number, address, location, status, h3_indices, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ST_GeomFromText(?, 4326), ?::text, ?, NOW(), NOW())
            ON CONFLICT (management_number) DO NOTHING
            """.trimIndent()
    }
}
