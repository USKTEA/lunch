package com.usktea.lunch.config.web

import com.usktea.lunch.common.AuthorizationGrantType
import com.usktea.lunch.controller.vo.IssueTokenRequest
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.UUID

@Component
class TokenRequestResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == IssueTokenRequest::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any {
        val request = webRequest.nativeRequest as HttpServletRequest
        val grantType = AuthorizationGrantType.from(request.getParam("grant_type"))

        return when (grantType) {
            AuthorizationGrantType.AUTHORIZATION_CODE ->
                IssueTokenRequest.AuthorizationCode(
                    code = request.getParam("code"),
                    state = UUID.fromString(request.getParam("state")),
                    redirectUri = request.getParam("redirect_uri"),
                )

            AuthorizationGrantType.REFRESH_TOKEN ->
                IssueTokenRequest.RefreshToken(
                    refreshToken =
                        request.cookies.find { it.name == "refresh_token" }?.value
                            ?: throw IllegalArgumentException("refresh token is required"),
                )

            AuthorizationGrantType.CLIENT_CREDENTIALS -> TODO()
            AuthorizationGrantType.PASSWORD -> TODO()
        }
    }

    fun HttpServletRequest.getParam(name: String): String {
        return this.getParameter(name) ?: throw IllegalArgumentException("$name is required")
    }
}
