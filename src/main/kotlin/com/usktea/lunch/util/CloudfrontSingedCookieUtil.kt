package com.usktea.lunch.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Base64

@Component
class CloudfrontSingedCookieUtil(
    @Value("\${aws.cloudfront.domain}")
    private val domain: String,
    @Value("\${aws.cloudfront.key-pair-id}")
    private val keyPairId: String,
    @Value("\${aws.cloudfront.private-key}")
    privateKeyString: String,
    @Value("\${aws.cloudfront.resource-path}")
    private val resourcePath: String,
    @Value("\${aws.cloudfront.cookie-expiration}")
    private val cookieExpiration: Duration,
    private val cloudfrontUtil: CloudFrontUtilities,
) {
    private val privateKey: PrivateKey =
        KeyFactory
            .getInstance("RSA")
            .generatePrivate(
                PKCS8EncodedKeySpec(
                    Base64
                        .getDecoder()
                        .decode(privateKeyString.replace("\\s".toRegex(), "")),
                ),
            )

    fun generateSignedCookie(now: OffsetDateTime): Map<String, String> {
        val cookies =
            cloudfrontUtil.getCookiesForCustomPolicy { builder ->
                builder.keyPairId(keyPairId)
                builder.privateKey(privateKey)
                builder.resourceUrl("https://$domain$resourcePath")
                builder.expirationDate(now.plus(cookieExpiration).toInstant().truncatedTo(ChronoUnit.SECONDS))
                builder.build()
            }

        return mapOf(
            parseHeaderValue(cookies.keyPairIdHeaderValue()),
            parseHeaderValue(cookies.policyHeaderValue()),
            parseHeaderValue(cookies.signatureHeaderValue()),
        )
    }

    private fun parseHeaderValue(headerValue: String): Pair<String, String> {
        val index = headerValue.indexOf('=')
        require(index != -1) { "Invalid CloudFront cookie header format: $headerValue" }
        return headerValue.substring(0, index) to headerValue.substring(index + 1)
    }
}
