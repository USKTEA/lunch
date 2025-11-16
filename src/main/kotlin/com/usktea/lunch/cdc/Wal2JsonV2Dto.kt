package com.usktea.lunch.cdc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

@JsonIgnoreProperties(ignoreUnknown = true)
data class Wal2JsonV2Message(
    @JsonProperty("action")
    val action: String,
    @JsonProperty("schema")
    val schema: String? = null,
    @JsonProperty("table")
    val table: String = "",
    @JsonProperty("columns")
    val columns: List<ColumnData> = emptyList(),
    @JsonProperty("identity")
    val identity: List<ColumnData> = emptyList(),
) {
    val isBegin = action == "B"
    val isCommit = action == "C"
    val isInsert = action == "I"
    val isUpdate = action == "U"
    val isDelete = action == "D"
    val isTruncate = action == "T"
    val isMessage = action == "M"

    fun toMap(): Map<String, Any?> {
        if (isDelete) {
            return identity.associate { data ->
                data.name to data.getTypedValue()
            }
        }
        return columns.associate { data ->
            data.name to data.getTypedValue()
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ColumnData(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("type")
    val type: String,
    @JsonProperty("value")
    val value: Any?,
) {
    fun getTypedValue(): Any? {
        if (value == null) return null

        return try {
            when {
                type.equals("integer", ignoreCase = true) ->
                    when (value) {
                        is Number -> value.toInt()
                        is String -> value.toIntOrNull()
                        else -> value
                    }

                type.equals("double precision", ignoreCase = true) ->
                    when (value) {
                        is Number -> value.toDouble()
                        is String -> value.toDoubleOrNull()
                        else -> value
                    }

                type.equals("date", ignoreCase = true) -> LocalDate.parse(value.toString(), DateTimeFormatter.ISO_LOCAL_DATE)

                type.startsWith("timestamp with time zone", ignoreCase = true) -> {
                    OffsetDateTime.parse(value.toString().replace(" ", "T"), POSTGRES_TIMESTAMP_TZ_FORMATTER)
                }

                type.startsWith("timestamp", ignoreCase = true) -> {
                    LocalDateTime.parse(value.toString(), POSTGRES_TIMESTAMP_FORMATTER)
                }

                type.startsWith("character varying", ignoreCase = true) -> value.toString()

                else -> value
            }
        } catch (e: Exception) {
            value.toString()
        }
    }

    companion object {
        private val POSTGRES_TIMESTAMP_TZ_FORMATTER =
            DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .optionalEnd()
                .appendOffset("+HH:MM", "+00:00")
                .toFormatter()

        private val POSTGRES_TIMESTAMP_FORMATTER =
            DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .optionalEnd()
                .toFormatter()
    }
}
