package com.usktea.lunch.config.auth

import com.usktea.lunch.service.auth.AuthorizationSessionAuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2LoginSuccessHandler(
    @Value("\${custom.jwt.token-end-point}")
    private val tokenEntPoint: String,
    private val authorizationSessionAuthService: AuthorizationSessionAuthService,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val authorizationSession = authorizationSessionAuthService.createSession(authentication)

        response.sendRedirect("$tokenEntPoint?code=${authorizationSession.code}")
    }
}
