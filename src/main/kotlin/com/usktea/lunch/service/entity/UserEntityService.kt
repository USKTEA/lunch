package com.usktea.lunch.service.entity

import com.usktea.lunch.entity.UserEntity
import com.usktea.lunch.entity.UserIdentityEntity
import com.usktea.lunch.repository.UserRepository
import com.usktea.lunch.util.NicknameGenerator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserEntityService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun findByExternalIdentityProviderAndSubject(
        provider: UserIdentityEntity.AuthProvider,
        subject: String,
    ): UserEntity? {
        return userRepository.findByProviderAndSubject(provider, subject)
    }

    @Transactional
    fun createUser(user: UserEntity): UserEntity {
        return createWithRetry(user)
    }

    private fun createWithRetry(user: UserEntity): UserEntity {
        try {
            return userRepository.save(user)
        } catch (exception: DataIntegrityViolationException) {
            if (exception.message?.contains("duplicate key") == true) {
                return createWithRetry(
                    UserEntity(
                        email = user.email,
                        nickname = NicknameGenerator.fromUuid(UUID.randomUUID()),
                        loginId = user.loginId,
                        password = user.password,
                        lastLoginAt = user.lastLoginAt,
                        externalIdentities = user.externalIdentities,
                    ),
                )
            }

            throw exception
        }
    }

    fun findById(userId: Long): UserEntity? {
        return userRepository.findByIdOrNull(userId)
    }

    fun findAllById(ids: Set<Long>): List<UserEntity> {
        return userRepository.findAllById(ids)
    }
}
