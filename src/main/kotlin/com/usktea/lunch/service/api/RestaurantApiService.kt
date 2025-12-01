package com.usktea.lunch.service.api

import com.uber.h3core.H3Core
import com.uber.h3core.PolygonToCellsFlags
import com.uber.h3core.util.LatLng
import com.usktea.lunch.controller.vo.GetRestaurantBusinessInfoResponse
import com.usktea.lunch.controller.vo.SearchRestaurantResponse
import com.usktea.lunch.service.entity.RestaurantEntityService
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import kotlin.String

@Service
class RestaurantApiService(
    private val h3core: H3Core,
    private val restaurantEntityService: RestaurantEntityService,
) {
    fun searchRestaurants(
        boundary: String,
        zoomLevel: Int,
    ): SearchRestaurantResponse {
        val h3Resolution = toH3Resolution(zoomLevel)
        val boundary = toPolygon(boundary)
        val h3CellIndices = getH3CellIndices(boundary, h3Resolution)

        val restaurants = restaurantEntityService.findAllRestaurantsH3IndicesIn(h3CellIndices)

        return SearchRestaurantResponse(
            restaurants =
                restaurants.map {
                    RestaurantVo(
                        restaurantManagementNumber = it.managementNumber,
                        name = it.name,
                        coordinate =
                            Coordinate(
                                x = it.location.x,
                                y = it.location.y,
                            ),
                        mainCategory = it.mainCategory,
                        detailCategory = it.detailCategory,
                    )
                },
        )
    }

    fun getRestaurantBusinessInfo(restaurantManagementNumber: String): GetRestaurantBusinessInfoResponse {
        val restaurant = restaurantEntityService.findByManagementNumber(restaurantManagementNumber) ?: throw EntityNotFoundException()

        return GetRestaurantBusinessInfoResponse(
            restaurantManagementNumber = restaurantManagementNumber,
            name = restaurant.name,
            contact = restaurant.contact,
            link = restaurant.externalLink,
            businessHours =
                restaurant.businessHours.sortedBy { it.day.order }.map {
                    GetRestaurantBusinessInfoResponse.BusinessHourVo(
                        day = it.day,
                        openAt = it.openAt,
                        closeAt = it.closeAt,
                        breakTimeStartAt = it.breakTimeStartAt,
                        breakTimeEndAt = it.breakTimeEndAt,
                        isOpen = it.isOpen,
                    )
                },
            menus =
                restaurant.menus.map {
                    GetRestaurantBusinessInfoResponse.MenuVo(
                        name = it.name,
                        price = it.price,
                        isRepresentative = it.isRepresentative,
                    )
                }.sortedWith(
                    compareByDescending<GetRestaurantBusinessInfoResponse.MenuVo> { it.isRepresentative }
                        .thenBy { it.name },
                ),
            summary = restaurant.summary,
            priceRange =
                restaurant.priceRange?.let {
                    GetRestaurantBusinessInfoResponse.PriceRangeVo(
                        minimum = it.minimum,
                        maximum = it.maximum,
                    )
                },
            mainCategory = restaurant.mainCategory,
            detailCategory = restaurant.detailCategory,
        )
    }

    private fun getH3CellIndices(
        polygon: Polygon,
        resolution: Int,
    ): List<String> {
        return h3core.polygonToCellAddressesExperimental(
            buildList {
                add(LatLng(polygon.southWest.y, polygon.southWest.x))
                add(LatLng(polygon.southEast.y, polygon.southEast.x))
                add(LatLng(polygon.nortEast.y, polygon.nortEast.x))
                add(LatLng(polygon.northWest.y, polygon.northWest.x))
            },
            emptyList(),
            resolution,
            PolygonToCellsFlags.containment_overlapping,
        )
    }

    /**
     * boundaries=se.lon;se.lat;nw.lon;nw.lat
     */
    private fun toPolygon(boundary: String): Polygon {
        val coordinates = boundary.split(";")

        val seLon = coordinates[0].toDouble()
        val seLat = coordinates[1].toDouble()
        val nwLon = coordinates[2].toDouble()
        val nwLat = coordinates[3].toDouble()

        return Polygon(
            southEast = Coordinate(x = seLon, y = seLat),
            southWest = Coordinate(x = nwLon, y = seLat),
            northWest = Coordinate(x = nwLon, y = nwLat),
            nortEast = Coordinate(x = seLon, y = nwLat),
        )
    }

    /**
     | 네이버 지도 Zoom Level | 대략적 거리 스케일 | H3 Resolution | 셀 지름(약) | 용도 |
     |------------------------|-------------------|--------------------|-------------|------|
     | 16 | 약 150m | 9 | ~150m | 상권 단위 탐색 / 거리 중심 |
     | 17 | 약 100m | 10 | ~70m | 거리 단위 / 주요 도로 |
     | 18 | 약 50m | 10 | ~35m | 개별 매장 탐색 (기본 보기) |
     | 19 | 약 30m | 11 | ~20m | 세밀 보기 / 건물 단위 |
     */
    private fun toH3Resolution(zoomLevel: Int): Int {
        return when (zoomLevel) {
            16 -> 9
            17 -> 10
            18 -> 10
            19 -> 11
            else -> 10
        }
    }

    data class Polygon(
        val southWest: Coordinate,
        val southEast: Coordinate,
        val nortEast: Coordinate,
        val northWest: Coordinate,
    )

    data class Coordinate(
        // lon 경도 동서
        val x: Double,
        // lat 위도 남북
        val y: Double,
    )

    data class RestaurantVo(
        val restaurantManagementNumber: String,
        val name: String,
        val coordinate: Coordinate,
        val mainCategory: String?,
        val detailCategory: String?,
    )
}
