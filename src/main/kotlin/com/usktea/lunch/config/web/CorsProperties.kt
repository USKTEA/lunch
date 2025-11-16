package com.usktea.lunch.config.web

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.cors")
class CorsProperties(
    val allowedOrigins: List<String> = emptyList(),
    val allowedMethods: List<String> = emptyList(),
    val allowedHeaders: List<String> = emptyList(),
    val allowCredentials: Boolean = false,
    val maxAge: Long = 3600,
)
