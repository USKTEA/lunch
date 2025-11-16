package com.usktea.lunch.cdc

import java.time.LocalDate
import kotlin.reflect.KProperty

data class SeoulRestaurantV2ChangeEvent(
    val action: String,
    val data: Map<String, Any?>,
) {
    // Property delegation을 사용한 동적 타입 매핑
    val managementNumber: String by data
    val businessPlaceName: String by data
    val roadWholeAddress: String? by data
    val siteWholeAddress: String? by data
    val siteTel: String? by data
    val xCoordinate: Double? by data
    val yCoordinate: Double? by data
    val approvalDate: LocalDate? by data
    val closeDate: LocalDate? by data
    val tradeStateCode: String by data

    companion object {
        fun from(message: Wal2JsonV2Message): SeoulRestaurantV2ChangeEvent {
            return SeoulRestaurantV2ChangeEvent(
                action = message.action,
                data =
                    message.toMap().mapKeys { (key, _) ->
                        key.split("_").mapIndexed { index, word ->
                            if (index == 0) word else word.replaceFirstChar { it.uppercase() }
                        }.joinToString("")
                    },
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private operator fun <T> Map<String, Any>.getValue(
        thisRef: Any,
        property: KProperty<*>,
    ): T {
        return this[property.name] as T
    }
}
