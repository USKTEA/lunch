package com.usktea.lunch.config

import com.usktea.lunch.config.auth.CustomAuthenticationEntryPoint
import com.usktea.lunch.config.auth.CustomJwtAuthenticationConverter
import com.usktea.lunch.config.auth.OAuth2LoginSuccessHandler
import com.usktea.lunch.service.entity.AuthorizationRequestEntityService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val authorizationRequestEntityService: AuthorizationRequestEntityService,
    private val oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
    private val customJwtAuthenticationConverter: CustomJwtAuthenticationConverter,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
) {
    @Bean
    @Order(1)
    fun permitAllSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/web/**", "/oauth2/**", "/login/**", "/check", "/api/auth/**")
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
            .oauth2Login { oAuth2 ->
                oAuth2.authorizationEndpoint { endPoint ->
                    endPoint.authorizationRequestRepository(authorizationRequestEntityService)
                }.successHandler(oAuth2LoginSuccessHandler)
            }
            .csrf { csrf -> csrf.disable() }
            .cors { }

        return http.build()
    }

    @Bean
    @Order(2)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatchers { matcher ->
                matcher.requestMatchers("/api/**")
            }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oAuth2 ->
                oAuth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)
                }
            }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(customAuthenticationEntryPoint)
            }
            .sessionManagement { sessionManager ->
                sessionManager.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .csrf { csrf -> csrf.disable() }
            .cors { }

        return http.build()
    }
}
