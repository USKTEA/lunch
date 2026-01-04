package com.usktea.lunch.service.api

import com.uber.h3core.H3Core
import com.uber.h3core.PolygonToCellsFlags
import com.uber.h3core.util.LatLng
import com.usktea.lunch.common.Category
import com.usktea.lunch.controller.vo.GetRestaurantBusinessInfoResponse
import com.usktea.lunch.controller.vo.SearchPlacesResponse
import com.usktea.lunch.controller.vo.SearchRestaurantResponse
import com.usktea.lunch.entity.RestaurantEntity
import com.usktea.lunch.repository.RestaurantRepository
import com.usktea.lunch.service.entity.RestaurantEntityService
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import kotlin.String
import kotlin.math.roundToInt

@Service
class RestaurantApiService(
    private val h3core: H3Core,
    private val restaurantEntityService: RestaurantEntityService,
    private val restaurantRepository: RestaurantRepository,
) {
    fun searchPlaces(
        centerLat: Double,
        centerLon: Double,
        keyword: String?,
        mainCategory: Category?,
        sortBy: String,
        maxDistance: Int,
    ): SearchPlacesResponse {
        val (resolution, kRing) = getH3SearchParams(maxDistance)
        val centerCell = h3core.latLngToCellAddress(centerLat, centerLon, resolution)
        val h3Cells = h3core.gridDisk(centerCell, kRing)

        val places =
            restaurantRepository.searchPlaces(
                h3Cells = h3Cells.toTypedArray(),
                centerLat = centerLat,
                centerLon = centerLon,
                maxDistance = maxDistance,
                keyword = keyword?.ifBlank { null },
                mainCategory = mainCategory?.korean,
                sortBy = sortBy,
                limit = 50,
            )

        return SearchPlacesResponse(
            places =
                places.map { place ->
                    SearchPlacesResponse.PlaceVo(
                        managementNumber = place.managementNumber,
                        name = place.name,
                        mainCategory = place.mainCategory,
                        detailCategory = place.detailCategory,
                        address = place.address,
                        distance = place.distance,
                        walkTime = calculateWalkTime(place.distance),
                        averageRating = if (place.reviewCount > 0) (place.averageRating * 10).roundToInt() / 10.0 else null,
                        reviewCount = place.reviewCount,
                        averagePrice = calculateAveragePrice(place.priceRangeMinimum, place.priceRangeMaximum),
                        latitude = place.latitude,
                        longitude = place.longitude,
                    )
                },
        )
    }

    private fun calculateWalkTime(distanceMeters: Int): Int {
        // 평균 도보 속도: 분당 80m
        return (distanceMeters / 80.0).roundToInt().coerceAtLeast(1)
    }

    private fun calculateAveragePrice(
        minimum: Int?,
        maximum: Int?,
    ): Int? {
        if (minimum == null || maximum == null) return null
        return (minimum + maximum) / 2
    }

    fun searchRestaurants(
        boundary: String,
        zoomLevel: Int,
    ): SearchRestaurantResponse {
        val h3Resolution = toH3Resolution(zoomLevel)
        val polygon = toPolygon(boundary)
        val h3CellIndices = getH3CellIndices(polygon, h3Resolution)
        val restaurants = restaurantEntityService.findAllRestaurantsH3IndicesIn(h3CellIndices)

        val h3IndicesAndRestaurantsPairs =
            buildMap<String, MutableList<RestaurantEntity>> {
                restaurants.forEach { restaurant ->
                    restaurant.h3Indices
                        .filter { it in h3CellIndices }
                        .forEach { h3Index ->
                            getOrPut(h3Index) { mutableListOf() }.add(restaurant)
                        }
                }
            }

        val clusters =
            h3IndicesAndRestaurantsPairs.entries.map { (h3Index, cellRestaurants) ->
                val center = h3core.cellToLatLng(h3Index)
                val cellBoundary = h3core.cellToBoundary(h3Index)

                ClusterVo(
                    h3Index = h3Index,
                    center = Coordinate(x = center.lng, y = center.lat),
                    boundary = cellBoundary.map { Coordinate(x = it.lng, y = it.lat) },
                    restaurants =
                        cellRestaurants.map {
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

        return SearchRestaurantResponse(
            clusters = clusters,
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
     * 거리 기반 검색을 위한 H3 파라미터 결정
     *
     * | 거리    | Resolution | 셀 지름  | k-ring | 셀 수 | 커버 범위 |
     * |---------|------------|---------|--------|-------|----------|
     * | 300m    | 9          | ~174m   | 2      | 19    | ~520m    |
     * | 500m    | 9          | ~174m   | 3      | 37    | ~700m    |
     * | 1000m   | 8          | ~460m   | 3      | 37    | ~1380m   |
     *
     * k-ring을 보수적으로 설정하여 경계에서 누락 방지
     * 정확한 거리는 ST_DistanceSphere로 후검증
     */
    private fun getH3SearchParams(maxDistance: Int): Pair<Int, Int> {
        return when {
            maxDistance <= 300 -> 9 to 2
            maxDistance <= 500 -> 9 to 3
            maxDistance <= 1000 -> 8 to 3
            else -> 8 to 4
        }
    }

    /**
     * | 네이버 지도 Zoom Level | 대략적 거리 스케일 | H3 Resolution | 셀 지름(약) | 용도 |
     * |------------------------|-------------------|--------------------|-------------|------|
     * | 16 | 약 150m | 9 | ~150m | 상권 단위 탐색 / 거리 중심 |
     * | 17 | 약 100m | 10 | ~70m | 거리 단위 / 주요 도로 |
     * | 18 | 약 50m | 10 | ~35m | 개별 매장 탐색 (기본 보기) |
     * | 19 | 약 30m | 11 | ~20m | 세밀 보기 / 건물 단위 |
     */
    private fun toH3Resolution(zoomLevel: Int): Int {
        return when (zoomLevel) {
            14 -> 7
            15 -> 8
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

    data class ClusterVo(
        val h3Index: String,
        val center: Coordinate,
        val boundary: List<Coordinate>,
        val restaurants: List<RestaurantVo>,
    )
}
