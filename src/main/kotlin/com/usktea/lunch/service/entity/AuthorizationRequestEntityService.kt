package com.usktea.lunch.service.entity

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.usktea.lunch.common.logger
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.stereotype.Service

@Service
class AuthorizationRequestEntityService(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private val customAttributes = ThreadLocal<CustomAuthorizationRequestAttributes>()

    override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val state = request.getParameter(OAuth2ParameterNames.STATE) ?: return null

        return try {
            jdbcTemplate.queryForObject(SELECT_SQL, { rs, _ ->
                mapToOAuth2AuthorizationRequest(rs)
            }, state)
        } catch (e: EmptyResultDataAccessException) {
            logger.warn("Authorization request not found")
            null
        }
    }

    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response)
            return
        }

        val customRedirectUri = request.getParameter("redirect_uri")
        val customState = request.getParameter("state")

        val modifiedAuthorizationRequest =
            OAuth2AuthorizationRequest
                .from(authorizationRequest)
                .attributes { attrs ->
                    // 기존 attributes 복사
                    attrs.putAll(authorizationRequest.attributes)
                    // frontendRedirectUri 추가
                    attrs["custom_redirect_uri"] = customRedirectUri
                    attrs["custom_state"] = customState
                }
                .build()

        jdbcTemplate.update(
            INSERT_SQL,
            modifiedAuthorizationRequest.state,
            modifiedAuthorizationRequest.authorizationUri,
            modifiedAuthorizationRequest.grantType.value,
            modifiedAuthorizationRequest.responseType.value,
            modifiedAuthorizationRequest.clientId,
            modifiedAuthorizationRequest.redirectUri,
            toJson(modifiedAuthorizationRequest.scopes),
            toJson(modifiedAuthorizationRequest.additionalParameters),
            modifiedAuthorizationRequest.authorizationRequestUri,
            toJson(modifiedAuthorizationRequest.attributes),
        )
    }

    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): OAuth2AuthorizationRequest? {
        val state = request.getParameter(OAuth2ParameterNames.STATE) ?: return null

        try {
            val oAuth2AuthorizationRequest =
                jdbcTemplate.queryForObject(DELETE_SQL, { rs, _ ->
                    mapToOAuth2AuthorizationRequest(rs)
                }, state)

            if (oAuth2AuthorizationRequest != null) {
                val customRedirectUri = oAuth2AuthorizationRequest.attributes["custom_redirect_uri"] as? String
                val customState = oAuth2AuthorizationRequest.attributes["custom_state"] as? String

                if (customRedirectUri != null && customState != null) {
                    customAttributes.set(
                        CustomAuthorizationRequestAttributes(
                            redirectUri = customRedirectUri,
                            state = customState,
                        ),
                    )
                }
            }

            return oAuth2AuthorizationRequest
        } catch (e: EmptyResultDataAccessException) {
            logger.warn("Authorization request not found")
            customAttributes.remove()
            return null
        }
    }

    fun getCustomAttributes(): CustomAuthorizationRequestAttributes {
        val attributes = customAttributes.get()
        customAttributes.remove()
        return attributes
    }

    private fun mapToOAuth2AuthorizationRequest(rs: java.sql.ResultSet): OAuth2AuthorizationRequest {
        return OAuth2AuthorizationRequest.authorizationCode()
            .state(rs.getString("state"))
            .authorizationUri(rs.getString("authorization_uri"))
            .clientId(rs.getString("client_id"))
            .redirectUri(rs.getString("redirect_uri"))
            .scopes(fromJson<Set<String>>(rs.getString("scopes")) ?: emptySet())
            .additionalParameters(fromJson<Map<String, Any>>(rs.getString("additional_parameters")) ?: emptyMap())
            .authorizationRequestUri(rs.getString("authorization_request_uri"))
            .attributes(fromJson<Map<String, Any>>(rs.getString("attributes")) ?: emptyMap())
            .build()
    }

    private fun toJson(obj: Any): String {
        return objectMapper.writeValueAsString(obj)
    }

    private inline fun <reified T> fromJson(json: String?): T? {
        return json?.let { objectMapper.readValue(it, object : TypeReference<T>() {}) }
    }

    data class CustomAuthorizationRequestAttributes(
        val redirectUri: String,
        val state: String,
    )

    companion object {
        private const val SELECT_SQL = """
              SELECT state, authorization_uri, grant_type, response_type, client_id, 
                     redirect_uri, scopes, additional_parameters, authorization_request_uri, attributes
              FROM lunch.oauth2_authorization_request
              WHERE state = ?
          """

        private const val INSERT_SQL = """
              INSERT INTO lunch.oauth2_authorization_request (
                  state, authorization_uri, grant_type, response_type, client_id, redirect_uri,
                  scopes, additional_parameters, authorization_request_uri, attributes, created_at
              ) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?::jsonb, NOW())
              ON CONFLICT (state) DO NOTHING
          """

        private const val DELETE_SQL = """
              DELETE FROM lunch.oauth2_authorization_request 
              WHERE state = ?
              RETURNING state, authorization_uri, grant_type, response_type, client_id, 
                        redirect_uri, scopes, additional_parameters, authorization_request_uri, attributes
          """
    }
}
