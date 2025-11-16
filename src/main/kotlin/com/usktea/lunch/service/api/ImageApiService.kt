package com.usktea.lunch.service.api

import com.usktea.lunch.common.logger
import com.usktea.lunch.config.auth.CustomAuthenticationToken
import com.usktea.lunch.controller.vo.CreatePreSignedUrlRequest
import com.usktea.lunch.controller.vo.CreatePreSignedUrlsResponse
import com.usktea.lunch.entity.ImageEntity
import com.usktea.lunch.service.client.S3ClientService
import com.usktea.lunch.service.entity.ImageEntityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class ImageApiService(
    private val s3ClientService: S3ClientService,
    private val imageEntityService: ImageEntityService,
) {
    fun createPreSignedUrls(request: CreatePreSignedUrlRequest): CreatePreSignedUrlsResponse {
        val authentication = SecurityContextHolder.getContext().authentication as CustomAuthenticationToken

        val isValid = validateRequest(request)

        if (!isValid) {
            throw IllegalArgumentException("invalid image meta")
        }

        val imageEntities =
            runBlocking(Dispatchers.IO) {
                try {
                    request.imageMetas.map { meta ->
                        async {
                            val (objectKey, presignedPutObjectRequest) = s3ClientService.createPreSignedUrl(request.context, meta)

                            ImageEntity(
                                name = meta.name,
                                userId = authentication.userId,
                                context = request.context,
                                objectKey = objectKey,
                                url = presignedPutObjectRequest.url(),
                            )
                        }
                    }.awaitAll()
                } catch (e: Exception) {
                    logger.error("Failed to retrieve PreSignedUrls", e)
                    throw e
                }
            }

        try {
            imageEntityService.saveAll(imageEntities)
        } catch (e: Exception) {
            logger.error("Failed to save PreSignedUrls", e)
            throw e
        }

        return CreatePreSignedUrlsResponse(
            preSignedUrls =
                imageEntities.map {
                    CreatePreSignedUrlsResponse.PreSignedUrl(
                        name = it.name,
                        url = it.url,
                    )
                },
        )
    }

    private fun validateRequest(request: CreatePreSignedUrlRequest): Boolean {
        return request.imageMetas.all {
            it.imageSize <= MAX_SIZE && it.contentType in supportedContentTypes
        }
    }

    companion object {
        private val supportedContentTypes =
            listOf(
                "image/jpeg",
                "image/png",
                "image/webp",
            )

        private const val MAX_SIZE = 10 * 1024 * 1024
    }
}
