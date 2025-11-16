package com.usktea.lunch.controller

import com.usktea.lunch.controller.vo.SearchRestaurantResponse
import com.usktea.lunch.service.api.RestaurantApiService
import org.springframework.web.bind.annotation.GetMapping
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
}
