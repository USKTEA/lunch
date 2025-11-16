package com.usktea.lunch.entity

import com.usktea.lunch.entity.common.AuditingBaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.locationtech.jts.geom.Point

@Entity
@Table(name = "restaurant", schema = "lunch")
class RestaurantEntity(
    @Id
    val managementNumber: String,
    val name: String,
    val contact: String?,
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
) : AuditingBaseEntity() {
    enum class BusinessStatus {
        OPEN,
        CLOSED,
        UNKNOWN,
    }
}
