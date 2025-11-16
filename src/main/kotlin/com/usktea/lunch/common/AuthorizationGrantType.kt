package com.usktea.lunch.common

enum class AuthorizationGrantType(
    val value: String,
) {
    AUTHORIZATION_CODE("authorization_code"),
    REFRESH_TOKEN("refresh_token"),
    CLIENT_CREDENTIALS("client_credentials"),
    PASSWORD("password"), ;

    companion object {
        fun from(value: String): AuthorizationGrantType {
            return AuthorizationGrantType.entries.first { it.value == value }
        }
    }
}
