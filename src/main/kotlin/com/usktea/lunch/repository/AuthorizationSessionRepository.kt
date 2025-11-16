package com.usktea.lunch.repository

import com.usktea.lunch.entity.AuthorizationSessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuthorizationSessionRepository : JpaRepository<AuthorizationSessionEntity, UUID>
