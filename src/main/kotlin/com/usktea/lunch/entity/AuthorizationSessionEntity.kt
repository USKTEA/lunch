package com.usktea.lunch.entity

import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "authorization_session", schema = "lunch")
class AuthorizationSessionEntity(
    @Id
    val code: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING)
    val provider: UserIdentityEntity.AuthProvider,
    val subject: String,
    val redirectUri: String,
    val state: UUID,
    @Embedded
    val userProfile: UserProfile,
    val issuedAt: OffsetDateTime = OffsetDateTime.now(),
    val expiresAt: OffsetDateTime,
    var usedAt: OffsetDateTime? = null,
) {
    fun used(now: OffsetDateTime) {
        this.usedAt = now
    }

    @Embeddable
    data class UserProfile(
        val name: String?,
        val email: String?,
    )
}
