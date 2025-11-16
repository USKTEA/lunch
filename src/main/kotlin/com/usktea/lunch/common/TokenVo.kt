package com.usktea.lunch.common

import java.time.Duration
import java.time.Instant

data class TokenVo(
    val accessToken: String,
    val issuedAt: Instant,
    val tokenType: TokenType,
    val expiresIn: Long,
    val expiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresIn: Duration,
)
