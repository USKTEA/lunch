package com.usktea.lunch.repository

import com.usktea.lunch.entity.TokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface TokenRepository : JpaRepository<TokenEntity, Long> {
    fun findByRefreshTokenAndExpiresAtAfter(
        token: String,
        before: OffsetDateTime,
    ): TokenEntity?
}
