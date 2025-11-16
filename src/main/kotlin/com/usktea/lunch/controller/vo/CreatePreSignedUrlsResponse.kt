package com.usktea.lunch.controller.vo

import java.net.URL
import java.util.UUID

data class CreatePreSignedUrlsResponse(
    val preSignedUrls: List<PreSignedUrl>,
) {
    data class PreSignedUrl(
        val name: UUID,
        val url: URL,
    )
}
