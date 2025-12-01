package com.usktea.lunch.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class TokenType(
    @JsonValue
    val value: String,
) {
    BEARER("Bearer"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String): TokenType {
            return entries.find { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown token type: $value")
        }
    }
}
