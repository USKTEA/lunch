package com.usktea.lunch.controller.vo

import com.usktea.lunch.service.api.RestaurantApiService

data class SearchRestaurantResponse(
    val restaurants: List<RestaurantApiService.RestaurantVo>,
)
