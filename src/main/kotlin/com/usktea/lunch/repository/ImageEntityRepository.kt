package com.usktea.lunch.repository

import com.usktea.lunch.entity.ImageEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ImageEntityRepository : JpaRepository<ImageEntity, UUID> {
    fun findAllByNameIn(imageNames: List<UUID>): List<ImageEntity>
}
