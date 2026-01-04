package com.usktea.lunch.controller

import com.usktea.lunch.common.Category
import com.usktea.lunch.controller.vo.GetRestaurantBusinessInfoResponse
import com.usktea.lunch.controller.vo.SearchPlacesResponse
import com.usktea.lunch.controller.vo.SearchRestaurantResponse
import com.usktea.lunch.service.api.RestaurantApiService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RestaurantController(
    private val restaurantApiService: RestaurantApiService,
) {
    @GetMapping("/api/restaurants")
    fun searchRestaurants(
        @RequestParam boundary: String,
        @RequestParam zoomLevel: Int,
    ): SearchRestaurantResponse {
        return restaurantApiService.searchRestaurants(boundary = boundary, zoomLevel = zoomLevel)
    }

    @GetMapping("/api/restaurants/{restaurantManagementNumber}")
    fun getRestaurantBusinessInfo(
        @PathVariable restaurantManagementNumber: String,
    ): GetRestaurantBusinessInfoResponse {
        return restaurantApiService.getRestaurantBusinessInfo(restaurantManagementNumber)
    }

    @GetMapping("/api/restaurants/search")
    fun searchPlaces(
        @RequestParam centerLat: Double,
        @RequestParam centerLon: Double,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: Category?,
        @RequestParam(defaultValue = "distance") sortBy: String,
        @RequestParam(defaultValue = "500") maxDistance: Int,
    ): SearchPlacesResponse {
        return restaurantApiService.searchPlaces(
            centerLat = centerLat,
            centerLon = centerLon,
            keyword = keyword,
            mainCategory = category,
            sortBy = sortBy,
            maxDistance = maxDistance,
        )
    }
}
