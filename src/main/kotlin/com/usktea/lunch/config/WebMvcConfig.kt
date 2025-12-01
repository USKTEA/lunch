package com.usktea.lunch.config

import com.usktea.lunch.config.web.CorsProperties
import com.usktea.lunch.config.web.TokenRequestResolver
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

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
            .resourceChain(true)
            .addResolver(
                object : PathResourceResolver() {
                    override fun getResource(
                        resourcePath: String,
                        location: Resource,
                    ): Resource? {
                        val requestedResource = location.createRelative(resourcePath)

                        // 실제 파일이 있으면 반환 (js, css, images 등)
                        return if (requestedResource.exists() && requestedResource.isReadable) {
                            requestedResource
                        } else {
                            // 없으면 index.html 반환 (React Router가 처리)
                            ClassPathResource("static/index.html")
                        }
                    }
                },
            )
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(tokenRequestResolver)
    }
}
