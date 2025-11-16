package com.usktea.lunch.service.crawler

import com.usktea.lunch.client.SeoulOpenDataClient
import com.usktea.lunch.client.vo.BusinessInfoVo
import com.usktea.lunch.common.logger
import com.usktea.lunch.entity.SeoulRestaurantEntity
import com.usktea.lunch.service.entity.SeoulRestaurantEntityService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import kotlin.math.min
import kotlin.sequences.forEach

/**
 * 서울시 일반음식점 인허가 정보 [링크](https://data.seoul.go.kr/dataList/OA-16094/S/1/datasetView.do)
 */
@Service
class SeoulRestaurantEntityCrawlerService(
    @Value("\${custom.restaurant.app-key}")
    private val appKey: String,
    private val seoulOpenDataClient: SeoulOpenDataClient,
    private val seoulRestaurantEntityService: SeoulRestaurantEntityService,
) {
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "downloadNewRestaurants", lockAtMostFor = "PT10M")
    fun downloadNewRestaurants() {
        val lastDownloadedSeoulRestaurant = seoulRestaurantEntityService.getSeoulRestaurantId()

        if (lastDownloadedSeoulRestaurant == null) {
            logger.warn("Seoul restaurant not found during download new restaurants.")

            return
        }

        getRequestSequence(startIndex = lastDownloadedSeoulRestaurant.id + 1)
            .mapNotNull { request ->
                val (response) =
                    seoulOpenDataClient.getData(
                        appKey = appKey,
                        responseType = request.responseType,
                        serviceName = request.serviceName,
                        startIndex = request.startIndex,
                        endIndex = request.endIndex,
                    )

                if (response == null) {
                    return@mapNotNull null
                }

                response.rows.map { vo ->
                    toEntity(vo)
                }
            }.forEach { restaurantEntities ->
                seoulRestaurantEntityService.batchUpsertRestaurants(restaurantEntities)
            }
    }

    fun downloadAll() {
        getRequestSequence()
            .mapNotNull { request ->
                val (response) =
                    seoulOpenDataClient.getData(
                        appKey = appKey,
                        responseType = request.responseType,
                        serviceName = request.serviceName,
                        startIndex = request.startIndex,
                        endIndex = request.endIndex,
                    )

                if (response == null) {
                    return@mapNotNull null
                }

                response.rows.map { vo ->
                    toEntity(vo)
                }
            }.forEach { restaurantEntities ->
                seoulRestaurantEntityService.batchUpsertRestaurants(restaurantEntities)
            }
    }

    private fun toEntity(businessInfoVo: BusinessInfoVo): SeoulRestaurantEntity {
        return SeoulRestaurantEntity(
            managementNumber = businessInfoVo.managementNumber,
            openSelfTeamCode = businessInfoVo.openSelfTeamCode.toNullIfBlank(),
            approvalDate = businessInfoVo.approvalDate,
            approvalCancelDate = businessInfoVo.approvalCancelDate,
            tradeStateCode = businessInfoVo.tradeStateCode,
            tradeStateName = businessInfoVo.tradeStateName,
            detailTradeStateCode = businessInfoVo.detailTradeStateCode,
            detailTradeStateName = businessInfoVo.detailTradeStateName,
            closeDate = businessInfoVo.closeDate,
            pauseStartDate = businessInfoVo.pauseStartDate,
            pauseEndDate = businessInfoVo.pauseEndDate,
            reopenDate = businessInfoVo.reopenDate,
            siteTel = businessInfoVo.siteTel?.formatTelephoneNumber()?.toNullIfBlank(),
            siteArea = businessInfoVo.siteArea?.toNullIfBlank(),
            sitePostNo = businessInfoVo.sitePostNo,
            siteWholeAddress = businessInfoVo.siteWholeAddress.toNullIfBlank(),
            roadWholeAddress = businessInfoVo.roadWholeAddress.toNullIfBlank(),
            roadPostNo = businessInfoVo.roadPostNo,
            businessPlaceName = businessInfoVo.businessPlaceName?.toNullIfBlank(),
            lastModifiedTimestamp = businessInfoVo.lastModifiedTimestamp,
            updateType = businessInfoVo.updateType?.toNullIfBlank(),
            updateDate = businessInfoVo.updateDate,
            businessType = businessInfoVo.businessType?.toNullIfBlank(),
            xCoordinate = businessInfoVo.xCoordinate,
            yCoordinate = businessInfoVo.yCoordinate,
            sanitaryBusinessType = businessInfoVo.sanitaryBusinessType?.toNullIfBlank(),
            maleEmployeeCount = businessInfoVo.maleEmployeeCount,
            femaleEmployeeCount = businessInfoVo.femaleEmployeeCount,
            tradeSurroundingCategory = businessInfoVo.tradeSurroundingCategory?.toNullIfBlank(),
            gradeCategory = businessInfoVo.gradeCategory?.toNullIfBlank(),
            waterSupplyFacility = businessInfoVo.waterSupplyFacility?.toNullIfBlank(),
            totalEmployees = businessInfoVo.totalEmployees,
            headquartersEmployees = businessInfoVo.headquartersEmployees,
            factoryOfficeWorkers = businessInfoVo.factoryOfficeWorkers,
            factorySalesWorkers = businessInfoVo.factorySalesWorkers,
            factoryProductionWorkers = businessInfoVo.factoryProductionWorkers,
            buildingOwnershipCategory = businessInfoVo.buildingOwnershipCategory?.toNullIfBlank(),
            securityDeposit = businessInfoVo.securityDeposit,
            monthlyRent = businessInfoVo.monthlyRent,
            multiUseBusiness = businessInfoVo.multiUseBusiness?.toNullIfBlank(),
            totalFacilitySize = businessInfoVo.totalFacilitySize,
            traditionalBusinessNumber = businessInfoVo.traditionalBusinessNumber?.toNullIfBlank(),
            traditionalMainDish = businessInfoVo.traditionalMainDish?.toNullIfBlank(),
            homepage = businessInfoVo.homepage?.toNullIfBlank(),
        )
    }

    private fun getRequestSequence(startIndex: Long = 0L): Sequence<SeoulOpenDataRequest> {
        val (response) =
            SeoulOpenDataRequest.init(
                responseType = "json",
                serviceName = SEOUL_RESTAURANT_OPEN_DATA_SERVICE_NAME,
                startIndex = startIndex.toInt(),
                endIndex = startIndex.toInt(),
            ).run {
                seoulOpenDataClient.getData(
                    appKey = appKey,
                    responseType = this.responseType,
                    serviceName = this.serviceName,
                    startIndex = this.startIndex,
                    endIndex = this.endIndex,
                )
            }

        if (response == null) {
            return emptySequence()
        }

        return generateSequence(
            SeoulOpenDataRequest.init(
                responseType = "json",
                serviceName = SEOUL_RESTAURANT_OPEN_DATA_SERVICE_NAME,
                startIndex = startIndex.toInt(),
                totalCount = response.listTotalCount,
            ),
        ) {
            it.next()
        }
    }

    data class SeoulOpenDataRequest(
        val responseType: String,
        val serviceName: String,
        val startIndex: Int,
        val endIndex: Int,
        private val totalCount: Int?,
    ) {
        fun hasNext(): Boolean {
            if (totalCount == null) {
                return true
            }

            return endIndex < totalCount
        }

        fun next(): SeoulOpenDataRequest? {
            if (totalCount == null) {
                return null
            }

            if (endIndex >= totalCount) {
                return null
            }

            if (totalCount - endIndex > 1000) {
                return copy(
                    startIndex = endIndex + 1,
                    endIndex = endIndex + 1000,
                )
            }

            return copy(
                startIndex = endIndex + 1,
                endIndex = endIndex + (totalCount - endIndex),
            )
        }

        companion object {
            fun init(
                responseType: String,
                serviceName: String,
                startIndex: Int,
                endIndex: Int = 1000,
                totalCount: Int? = null,
            ): SeoulOpenDataRequest {
                return SeoulOpenDataRequest(
                    responseType = responseType,
                    serviceName = serviceName,
                    startIndex = startIndex,
                    endIndex = totalCount?.let { min(endIndex, totalCount) } ?: endIndex,
                    totalCount = totalCount,
                )
            }
        }
    }

    fun String.formatTelephoneNumber(): String {
        if (this.isBlank()) {
            return ""
        }

        val origin = this@formatTelephoneNumber.replace(" ", "")

        if (origin.startsWith("070")) {
            return origin
        }

        if (origin.startsWith("2")) {
            return buildString {
                append("02")

                if (origin[1] == '0') {
                    append(origin.substring(2, origin.length))
                }

                if (origin[1] != '0') {
                    append(origin.substring(1, origin.length))
                }
            }
        }

        if (origin.startsWith("02")) {
            return buildString {
                append("02")

                if (origin[2] == '0') {
                    append(origin.substring(3, origin.length))
                }

                if (origin[2] != '0') {
                    append(origin.substring(2, origin.length))
                }
            }
        }

        if (origin.length == 7 || origin.length == 8) {
            return buildString {
                append("02")

                if (origin[0] == '0') {
                    append(origin.substring(1, origin.length))
                }

                if (origin[0] != '0') {
                    append(origin.substring(0, origin.length))
                }
            }
        }

        return origin
    }

    private fun String.toNullIfBlank(): String? = this.takeIf { it.isNotBlank() }

    companion object {
        private const val SEOUL_RESTAURANT_OPEN_DATA_SERVICE_NAME = "LOCALDATA_072404"
    }
}
