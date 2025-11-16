package com.usktea.lunch.service.entity

import com.usktea.lunch.entity.ImageEntity
import com.usktea.lunch.repository.ImageEntityRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ImageEntityService(
    private val imageEntityRepository: ImageEntityRepository,
) {
    fun saveAll(imageEntities: List<ImageEntity>) {
        imageEntityRepository.saveAll(imageEntities)
    }

    fun findAllByNames(imageNames: List<UUID>): List<ImageEntity> {
        return imageEntityRepository.findAllByNameIn(imageNames)
    }
}
