package com.usktea.lunch.config.auth

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class CustomAuthenticationToken(
    val userId: Long,
    authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {
    init {
        super.setAuthenticated(true)
    }

    override fun getCredentials(): Any? {
        return null
    }

    override fun getPrincipal(): Any {
        return userId
    }
}
