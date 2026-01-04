package com.usktea.lunch.controller.vo

data class SearchPlacesResponse(
    val places: List<PlaceVo>,
) {
    data class PlaceVo(
        val managementNumber: String,
        val name: String,
        val mainCategory: String?,
        val detailCategory: String?,
        val address: String?,
        val distance: Int,
        val walkTime: Int,
        val averageRating: Double?,
        val reviewCount: Int,
        val averagePrice: Int?,
        val latitude: Double,
        val longitude: Double,
    )
}
