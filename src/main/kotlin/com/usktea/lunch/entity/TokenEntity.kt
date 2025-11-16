package com.usktea.lunch.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "token",
    schema = "lunch",
    indexes = [
        Index(name = "idx_token_refresh_token", columnList = "refreshToken"),
        Index(name = "idx_token_family_id", columnList = "familyId"),
        Index(name = "idx_token_user_id", columnList = "userId"),
        Index(name = "idx_token_expires_at", columnList = "expiresAt"),
    ],
)
class TokenEntity(
    val familyId: UUID,
    val userId: Long,
    @Column(unique = true)
    val refreshToken: String,
    val issuedAt: OffsetDateTime,
    val expiresAt: OffsetDateTime,
    var usedAt: OffsetDateTime? = null,
    var revokedAt: OffsetDateTime? = null,
    val generation: Int = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    fun use(now: OffsetDateTime) {
        this.usedAt = now
    }
}
