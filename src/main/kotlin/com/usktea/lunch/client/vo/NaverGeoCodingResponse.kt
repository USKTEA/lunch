package com.usktea.lunch.client.vo

data class NaverGeoCodingResponse(
    val status: String,
    val meta: Meta,
    val addresses: List<Address>,
) {
    data class Meta(
        val totalCount: Int,
        val page: Int,
        val count: Int,
    )

    data class Address(
        val roadAddress: String?,
        val jibunAddress: String?,
        val englishAddress: String?,
        val addressElements: List<AddressElement>,
        val x: String,
        val y: String,
    )

    data class AddressElement(
        val types: List<String>,
        val longName: String?,
        val shortName: String?,
    )
}
