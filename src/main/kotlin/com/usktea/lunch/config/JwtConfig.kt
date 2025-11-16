package com.usktea.lunch.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Configuration
class JwtConfig(
    @Value("\${custom.jwt.private-key}")
    private val base64privateKey: String,
    @Value("\${custom.jwt.public-key}")
    private val base64publicKey: String,
) {
    private val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey =
            keyFactory.generatePublic(
                X509EncodedKeySpec(
                    Base64.getDecoder().decode(base64publicKey.replace(whitespaceRegex, "")),
                ),
            ) as RSAPublicKey

        return NimbusJwtDecoder.withPublicKey(publicKey).build()
    }

    @Bean
    fun jwtEncoder(): JwtEncoder {
        val privateKey =
            keyFactory.generatePrivate(
                Base64.getDecoder().decode(base64privateKey.replace(whitespaceRegex, "")).let { PKCS8EncodedKeySpec(it) },
            )
        val publicKey =
            keyFactory.generatePublic(
                Base64.getDecoder().decode(base64publicKey.replace(whitespaceRegex, "")).let { X509EncodedKeySpec(it) },
            )

        val rsaJWK =
            RSAKey.Builder(publicKey as RSAPublicKey)
                .privateKey(privateKey as RSAPrivateKey)
                .build()

        return NimbusJwtEncoder(ImmutableJWKSet(JWKSet(rsaJWK)))
    }

    companion object {
        private val whitespaceRegex = "\\s+".toRegex()
    }
}
