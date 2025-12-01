package com.usktea.lunch.controller

import com.usktea.lunch.config.auth.CustomAuthenticationToken
import com.usktea.lunch.controller.vo.GetUserResponse
import com.usktea.lunch.service.auth.UserApiService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userApiService: UserApiService,
) {
    @GetMapping("/api/users/me")
    fun getUser(authentication: CustomAuthenticationToken): GetUserResponse {
        return userApiService.getUser(userId = authentication.userId)
    }
}
