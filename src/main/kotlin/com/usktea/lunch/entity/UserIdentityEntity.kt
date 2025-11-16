package com.usktea.lunch.entity

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime

@Entity
@Table(
    name = "user_identity",
    schema = "lunch",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_identities_provider_subject",
            columnNames = ["provider", "subject"],
        ),
    ],
    indexes = [
        Index(name = "idx_user_identities_user_id", columnList = "user_id"),
    ],
)
class UserIdentityEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,
    @Enumerated(EnumType.STRING)
    val provider: AuthProvider,
    val subject: String,
    val linkedAt: OffsetDateTime = OffsetDateTime.now(),
) {
    enum class AuthProvider(
        @get:JsonValue
        val value: String,
    ) {
        AZURE("azure"),
    }
}
