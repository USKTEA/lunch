package com.usktea.lunch.service.entity

import com.usktea.lunch.entity.TokenEntity
import com.usktea.lunch.repository.TokenRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TokenEntityService(
    private val jdbcTemplate: JdbcTemplate,
    private val tokenRepository: TokenRepository,
) {
    @Transactional
    fun save(tokenEntity: TokenEntity) {
        tokenRepository.save(tokenEntity)
    }

    fun findNotExpiredTokenByRefreshToken(refreshToken: String): TokenEntity? {
        return tokenRepository.findByRefreshTokenAndExpiresAtAfter(refreshToken, OffsetDateTime.now())
    }

    @Transactional
    fun useToken(id: Long): Int {
        return jdbcTemplate.update(useTokenSql, id)
    }

    fun revokeFamily(familyId: UUID) {
        jdbcTemplate.update(revokeFamilySql, familyId)
    }

    companion object {
        private val revokeFamilySql =
            """
            UPDATE lunch.token 
            SET revoked_at = now()
            WHERE family_id = ?
              AND revoked_at IS NULL
            """.trimIndent()

        private val useTokenSql =
            """
            UPDATE lunch.token t
            SET used_at = now()
            WHERE t.id = ?
              AND t.used_at IS NULL
              AND t.revoked_at IS NULL
              AND t.expires_at > now()
              AND pg_try_advisory_xact_lock(uuid_hash_extended(t.family_id, 0))
              AND t.generation = (
                    SELECT MAX(t2.generation)
                    FROM lunch.token t2
                    WHERE t2.family_id = t.family_id
                    );
            """.trimIndent()
    }
}
