package com.usktea.lunch.service.auth

import com.usktea.lunch.controller.vo.GetUserResponse
import com.usktea.lunch.service.entity.UserEntityService
import org.springframework.stereotype.Service

@Service
class UserApiService(
    private val userEntityService: UserEntityService,
) {
    fun getUser(userId: Long): GetUserResponse {
        val user = userEntityService.findById(userId) ?: throw IllegalArgumentException("user not found")

        return GetUserResponse(
            id = user.id,
            nickname = user.nickname,
        )
    }
}
