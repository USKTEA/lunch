package com.usktea.lunch.util

import com.usktea.lunch.common.TokenType
import com.usktea.lunch.common.TokenVo
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Component
class JwtUtil(
    private val jwtEncoder: JwtEncoder,
    private val jwtDecoder: JwtDecoder,
    @Value("\${custom.jwt.expiration.access-token}")
    private val accessTokenExpiration: Duration,
    @Value("\${custom.jwt.expiration.refresh-token}")
    private val refreshTokenExpiration: Duration,
    @Value("\${custom.jwt.issuer}")
    private val issuer: String,
) {
    private val secureRandom = SecureRandom()
    private val base64Encoder = Base64.getEncoder()

    fun generate(userId: Long): TokenVo {
        val now = Instant.now()
        val expiresAt = now.plus(accessTokenExpiration)

        val claimSet =
            JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(userId.toString())

        val accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claimSet.build())).tokenValue
        val refreshToken = base64Encoder.encodeToString(ByteArray(64).apply { secureRandom.nextBytes(this) })

        return TokenVo(
            accessToken = accessToken,
            issuedAt = now,
            expiresIn = accessTokenExpiration.seconds,
            expiresAt = expiresAt,
            tokenType = TokenType.BEARER,
            refreshToken = refreshToken,
            refreshTokenExpiresIn = refreshTokenExpiration,
        )
    }

    fun validate(token: String): Jwt {
        return jwtDecoder.decode(token)
    }
}
