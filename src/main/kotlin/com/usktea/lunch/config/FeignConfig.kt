package com.usktea.lunch.config

import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Configuration

@EnableFeignClients(
    basePackages = ["com.usktea.lunch"],
)
@Configuration
class FeignConfig
