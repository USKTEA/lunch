package com.usktea.lunch.entity

import com.usktea.lunch.common.AuthorizationGrantType
import com.usktea.lunch.common.AuthorizationResponseType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "oauth2_authorization_request", schema = "lunch")
class CustomOAuth2AuthorizationRequestEntity(
    @Id
    val state: String,
    val authorizationUri: String,
    @Enumerated(EnumType.STRING)
    val grantType: AuthorizationGrantType,
    @Enumerated(EnumType.STRING)
    val responseType: AuthorizationResponseType,
    val clientId: String,
    val redirectUri: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val scopes: Set<String>,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val additionalParameters: Map<String, Any> = emptyMap(),
    val authorizationRequestUri: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val attributes: Map<String, Any> = emptyMap(),
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
