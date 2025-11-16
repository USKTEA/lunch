package com.usktea.lunch.util

import java.security.MessageDigest
import java.util.UUID

object NicknameGenerator {
    private val adjectives =
        listOf(
            "푸른", "조용한", "용감한", "고요한", "빠른",
            "은은한", "따뜻한", "차가운", "행복한", "빛나는",
            "배고픈", "게으른", "성실한", "즐거운", "외로운",
            "강한", "약한", "밝은", "어두운", "날카로운",
        )

    private val nouns =
        listOf(
            "별", "강", "늑대", "달", "구름",
            "바람", "꽃", "산", "하늘", "불꽃",
            "바나나", "나무", "물결", "고양이", "해",
            "사과", "달빛", "눈", "파도", "불",
        )

    fun fromUuid(uuid: UUID): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(uuid.toString().toByteArray())

        val adjIndex = (bytes[0].toInt() and 0xFF) % adjectives.size
        val nounIndex = (bytes[1].toInt() and 0xFF) % nouns.size

        val v =
            ((bytes[2].toLong() and 0xFF) shl 32) or
                ((bytes[3].toLong() and 0xFF) shl 24) or
                ((bytes[4].toLong() and 0xFF) shl 16) or
                ((bytes[5].toLong() and 0xFF) shl 8) or
                (bytes[6].toLong() and 0xFF)

        val suffix = (v % 60_466_176).toString(36).padStart(5, '0')

        return "${adjectives[adjIndex]}${nouns[nounIndex]}-$suffix"
    }
}
