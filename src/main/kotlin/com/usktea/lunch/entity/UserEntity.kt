package com.usktea.lunch.entity

import com.usktea.lunch.entity.common.AuditingBaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "users", schema = "lunch")
class UserEntity(
    val email: String? = null,
    @Column(unique = true, nullable = false)
    var nickname: String,
    var loginId: String? = null,
    var password: String? = null,
    var lastLoginAt: OffsetDateTime = OffsetDateTime.now(),
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val externalIdentities: MutableList<UserIdentityEntity> = mutableListOf(),
) : AuditingBaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    fun linkExternalProvider(externalIdentity: UserIdentityEntity) {
        this.externalIdentities.add(externalIdentity)
    }

    fun loginIn(now: OffsetDateTime) {
        this.lastLoginAt = now
    }

    fun linkExternalProvider(
        provider: UserIdentityEntity.AuthProvider,
        subject: String,
    ) {
        val userIdentityEntity =
            UserIdentityEntity(
                user = this,
                provider = provider,
                subject = subject,
                linkedAt = OffsetDateTime.now(),
            )

        this.externalIdentities.add(userIdentityEntity)
    }
}
