package com.usktea.lunch.service.client

import com.usktea.lunch.common.ImageContext
import com.usktea.lunch.controller.vo.CreatePreSignedUrlRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest
import software.amazon.awssdk.services.s3.model.Tag
import software.amazon.awssdk.services.s3.model.Tagging
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import java.time.Duration

@Service
class S3ClientService(
    @Value("\${aws.s3.bucket}")
    private val bucket: String,
    private val s3Presigner: S3Presigner,
    private val s3Client: S3Client,
) {
    suspend fun createPreSignedUrl(
        context: ImageContext,
        meta: CreatePreSignedUrlRequest.ImageMeta,
    ): Pair<String, PresignedPutObjectRequest> {
        val objectKey = getObjectKey(context, meta)
        val preSignedRequest =
            s3Presigner.presignPutObject { presignRequest ->
                presignRequest.signatureDuration(expiration)
                    .putObjectRequest { requestBuilder ->
                        requestBuilder.bucket(bucket)
                        requestBuilder.key(objectKey)
                        requestBuilder.contentType(meta.contentType)
                        requestBuilder.tagging(attachedFalseTag)
                    }
            }

        return objectKey to preSignedRequest
    }

    suspend fun markAsAttached(objectKey: String) {
        val request =
            PutObjectTaggingRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .tagging(attachedTrueTag)
                .build()

        s3Client.putObjectTagging(request)
    }

    private fun getObjectKey(
        context: ImageContext,
        meta: CreatePreSignedUrlRequest.ImageMeta,
    ): String {
        val folder =
            when (context) {
                ImageContext.REVIEW -> "review"
            }

        val extension =
            when (meta.contentType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> throw IllegalArgumentException("Unsupported content type")
            }

        return "image/$folder/${meta.name}.$extension"
    }

    companion object {
        private val expiration = Duration.ofMinutes(5)
        private val attachedFalseTag =
            Tagging.builder()
                .tagSet(
                    Tag.builder()
                        .key("attached")
                        .value(false.toString())
                        .build(),
                ).build()

        private val attachedTrueTag =
            Tagging.builder()
                .tagSet(
                    Tag.builder()
                        .key("attached")
                        .value(true.toString())
                        .build(),
                )
                .build()
    }
}
