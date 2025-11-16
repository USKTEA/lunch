package com.usktea.lunch.client

import com.usktea.lunch.client.config.NaverMapApiClientConfig
import com.usktea.lunch.client.vo.NaverGeoCodingResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "NaverMapApiClient", url = "https://maps.apigw.ntruss.com", configuration = [NaverMapApiClientConfig::class])
interface NaverMapApiClient {
    @GetMapping("/map-geocode/v2/geocode")
    fun geoCoding(
        @RequestParam("query") query: String,
    ): NaverGeoCodingResponse
}
