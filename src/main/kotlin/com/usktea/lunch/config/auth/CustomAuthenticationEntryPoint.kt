package com.usktea.lunch.config.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.usktea.lunch.config.web.CorsProperties
import com.usktea.lunch.entity.UserIdentityEntity
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
    private val corsProperties: CorsProperties,
    private val clientRegistrations: Iterable<ClientRegistration>,
) : AuthenticationEntryPoint {
    private val allowedMethodsString = corsProperties.allowedMethods.joinToString(", ")
    private val allowedCredentialString = corsProperties.allowCredentials.toString()
    private val allowedHeaders = corsProperties.allowedHeaders.joinToString(", ")
    private val maxAgeString: String = corsProperties.maxAge.toString()

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        addCorsHeaders(request, response)
        addResponse(response)
    }

    private fun addResponse(response: HttpServletResponse) {
        val providers =
            clientRegistrations.map {
                OAuth2ProviderVo(
                    provider = UserIdentityEntity.AuthProvider.valueOf(it.registrationId.uppercase()),
                    authorizationUri = "/oauth2/authorization/${it.registrationId}",
                )
            }

        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.characterEncoding = Charsets.UTF_8.name()

        val errorResponse =
            buildMap {
                put("error", HttpStatus.UNAUTHORIZED.name)
                put("message", "Authentication required")
                put("providers", providers)
            }

        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }

    private fun addCorsHeaders(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val origin = request.getHeader("Origin")

        if (origin in corsProperties.allowedOrigins) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, allowedCredentialString)
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, allowedMethodsString)
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders)
            response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, maxAgeString)
        }
    }

    data class OAuth2ProviderVo(
        val provider: UserIdentityEntity.AuthProvider,
        val authorizationUri: String,
    )
}
