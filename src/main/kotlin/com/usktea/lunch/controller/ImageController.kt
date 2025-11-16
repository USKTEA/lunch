package com.usktea.lunch.controller

import com.usktea.lunch.controller.vo.CreatePreSignedUrlRequest
import com.usktea.lunch.controller.vo.CreatePreSignedUrlsResponse
import com.usktea.lunch.service.api.ImageApiService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ImageController(
    private val imageApiService: ImageApiService,
) {
    @PostMapping("/api/images/presigned-urls")
    fun createPreSignedUrls(
        @RequestBody request: CreatePreSignedUrlRequest,
    ): CreatePreSignedUrlsResponse {
        return imageApiService.createPreSignedUrls(request)
    }
}
