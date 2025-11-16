package com.usktea.lunch.controller

import com.usktea.lunch.controller.vo.IssueTokenRequest
import com.usktea.lunch.controller.vo.IssueTokenResponse
import com.usktea.lunch.service.api.TokenApiService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TokenController(
    private val tokenApiService: TokenApiService,
) {
    @PostMapping("/api/auth/tokens", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun issueToken(
        issueTokenRequest: IssueTokenRequest,
        response: HttpServletResponse,
    ): IssueTokenResponse {
        return tokenApiService.issueToken(issueTokenRequest, response)
    }
}
