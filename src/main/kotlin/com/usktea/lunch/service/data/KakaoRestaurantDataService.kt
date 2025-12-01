package com.usktea.lunch.service.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.usktea.lunch.common.Day
import com.usktea.lunch.common.logger
import com.usktea.lunch.entity.ReviewEntity
import com.usktea.lunch.service.entity.RestaurantEntityService
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.math.roundToInt

@Service
class KakaoRestaurantDataService(
    private val objectMapper: ObjectMapper,
    private val restaurantEntityService: RestaurantEntityService,
    private val restaurantRepository: com.usktea.lunch.repository.RestaurantRepository,
    private val reviewRepository: com.usktea.lunch.repository.ReviewRepository,
) {
    fun process() {
        val matcher =
            FileSystems.getDefault()
                .getPathMatcher("glob:kakao-scraping-results-worker-*.json")

        Files.list(Paths.get("."))
            .filter { matcher.matches(it.fileName) }
            .forEach { path ->
                processWorkerFile(path.toFile())
            }
    }

    private fun processWorkerFile(file: File) {
        logger.error("시작")
        val kakaoRestaurantDataList: List<KakaoRestaurantData> = objectMapper.readValue(file)

        // 음식점이 아닌 카테고리 필터링
        val filteredData =
            kakaoRestaurantDataList.filter { data ->
                data.category?.let { category ->
                    val categories =
                        category.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toSet()
                    CategoryMappings.hasRestaurantCategory(categories)
                } ?: true // 카테고리가 없으면 일단 포함
            }

        logger.info("전체 데이터: ${kakaoRestaurantDataList.size}개, 음식점 데이터: ${filteredData.size}개")

        val restaurantEntities =
            restaurantEntityService.findAllByManagementNumbers(filteredData.mapTo(mutableSetOf()) { it.restaurantManagementNumber })
                .associateBy { it.managementNumber }

        val restaurants =
            filteredData.mapNotNull { data ->
                val restaurantEntity = restaurantEntities[data.restaurantManagementNumber] ?: return@mapNotNull null

                updateRestaurantEntity(restaurantEntity, data)
                restaurantEntity
            }

        val reviews =
            kakaoRestaurantDataList.mapNotNull { data ->
                if (data.reviews == null || data.reviews.isEmpty()) {
                    return@mapNotNull null
                }

                data.reviews.mapNotNull { review ->
                    val localDate = LocalDate.parse(review.date)
                    val createdAt = localDate.atStartOfDay(java.time.ZoneId.of("Asia/Seoul")).toOffsetDateTime()

                    ReviewEntity(
                        restaurantManagementNumber = data.restaurantManagementNumber,
                        reviewerId = -1L,
                        rating = review.rating?.roundToInt() ?: return@mapNotNull null,
                        content = review.content,
                        createdAt = createdAt,
                    )
                }
            }.flatten()

        restaurantRepository.saveAll(restaurants)
        reviewRepository.saveAll(reviews)
    }

    private fun updateRestaurantEntity(
        entity: com.usktea.lunch.entity.RestaurantEntity,
        data: KakaoRestaurantData,
    ) {
        // name: "장소명" 제거
        data.name?.let { name ->
            entity.name = name.replace("장소명", "").trim()
        }

        entity.externalLink = data.url

        data.category?.let { category ->
            val categories =
                category.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()

            // 카테고리 매핑 적용
            val mapping = CategoryMappings.findFirstRestaurantMapping(categories)
            if (mapping != null) {
                entity.mainCategory = mapping.mainCategory
                entity.detailCategory = mapping.detailCategory
            } else {
                // 매핑되지 않은 카테고리 로그
                logger.warn("매핑되지 않은 카테고리: $categories (레스토랑: ${entity.name})")
            }
        }

        // summary
        entity.summary = data.summary

        // contact: phone
        entity.contact = data.phone

        // businessHours
        data.businessHours?.let { hours ->
            entity.businessHours = hours.mapNotNull { convertToBusinessHour(it) }
        }

        // menus
        data.menus?.let { menus ->
            entity.menus =
                menus.map { menu ->
                    com.usktea.lunch.entity.RestaurantEntity.Menu(
                        name = menu.name,
                        price = menu.price,
                        isRepresentative = menu.isRepresentative,
                    )
                }

            // priceRange: 메뉴의 최소/최대 가격
            if (menus.isNotEmpty()) {
                val prices = menus.map { it.price }
                entity.priceRange =
                    com.usktea.lunch.entity.RestaurantEntity.PriceRange(
                        minimum = prices.minOrNull() ?: 0,
                        maximum = prices.maxOrNull() ?: 0,
                    )
            }
        }
    }

    private fun convertToBusinessHour(hour: BusinessHour): com.usktea.lunch.entity.RestaurantEntity.BusinessHour? {
        if (!hour.isOpen || hour.openAt == null || hour.closeAt == null) {
            // 휴무일이거나 시간 정보가 없으면 skip
            return null
        }

        return try {
            com.usktea.lunch.entity.RestaurantEntity.BusinessHour(
                day = convertDay(hour.day),
                openAt = java.time.LocalTime.parse(hour.openAt),
                closeAt = java.time.LocalTime.parse(hour.closeAt),
                breakTimeStartAt = hour.breakTimeStart?.let { java.time.LocalTime.parse(it) },
                breakTimeEndAt = hour.breakTimeEnd?.let { java.time.LocalTime.parse(it) },
                isOpen = hour.isOpen,
            )
        } catch (e: Exception) {
            println("⚠️  BusinessHour 변환 실패: ${hour.day} ${hour.openAt}-${hour.closeAt}, error: ${e.message}")
            null
        }
    }

    private fun convertDay(day: String): Day {
        return when (day) {
            "SUN" -> Day.SUN
            "MON" -> Day.MON
            "TUE" -> Day.TUE
            "WED" -> Day.WED
            "THU" -> Day.THU
            "FRI" -> Day.FRI
            "SAT" -> Day.SAT
            else -> throw IllegalArgumentException("Unknown day: $day")
        }
    }

    data class KakaoRestaurantData(
        val restaurantManagementNumber: String,
        val url: String,
        val name: String?,
        val category: String?,
        val summary: String?,
        val phone: String?,
        val businessHours: List<BusinessHour>?,
        val menus: List<Menu>?,
        val reviews: List<Review>?,
    )

    data class BusinessHour(
        val day: String,
        val openAt: String?,
        val closeAt: String?,
        val breakTimeStart: String?,
        val breakTimeEnd: String?,
        val isOpen: Boolean,
    )

    data class Menu(
        val name: String,
        val price: Int,
        val isRepresentative: Boolean,
    )

    data class Review(
        val author: String,
        // 임시로 String으로 변경
        val date: String,
        val rating: Double?,
        val content: String,
    )
}
