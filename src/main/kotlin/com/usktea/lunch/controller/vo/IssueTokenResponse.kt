package com.usktea.lunch.controller.vo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.usktea.lunch.common.TokenType
import java.time.Duration
import java.time.Instant

data class IssueTokenResponse(
    val accessToken: String,
    val issuedAt: Instant,
    val tokenType: TokenType,
    val expiresIn: Long,
    val expiresAt: Instant,
    @JsonIgnore
    val refreshToken: String,
    @JsonIgnore
    val refreshTokenExpiresIn: Duration,
    @JsonIgnore
    val cloudfrontSignedCookies: Map<String, String>,
)
