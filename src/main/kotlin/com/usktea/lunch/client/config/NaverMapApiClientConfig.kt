package com.usktea.lunch.client.config

import feign.Logger
import feign.RequestInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean

class NaverMapApiClientConfig(
    @Value("\${custom.naver.client-id}")
    private val clientId: String,
    @Value("\${custom.naver.client-secret}")
    private val clientSecret: String,
) {
    @Bean
    fun requestHeaderInterceptor(): RequestInterceptor {
        return RequestInterceptor { template ->
            template.header("x-ncp-apigw-api-key-id", clientId)
            template.header("x-ncp-apigw-api-key", clientSecret)
            template.header("Accept", "application/json")
        }
    }

    @Bean
    fun feignLoggerLevel(): Logger.Level {
        return Logger.Level.FULL
    }
}
