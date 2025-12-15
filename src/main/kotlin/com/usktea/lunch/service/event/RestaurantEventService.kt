package com.usktea.lunch.service.event

import com.uber.h3core.H3Core
import com.usktea.lunch.cdc.SeoulRestaurantV2ChangeEvent
import com.usktea.lunch.client.NaverMapApiClient
import com.usktea.lunch.common.logger
import com.usktea.lunch.entity.RestaurantEntity
import com.usktea.lunch.service.entity.RestaurantEntityService
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.springframework.stereotype.Service

@Service
class RestaurantEventService(
    private val h3Core: H3Core,
    private val geometryFactory: GeometryFactory,
    private val naverMapApiClient: NaverMapApiClient,
    private val restaurantEntityService: RestaurantEntityService,
) {
    fun insertRestaurantByEvents(events: List<SeoulRestaurantV2ChangeEvent>) {
        val restaurants =
            events.mapNotNull { event ->
                val address = if (event.roadWholeAddress == null) event.siteWholeAddress else event.roadWholeAddress

                if (address == null) {
                    return@mapNotNull null
                }

                val addressResponse =
                    try {
                        naverMapApiClient.geoCoding(address).addresses.firstOrNull()
                    } catch (exception: Exception) {
                        logger.error("Failed to get address. events:{}", events.map { it.managementNumber }, exception)
                        null
                    }

                if (addressResponse == null) {
                    logger.error("Filtered cause no addressResponse From naver. event: {}", event)
                    return@mapNotNull null
                }

                val sido = addressResponse.addressElements.firstOrNull { it.types.contains("SIDO") }?.longName
                val sigungu = addressResponse.addressElements.firstOrNull { it.types.contains("SIGUGUN") }?.longName
                val dongmyun = addressResponse.addressElements.firstOrNull { it.types.contains("DONGMYUN") }?.longName
                val ri = addressResponse.addressElements.firstOrNull { it.types.contains("RI") }?.longName
                val roadName = addressResponse.addressElements.firstOrNull { it.types.contains("ROAD_NAME") }?.longName
                val buildingNumber =
                    addressResponse.addressElements.firstOrNull { it.types.contains("BUILDING_NUMBER") }?.longName
                val xCoordinate = addressResponse.x.toDouble()
                val yCoordinate = addressResponse.y.toDouble()
                val businessStatus =
                    when (event.tradeStateCode) {
                        "01" -> RestaurantEntity.BusinessStatus.OPEN
                        "03" -> RestaurantEntity.BusinessStatus.CLOSED
                        else -> RestaurantEntity.BusinessStatus.UNKNOWN
                    }

                RestaurantEntity(
                    managementNumber = event.managementNumber,
                    name = event.businessPlaceName,
                    contact = event.siteTel,
                    sido = sido,
                    sigungu = sigungu,
                    dongmyun = dongmyun,
                    ri = ri,
                    road = roadName,
                    buildingNumber = buildingNumber,
                    address = addressResponse.roadAddress ?: address,
                    location = geometryFactory.createPoint(Coordinate(xCoordinate, yCoordinate)),
                    h3Indices =
                        arrayOf(
                            h3Core.latLngToCellAddress(yCoordinate, xCoordinate, 7),
                            h3Core.latLngToCellAddress(yCoordinate, xCoordinate, 8),
                            h3Core.latLngToCellAddress(yCoordinate, xCoordinate, 9),
                            h3Core.latLngToCellAddress(yCoordinate, xCoordinate, 10),
                            h3Core.latLngToCellAddress(yCoordinate, xCoordinate, 11),
                        ),
                    status = businessStatus,
                )
            }

        restaurantEntityService.insert(restaurants)
    }
}
