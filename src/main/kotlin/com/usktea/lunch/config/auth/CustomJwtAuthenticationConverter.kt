package com.usktea.lunch.config.auth

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class CustomJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val userId = jwt.subject.toLong()
        val authorities =
            jwt.getClaimAsStringList("roles")
                ?.map { SimpleGrantedAuthority("ROLE_$it") }
                ?: emptyList()

        return CustomAuthenticationToken(userId, authorities)
    }
}
