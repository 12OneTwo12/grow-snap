package me.onetwo.growsnap.infrastructure.redis

import me.onetwo.growsnap.infrastructure.security.jwt.JwtProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

/**
 * Refresh Token Redis Repository
 *
 * Refresh Token을 Redis에 저장하고 관리합니다.
 * Key 형식: "refresh_token:{userId}"
 *
 * @property redisTemplate Redis 템플릿
 * @property jwtProperties JWT 설정
 */
@Repository
class RefreshTokenRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    private val jwtProperties: JwtProperties
) {
    companion object {
        private const val KEY_PREFIX = "refresh_token:"
    }

    /**
     * Refresh Token 저장
     *
     * @param userId 사용자 ID
     * @param refreshToken Refresh Token
     */
    fun save(userId: Long, refreshToken: String) {
        val key = getKey(userId)
        redisTemplate.opsForValue().set(
            key,
            refreshToken,
            jwtProperties.refreshTokenExpiration,
            TimeUnit.MILLISECONDS
        )
    }

    /**
     * Refresh Token 조회
     *
     * @param userId 사용자 ID
     * @return Refresh Token (존재하지 않으면 null)
     */
    fun findByUserId(userId: Long): String? {
        val key = getKey(userId)
        return redisTemplate.opsForValue().get(key)
    }

    /**
     * Refresh Token 삭제
     *
     * @param userId 사용자 ID
     */
    fun deleteByUserId(userId: Long) {
        val key = getKey(userId)
        redisTemplate.delete(key)
    }

    /**
     * Refresh Token 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return 존재하면 true, 그렇지 않으면 false
     */
    fun existsByUserId(userId: Long): Boolean {
        val key = getKey(userId)
        return redisTemplate.hasKey(key)
    }

    /**
     * Redis Key 생성
     *
     * @param userId 사용자 ID
     * @return Redis Key
     */
    private fun getKey(userId: Long): String {
        return "$KEY_PREFIX$userId"
    }
}
