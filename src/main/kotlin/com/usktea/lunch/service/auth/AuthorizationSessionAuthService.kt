package com.usktea.lunch.service.auth

import com.usktea.lunch.entity.AuthorizationSessionEntity
import com.usktea.lunch.entity.UserIdentityEntity
import com.usktea.lunch.service.entity.AuthorizationRequestEntityService
import com.usktea.lunch.service.entity.AuthorizationSessionEntityService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AuthorizationSessionAuthService(
    @Value("\${custom.jwt.expiration.code}")
    private val codeExpiration: Duration,
    private val authorizationRequestEntityService: AuthorizationRequestEntityService,
    private val authorizationSessionEntityService: AuthorizationSessionEntityService,
) {
    fun createSession(authentication: Authentication): AuthorizationSessionEntity {
        val token = authentication as OAuth2AuthenticationToken
        val customAttributes = authorizationRequestEntityService.getCustomAttributes()

        val provider =
            UserIdentityEntity.AuthProvider.entries.find { it.value == authentication.authorizedClientRegistrationId }
                ?: throw IllegalArgumentException("Provider not found : ${authentication.authorizedClientRegistrationId}")
        val oAuth2User = token.principal
        val subject = oAuth2User.getAttribute<String>("sub") ?: throw IllegalArgumentException("subject not found")
        val now = OffsetDateTime.now()

        val authorizationSession =
            AuthorizationSessionEntity(
                provider = provider,
                subject = subject,
                redirectUri = customAttributes.redirectUri,
                state = UUID.fromString(customAttributes.state),
                userProfile =
                    AuthorizationSessionEntity.UserProfile(
                        name = oAuth2User.getAttribute<String>("name"),
                        email = oAuth2User.getAttribute<String>("email"),
                    ),
                issuedAt = now,
                expiresAt = now.plus(codeExpiration),
            )

        return authorizationSessionEntityService.save(authorizationSession)
    }
}
