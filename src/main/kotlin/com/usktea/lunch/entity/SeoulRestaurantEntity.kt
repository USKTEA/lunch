package com.usktea.lunch.entity

import com.usktea.lunch.entity.common.AuditingBaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "seoul_restaurant",
    schema = "open_data_cloud",
    indexes = [Index(name = "idx_restaurant_management_number", columnList = "management_number", unique = true)],
)
class SeoulRestaurantEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    val managementNumber: String,
    val openSelfTeamCode: String?,
    val approvalDate: LocalDate?,
    val approvalCancelDate: LocalDate? = null,
    val tradeStateCode: String,
    val tradeStateName: String,
    val detailTradeStateCode: String,
    val detailTradeStateName: String,
    val closeDate: LocalDate? = null,
    val pauseStartDate: LocalDate? = null,
    val pauseEndDate: LocalDate? = null,
    val reopenDate: LocalDate? = null,
    val siteTel: String? = null,
    val siteArea: String? = null,
    val sitePostNo: String,
    val siteWholeAddress: String? = null,
    val roadWholeAddress: String? = null,
    val roadPostNo: String,
    val businessPlaceName: String? = null,
    val lastModifiedTimestamp: LocalDateTime? = null,
    val updateType: String? = null,
    val updateDate: LocalDateTime? = null,
    val businessType: String? = null,
    val xCoordinate: Double? = null,
    val yCoordinate: Double? = null,
    val sanitaryBusinessType: String? = null,
    val maleEmployeeCount: Int? = null,
    val femaleEmployeeCount: Int? = null,
    val tradeSurroundingCategory: String? = null,
    val gradeCategory: String? = null,
    val waterSupplyFacility: String? = null,
    val totalEmployees: Int? = null,
    val headquartersEmployees: Int? = null,
    val factoryOfficeWorkers: Int? = null,
    val factorySalesWorkers: Int? = null,
    val factoryProductionWorkers: Int? = null,
    val buildingOwnershipCategory: String? = null,
    val securityDeposit: Double? = null,
    val monthlyRent: Double? = null,
    val multiUseBusiness: String? = null,
    val totalFacilitySize: Double? = null,
    val traditionalBusinessNumber: String? = null,
    val traditionalMainDish: String? = null,
    val homepage: String? = null,
) : AuditingBaseEntity()
