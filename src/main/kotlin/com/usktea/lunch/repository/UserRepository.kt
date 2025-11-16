package com.usktea.lunch.repository

import com.usktea.lunch.entity.UserEntity
import com.usktea.lunch.entity.UserIdentityEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<UserEntity, Long> {
    @Query(
        """
            SELECT u FROM UserEntity u
            JOIN u.externalIdentities i 
            WHERE i.provider = :provider AND i.subject = :subject
        """,
    )
    fun findByProviderAndSubject(
        provider: UserIdentityEntity.AuthProvider,
        subject: String,
    ): UserEntity?
}
