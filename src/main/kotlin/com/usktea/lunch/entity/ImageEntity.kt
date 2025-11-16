package com.usktea.lunch.entity

import com.usktea.lunch.common.ImageContext
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.net.URL
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "image", schema = "lunch", indexes = [Index(columnList = "image_number", unique = true)])
class ImageEntity(
    val name: UUID,
    val userId: Long,
    @Enumerated(EnumType.STRING)
    val context: ImageContext,
    val objectKey: String,
    val url: URL,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    var attachedSource: String? = null,
    var attachedAt: OffsetDateTime? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    fun attached(id: Long) {
        this.attachedSource = id.toString()
        this.attachedAt = OffsetDateTime.now()
    }
}
