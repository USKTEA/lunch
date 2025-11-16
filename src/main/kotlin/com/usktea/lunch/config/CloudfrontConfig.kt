package com.usktea.lunch.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities

@Configuration
class CloudfrontConfig {
    @Bean
    fun cloudfrontUtil(): CloudFrontUtilities {
        return CloudFrontUtilities.create()
    }
}
