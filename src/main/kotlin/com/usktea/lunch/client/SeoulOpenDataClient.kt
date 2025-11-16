package com.usktea.lunch.client

import com.usktea.lunch.client.vo.OpenDataResponseWrapper
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "SeoulOpenDataClient", url = "http://openapi.seoul.go.kr:8088")
interface SeoulOpenDataClient {
    @GetMapping("/{appKey}/{responseType}/{serviceName}/{startIndex}/{endIndex}")
    fun getData(
        @PathVariable appKey: String,
        @PathVariable responseType: String,
        @PathVariable serviceName: String,
        @PathVariable startIndex: Int,
        @PathVariable endIndex: Int,
    ): OpenDataResponseWrapper
}
