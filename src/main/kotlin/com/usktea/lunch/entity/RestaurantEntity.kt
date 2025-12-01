package com.usktea.lunch.entity

import com.usktea.lunch.common.Day
import com.usktea.lunch.entity.common.AuditingBaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.locationtech.jts.geom.Point
import java.awt.Menu
import java.time.LocalTime

@Entity
@Table(
    name = "restaurant",
    schema = "lunch",
    indexes = [
        Index(name = "idx_restaurant_main_category", columnList = "mainCategory"),
        Index(name = "idx_restaurant_detail_category", columnList = "detailCategory"),
        Index(name = "idx_restaurant_main_detail_category", columnList = "mainCategory, detailCategory"),
    ],
)
class RestaurantEntity(
    @Id
    val managementNumber: String,
    var name: String,
    var contact: String?,
    val sido: String?,
    val sigungu: String?,
    val dongmyun: String?,
    val ri: String?,
    val road: String?,
    val buildingNumber: String?,
    val address: String?,
    @Column(columnDefinition = "geometry(Point, 4326)")
    val location: Point,
    @Enumerated(EnumType.STRING)
    val status: BusinessStatus,
    @Column(name = "h3_indices", columnDefinition = "text[] DEFAULT '{}'")
    @JdbcTypeCode(SqlTypes.ARRAY)
    val h3Indices: Array<String> = emptyArray(),
    var externalLink: String? = null,
    // 대분류: 한식, 중식, 일식, 양식 등
    var mainCategory: String? = null,
    // 소분류: 실제 메뉴/요리명 (삼겹살, 돈까스, 짜장면 등)
    var detailCategory: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var businessHours: List<BusinessHour> = emptyList(),
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var menus: List<Menu> = emptyList(),
    var summary: String? = null,
    @Embedded
    var priceRange: PriceRange? = null,
) : AuditingBaseEntity() {
    @Embeddable
    data class PriceRange(
        val minimum: Int,
        val maximum: Int,
    )

    data class Menu(
        val name: String,
        val price: Int,
        val isRepresentative: Boolean,
    )

    data class BusinessHour(
        val day: Day,
        val openAt: LocalTime,
        val closeAt: LocalTime,
        val breakTimeStartAt: LocalTime? = null,
        val breakTimeEndAt: LocalTime? = null,
        val isOpen: Boolean,
    )

    enum class BusinessStatus {
        OPEN,
        CLOSED,
        UNKNOWN,
    }
}
