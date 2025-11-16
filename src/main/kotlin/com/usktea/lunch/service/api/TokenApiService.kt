package com.usktea.lunch.service.api

import com.usktea.lunch.common.TokenVo
import com.usktea.lunch.common.logger
import com.usktea.lunch.controller.vo.IssueTokenRequest
import com.usktea.lunch.controller.vo.IssueTokenResponse
import com.usktea.lunch.entity.AuthorizationSessionEntity
import com.usktea.lunch.entity.TokenEntity
import com.usktea.lunch.entity.UserEntity
import com.usktea.lunch.service.entity.AuthorizationSessionEntityService
import com.usktea.lunch.service.entity.TokenEntityService
import com.usktea.lunch.service.entity.UserEntityService
import com.usktea.lunch.util.CloudfrontSingedCookieUtil
import com.usktea.lunch.util.JwtUtil
import com.usktea.lunch.util.NicknameGenerator
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class TokenApiService(
    @Value("\${aws.cloudfront.domain}")
    private val cloudfrontDomain: String,
    private val jwtUtil: JwtUtil,
    private val cloudfrontSingedCookieUtil: CloudfrontSingedCookieUtil,
    private val authorizationSessionEntityService: AuthorizationSessionEntityService,
    private val userEntityService: UserEntityService,
    private val tokenEntityService: TokenEntityService,
) {
    @Transactional
    fun issueToken(
        issueTokenRequest: IssueTokenRequest,
        httpServletResponse: HttpServletResponse,
    ): IssueTokenResponse {
        val issueTokenResponse =
            when (issueTokenRequest) {
                is IssueTokenRequest.AuthorizationCode -> issueToken(issueTokenRequest)
                is IssueTokenRequest.RefreshToken -> refreshToken(issueTokenRequest)
            }

        val refreshTokenCookie =
            ResponseCookie.from("refresh_token", issueTokenResponse.refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/auth/tokens")
                .maxAge(issueTokenResponse.refreshTokenExpiresIn.seconds)
                .build()

        httpServletResponse.addHeader("Set-Cookie", refreshTokenCookie.toString())

        issueTokenResponse.cloudfrontSignedCookies.forEach { (name, value) ->
            val cookie =
                ResponseCookie.from(name, value)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Lax")
                    .domain(".$cloudfrontDomain")
                    .path("/")
                    .build()

            httpServletResponse.addHeader("Set-Cookie", cookie.toString())
        }

        return issueTokenResponse
    }

    private fun refreshToken(issueTokenRequest: IssueTokenRequest.RefreshToken): IssueTokenResponse {
        logger.debug("Refresh token request received")

        val token = tokenEntityService.findNotExpiredTokenByRefreshToken(issueTokenRequest.refreshToken)

        if (token == null) {
            logger.warn("Refresh token not found or expired")
            throw IllegalArgumentException("Refresh token not found or expired")
        }

        val updated = tokenEntityService.useToken(token.id) > 0

        if (!updated) {
            logger.error(
                "Refresh token reuse detected for familyId: ${token.familyId}, userId: ${token.userId}. Revoking all tokens in family.",
            )
            tokenEntityService.revokeFamily(token.familyId)

            throw IllegalStateException("Refresh token reuse detected. All tokens in family have been revoked.")
        }

        val newToken = refreshToken(token)
        logger.info(
            "Refresh token successfully rotated for userId: ${token.userId}, generation: ${token.generation} -> ${token.generation + 1}",
        )

        val cloudfrontCookie = cloudfrontSingedCookieUtil.generateSignedCookie(OffsetDateTime.now())

        return IssueTokenResponse(
            accessToken = newToken.accessToken,
            issuedAt = newToken.issuedAt,
            tokenType = newToken.tokenType,
            expiresIn = newToken.expiresIn,
            expiresAt = newToken.expiresAt,
            refreshToken = newToken.refreshToken,
            refreshTokenExpiresIn = newToken.refreshTokenExpiresIn,
            cloudfrontSignedCookies = cloudfrontCookie,
        )
    }

    private fun refreshToken(usedToken: TokenEntity): TokenVo {
        val newToken = jwtUtil.generate(usedToken.userId)
        val issuedAt = newToken.issuedAt.atOffset(ZoneOffset.UTC)

        val tokenEntity =
            TokenEntity(
                familyId = usedToken.familyId,
                userId = usedToken.userId,
                refreshToken = newToken.refreshToken,
                issuedAt = issuedAt,
                expiresAt = issuedAt.plus(newToken.refreshTokenExpiresIn),
                generation = usedToken.generation + 1,
            )

        tokenEntityService.save(tokenEntity)

        return newToken
    }

    private fun issueToken(issueTokenRequest: IssueTokenRequest.AuthorizationCode): IssueTokenResponse {
        logger.debug("Authorization code token request received, code: ${issueTokenRequest.code}")

        val authorizationSession = authorizationSessionEntityService.findByCode(UUID.fromString(issueTokenRequest.code))

        if (authorizationSession == null) {
            logger.warn("Authorization session not found for code: ${issueTokenRequest.code}")
            throw IllegalArgumentException("Issue Token Request not found")
        }

        if (authorizationSession.state != issueTokenRequest.state || authorizationSession.redirectUri != issueTokenRequest.redirectUri) {
            logger.warn("Invalid state or redirect URI for code: ${issueTokenRequest.code}")
            throw IllegalArgumentException("Issue Token Request not found")
        }

        val now = OffsetDateTime.now()

        if (now.isAfter(authorizationSession.expiresAt.plusMinutes(1))) {
            logger.warn("Expired authorization session for code: ${issueTokenRequest.code}")
            throw IllegalArgumentException("Issue Token Request not found")
        }

        val user = getOrCreateUser(authorizationSession)
        val token = createToken(user)
        val cloudfrontCookie = cloudfrontSingedCookieUtil.generateSignedCookie(now)

        authorizationSession.used(now)

        logger.info("Initial token issued for userId: ${user.id}, provider: ${authorizationSession.provider}")

        return IssueTokenResponse(
            accessToken = token.accessToken,
            issuedAt = token.issuedAt,
            tokenType = token.tokenType,
            expiresIn = token.expiresIn,
            expiresAt = token.expiresAt,
            refreshToken = token.refreshToken,
            refreshTokenExpiresIn = token.refreshTokenExpiresIn,
            cloudfrontSignedCookies = cloudfrontCookie,
        )
    }

    private fun createToken(user: UserEntity): TokenVo {
        val token = jwtUtil.generate(user.id)
        val issuedAt = token.issuedAt.atOffset(ZoneOffset.UTC)

        val tokenEntity =
            TokenEntity(
                familyId = UUID.randomUUID(),
                userId = user.id,
                refreshToken = token.refreshToken,
                issuedAt = issuedAt,
                expiresAt = issuedAt.plus(token.refreshTokenExpiresIn),
                generation = 0,
            )

        tokenEntityService.save(tokenEntity)

        return token
    }

    private fun getOrCreateUser(authorizationSession: AuthorizationSessionEntity): UserEntity {
        val provider = authorizationSession.provider
        val subject = authorizationSession.subject

        val existsUser = userEntityService.findByExternalIdentityProviderAndSubject(provider, subject)

        if (existsUser != null) {
            existsUser.loginIn(OffsetDateTime.now())
            return existsUser
        }

        val newUser =
            UserEntity(
                email = authorizationSession.userProfile.email,
                nickname = NicknameGenerator.fromUuid(UUID.randomUUID()),
            )

        newUser.linkExternalProvider(provider, subject)

        return userEntityService.createUser(newUser)
    }
}
