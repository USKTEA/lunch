package com.usktea.lunch.config

import com.usktea.lunch.config.web.CorsProperties
import com.usktea.lunch.config.web.TokenRequestResolver
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class WebMvcConfig(
    private val corsProperties: CorsProperties,
    private val tokenRequestResolver: TokenRequestResolver,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(*corsProperties.allowedOrigins.toTypedArray())
            .allowedMethods(*corsProperties.allowedMethods.toTypedArray())
            .allowedHeaders(*corsProperties.allowedHeaders.toTypedArray())
            .allowCredentials(corsProperties.allowCredentials)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/web/**")
            .addResourceLocations("classpath:/static/")
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(tokenRequestResolver)
    }
}
