package com.usktea.lunch.service.entity

import com.usktea.lunch.entity.AuthorizationSessionEntity
import com.usktea.lunch.repository.AuthorizationSessionRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthorizationSessionEntityService(
    private val authorizationSessionRepository: AuthorizationSessionRepository,
) {
    fun save(authorizationSession: AuthorizationSessionEntity): AuthorizationSessionEntity {
        return authorizationSessionRepository.save(authorizationSession)
    }

    fun findByCode(code: UUID): AuthorizationSessionEntity? {
        return authorizationSessionRepository.findByIdOrNull(code)
    }
}
