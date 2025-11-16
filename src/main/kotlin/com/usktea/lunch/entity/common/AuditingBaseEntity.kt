package com.usktea.lunch.entity.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@EntityListeners(AuditingEntityListener::class)
@MappedSuperclass
abstract class AuditingBaseEntity {
    @DirtyCheckIgnore
    @CreatedDate
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
        columnDefinition = "TIMESTAMP WITH TIME ZONE",
    )
    val createdAt: LocalDateTime = LocalDateTime.now()

    @DirtyCheckIgnore
    @LastModifiedDate
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(
        name = "updated_at",
        nullable = false,
        columnDefinition = "TIMESTAMP WITH TIME ZONE",
    )
    val updatedAt: LocalDateTime = LocalDateTime.now()
}
