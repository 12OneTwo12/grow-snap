package me.onetwo.growsnap.infrastructure.config

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import redis.embedded.RedisServer
import java.io.IOException

/**
 * 테스트용 Embedded Redis 설정
 *
 * 테스트 실행 시 자동으로 Redis 서버를 시작하고 종료합니다.
 * application-test.yml의 Redis 포트와 동일하게 설정됩니다.
 *
 * @Profile("test")를 사용하여 테스트 환경에서만 활성화됩니다.
 */
@Configuration
@Profile("test")
class EmbeddedRedisConfig {

    @Value("\${spring.data.redis.port:6379}")
    private var redisPort: Int = 6379

    private var redisServer: RedisServer? = null

    /**
     * 테스트 시작 시 Embedded Redis 서버 시작
     */
    @PostConstruct
    fun startRedis() {
        try {
            redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("maxmemory 128M")
                .build()
            redisServer?.start()
            logger.info("Embedded Redis started on port {}", redisPort)
        } catch (e: IOException) {
            // Redis가 이미 실행 중이거나 포트가 사용 중인 경우
            logger.warn("Redis server failed to start: {}", e.message)
        } catch (e: Exception) {
            logger.warn("Redis server error: {}", e.message)
        }
    }

    /**
     * 테스트 종료 시 Embedded Redis 서버 종료
     */
    @PreDestroy
    fun stopRedis() {
        try {
            redisServer?.stop()
            logger.info("Embedded Redis stopped")
        } catch (e: Exception) {
            logger.warn("Redis server failed to stop: {}", e.message)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EmbeddedRedisConfig::class.java)
    }
}
