package com.usktea.lunch.controller.vo

import com.usktea.lunch.common.AuthorizationGrantType
import java.util.UUID

sealed interface IssueTokenRequest {
    val grantType: AuthorizationGrantType

    data class AuthorizationCode(
        val code: String,
        val redirectUri: String,
        val state: UUID,
        override val grantType: AuthorizationGrantType = AuthorizationGrantType.AUTHORIZATION_CODE,
    ) : IssueTokenRequest

    data class RefreshToken(
        val refreshToken: String,
        override val grantType: AuthorizationGrantType = AuthorizationGrantType.REFRESH_TOKEN,
    ) : IssueTokenRequest
}
