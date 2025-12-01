package com.usktea.lunch.controller.vo

import com.usktea.lunch.common.Day
import java.time.LocalTime

data class GetRestaurantBusinessInfoResponse(
    val restaurantManagementNumber: String,
    val name: String,
    val contact: String?,
    val link: String?,
    val businessHours: List<BusinessHourVo>,
    val menus: List<MenuVo>,
    val summary: String?,
    val priceRange: PriceRangeVo?,
    val mainCategory: String?,
    val detailCategory: String?,
) {
    data class PriceRangeVo(
        val minimum: Int,
        val maximum: Int,
    )

    data class BusinessHourVo(
        val day: Day,
        val openAt: LocalTime,
        val closeAt: LocalTime,
        val breakTimeStartAt: LocalTime? = null,
        val breakTimeEndAt: LocalTime? = null,
        val isOpen: Boolean,
    )

    data class MenuVo(
        val name: String,
        val price: Int,
        val isRepresentative: Boolean,
    )
}
