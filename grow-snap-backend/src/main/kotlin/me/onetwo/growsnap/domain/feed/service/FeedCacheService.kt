package me.onetwo.growsnap.domain.feed.service

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

/**
 * 피드 캐시 서비스
 *
 * Redis를 사용하여 추천 피드 결과를 캐싱합니다.
 * TikTok/Instagram Reels와 같은 숏폼 피드의 성능 최적화를 위해
 * 세션 기반으로 추천 결과를 미리 계산하여 저장합니다.
 *
 * ### 캐싱 전략
 * - 250개의 콘텐츠 ID를 미리 생성하여 캐싱
 * - TTL: 30분 (사용자 세션 유지 시간)
 * - Key 형식: `feed:rec:{userId}:batch:{batchNumber}`
 *
 * @property redisTemplate Reactive Redis Template
 */
@Service
class FeedCacheService(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {

    /**
     * 추천 피드 배치 조회
     *
     * Redis에서 캐싱된 추천 콘텐츠 ID 배치를 조회합니다.
     * 캐시가 없으면 empty Mono를 반환합니다.
     *
     * @param userId 사용자 ID
     * @param batchNumber 배치 번호 (0부터 시작)
     * @return 추천 콘텐츠 ID 목록 (최대 250개)
     */
    fun getRecommendationBatch(userId: UUID, batchNumber: Int): Mono<List<UUID>> {
        val key = buildBatchKey(userId, batchNumber)

        return redisTemplate.opsForList()
            .range(key, 0, -1)  // 전체 조회
            .map { UUID.fromString(it) }
            .collectList()
            .filter { it.isNotEmpty() }  // 빈 리스트는 캐시 miss로 처리
    }

    /**
     * 추천 피드 배치 저장
     *
     * 추천 알고리즘 결과를 Redis에 캐싱합니다.
     * TTL은 30분으로 설정됩니다.
     *
     * @param userId 사용자 ID
     * @param batchNumber 배치 번호
     * @param contentIds 추천 콘텐츠 ID 목록
     * @return 저장 완료 신호
     */
    fun saveRecommendationBatch(
        userId: UUID,
        batchNumber: Int,
        contentIds: List<UUID>
    ): Mono<Boolean> {
        if (contentIds.isEmpty()) {
            return Mono.just(false)
        }

        val key = buildBatchKey(userId, batchNumber)
        val values = contentIds.map { it.toString() }

        return redisTemplate.opsForList()
            .rightPushAll(key, values)
            .flatMap {
                // TTL 설정 (30분)
                redisTemplate.expire(key, BATCH_TTL)
            }
    }

    /**
     * 추천 피드 배치에서 특정 범위 조회
     *
     * 배치에서 offset부터 limit개만큼 조회합니다.
     * 페이지네이션을 위해 사용됩니다.
     *
     * @param userId 사용자 ID
     * @param batchNumber 배치 번호
     * @param offset 시작 인덱스
     * @param limit 조회 개수
     * @return 추천 콘텐츠 ID 목록
     */
    fun getRecommendationRange(
        userId: UUID,
        batchNumber: Int,
        offset: Long,
        limit: Long
    ): Flux<UUID> {
        val key = buildBatchKey(userId, batchNumber)

        return redisTemplate.opsForList()
            .range(key, offset, offset + limit - 1)
            .map { UUID.fromString(it) }
    }

    /**
     * 배치 크기 조회
     *
     * @param userId 사용자 ID
     * @param batchNumber 배치 번호
     * @return 배치에 저장된 ID 개수
     */
    fun getBatchSize(userId: UUID, batchNumber: Int): Mono<Long> {
        val key = buildBatchKey(userId, batchNumber)
        return redisTemplate.opsForList().size(key)
    }

    /**
     * 사용자의 모든 배치 삭제
     *
     * 사용자의 모든 추천 캐시를 삭제합니다.
     * 재추천이 필요할 때 사용합니다.
     *
     * @param userId 사용자 ID
     * @return 삭제 완료 신호
     */
    fun clearUserCache(userId: UUID): Mono<Boolean> {
        val pattern = "feed:rec:$userId:batch:*"

        return redisTemplate.keys(pattern)
            .collectList()
            .flatMap { keys ->
                if (keys.isEmpty()) {
                    Mono.just(true)
                } else {
                    redisTemplate.delete(*keys.toTypedArray())
                        .map { it > 0 }
                }
            }
    }

    /**
     * Redis 키 생성
     *
     * @param userId 사용자 ID
     * @param batchNumber 배치 번호
     * @return Redis key
     */
    private fun buildBatchKey(userId: UUID, batchNumber: Int): String {
        return "feed:rec:$userId:batch:$batchNumber"
    }

    companion object {
        /**
         * 배치당 콘텐츠 개수
         *
         * TikTok/Instagram Reels 방식: 250개를 미리 계산하여 캐싱
         */
        const val BATCH_SIZE = 250

        /**
         * 배치 TTL (30분)
         *
         * 사용자 세션 유지 시간
         */
        val BATCH_TTL: Duration = Duration.ofMinutes(30)

        /**
         * Prefetch 임계값 (50%)
         *
         * 사용자가 배치의 50%를 소진하면 다음 배치를 미리 생성
         */
        const val PREFETCH_THRESHOLD = 0.5
    }
}
