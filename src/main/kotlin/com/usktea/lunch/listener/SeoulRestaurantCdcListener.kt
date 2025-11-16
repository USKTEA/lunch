package com.usktea.lunch.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.usktea.lunch.cdc.SeoulRestaurantV2ChangeEvent
import com.usktea.lunch.cdc.Wal2JsonV2Message
import com.usktea.lunch.service.event.RestaurantEventService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SeoulRestaurantCdcListener(
    @Value("\${spring.datasource.url}") private val baseUrl: String,
    @Value("\${spring.datasource.username}") private val user: String,
    @Value("\${spring.datasource.password}") private val password: String,
    objectMapper: ObjectMapper,
    private val restaurantEventService: RestaurantEventService,
) : Wal2JsonListener(baseUrl, user, password, objectMapper) {
    override val slotName: String = "seoul_restaurant"

    override fun processCdcEvent(messages: List<Wal2JsonV2Message>) {
        messages.filter { it.table == slotName }
            .groupBy { it.action }
            .mapValues { it -> it.value.map { SeoulRestaurantV2ChangeEvent.from(it) } }
            .forEach { (action, events) ->
                when (action) {
                    "I" -> onSeoulRestaurantCreated(events)
                    "U" -> onSeoulRestaurantUpdated(events)
                    "D" -> onSeoulRestaurantDeleted(events)
                    else -> doNothing()
                }
            }
    }

    private fun onSeoulRestaurantDeleted(events: List<SeoulRestaurantV2ChangeEvent>) {}

    private fun onSeoulRestaurantUpdated(events: List<SeoulRestaurantV2ChangeEvent>) {}

    private fun onSeoulRestaurantCreated(events: List<SeoulRestaurantV2ChangeEvent>) {
        restaurantEventService.insertRestaurantByEvents(events)
    }

    private fun doNothing() {}
}
