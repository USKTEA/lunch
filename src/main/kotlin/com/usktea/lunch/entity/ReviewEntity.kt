package com.usktea.lunch.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.net.URL
import java.time.OffsetDateTime

@Entity
@Table(
    name = "review",
    schema = "lunch",
)
class ReviewEntity(
    val restaurantManagementNumber: String,
    val reviewerId: Long,
    val rating: Int,
    val content: String,
    @Type(JsonType::class)
    @Column(columnDefinition = "jsonb")
    val imageUrls: List<URL> = emptyList(),
    @Enumerated(EnumType.STRING)
    val status: ReviewStatus = ReviewStatus.CREATED,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    enum class ReviewStatus {
        CREATED,
        DELETED,
    }
}
