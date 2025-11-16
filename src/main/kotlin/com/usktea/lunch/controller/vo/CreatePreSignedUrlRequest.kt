package com.usktea.lunch.controller.vo

import com.usktea.lunch.common.ImageContext
import java.util.UUID

data class CreatePreSignedUrlRequest(
    val context: ImageContext,
    val imageMetas: List<ImageMeta>,
) {
    data class ImageMeta(
        val name: UUID,
        val imageSize: Int,
        val contentType: String,
    )
}
