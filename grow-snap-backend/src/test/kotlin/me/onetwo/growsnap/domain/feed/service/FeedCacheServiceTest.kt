package me.onetwo.growsnap.domain.feed.service

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.redis.core.ReactiveListOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 피드 캐시 서비스 테스트
 *
 * Redis를 사용한 피드 캐싱 기능을 검증합니다.
 * MockK를 사용한 단위 테스트입니다.
 */
@ExtendWith(MockKExtension::class)
@DisplayName("피드 캐시 서비스 테스트")
class FeedCacheServiceTest {

    private lateinit var reactiveRedisTemplate: ReactiveRedisTemplate<String, String>
    private lateinit var listOps: ReactiveListOperations<String, String>
    private lateinit var feedCacheService: FeedCacheService

    private val userId = UUID.randomUUID()
    private val batchNumber = 0

    @BeforeEach
    fun setUp() {
        reactiveRedisTemplate = mockk(relaxed = true)
        listOps = mockk(relaxed = true)

        every { reactiveRedisTemplate.opsForList() } returns listOps

        feedCacheService = FeedCacheService(reactiveRedisTemplate)
    }

    @Nested
    @DisplayName("getRecommendationBatch - 추천 배치 조회")
    inner class GetRecommendationBatch {

        @Test
        @DisplayName("캐시에 데이터가 있는 경우, 저장된 콘텐츠 ID 목록을 반환한다")
        fun getRecommendationBatch_WithCachedData_ReturnsContentIds() {
            // Given: Redis에 콘텐츠 ID 배치 저장
            val contentIds = List(250) { UUID.randomUUID() }
            val contentIdStrings = contentIds.map { it.toString() }
            val key = "feed:rec:$userId:batch:$batchNumber"

            every { listOps.range(key, 0, -1) } returns Flux.fromIterable(contentIdStrings)

            // When: 캐시에서 배치 조회
            val result = feedCacheService.getRecommendationBatch(userId, batchNumber)

            // Then: 저장된 ID 목록이 반환됨
            StepVerifier.create(result)
                .assertNext { cachedIds ->
                    assertEquals(250, cachedIds.size)
                    assertEquals(contentIds, cachedIds)
                }
                .verifyComplete()

            verify(exactly = 1) { listOps.range(key, 0, -1) }
        }

        @Test
        @DisplayName("캐시에 데이터가 없는 경우, empty Mono를 반환한다")
        fun getRecommendationBatch_WithoutCachedData_ReturnsEmpty() {
            // Given: 캐시에 데이터 없음
            val key = "feed:rec:$userId:batch:$batchNumber"
            every { listOps.range(key, 0, -1) } returns Flux.empty()

            // When: 존재하지 않는 배치 조회
            val result = feedCacheService.getRecommendationBatch(userId, batchNumber)

            // Then: empty Mono 반환
            StepVerifier.create(result)
                .verifyComplete()
        }

        @Test
        @DisplayName("다른 사용자의 배치는 조회되지 않는다")
        fun getRecommendationBatch_WithDifferentUser_ReturnsEmpty() {
            // Given: user1의 배치는 있지만 user2의 배치는 없음
            val user1 = UUID.randomUUID()
            val user2 = UUID.randomUUID()
            val key2 = "feed:rec:$user2:batch:$batchNumber"

            every { listOps.range(key2, 0, -1) } returns Flux.empty()

            // When: user2로 조회
            val result = feedCacheService.getRecommendationBatch(user2, batchNumber)

            // Then: empty Mono 반환
            StepVerifier.create(result)
                .verifyComplete()
        }
    }

    @Nested
    @DisplayName("saveRecommendationBatch - 추천 배치 저장")
    inner class SaveRecommendationBatch {

        @Test
        @DisplayName("콘텐츠 ID 목록을 Redis에 저장하고 true를 반환한다")
        fun saveRecommendationBatch_WithValidData_SavesAndReturnsTrue() {
            // Given: 저장할 콘텐츠 ID 목록
            val contentIds = List(250) { UUID.randomUUID() }
            val contentIdStrings = contentIds.map { it.toString() }
            val key = "feed:rec:$userId:batch:$batchNumber"

            every { listOps.rightPushAll(key, contentIdStrings) } returns Mono.just(250L)
            every { reactiveRedisTemplate.expire(key, Duration.ofMinutes(30)) } returns Mono.just(true)

            // When: Redis에 저장
            val result = feedCacheService.saveRecommendationBatch(userId, batchNumber, contentIds)

            // Then: true 반환
            StepVerifier.create(result)
                .assertNext { success ->
                    assertTrue(success)
                }
                .verifyComplete()

            verify(exactly = 1) { listOps.rightPushAll(key, contentIdStrings) }
            verify(exactly = 1) { reactiveRedisTemplate.expire(key, Duration.ofMinutes(30)) }
        }

        @Test
        @DisplayName("빈 리스트를 저장하려는 경우, false를 반환한다")
        fun saveRecommendationBatch_WithEmptyList_ReturnsFalse() {
            // Given: 빈 리스트
            val emptyList = emptyList<UUID>()

            // When: 빈 리스트 저장 시도
            val result = feedCacheService.saveRecommendationBatch(userId, batchNumber, emptyList)

            // Then: false 반환
            StepVerifier.create(result)
                .assertNext { success ->
                    assertFalse(success)
                }
                .verifyComplete()

            // Then: Redis 호출 없음
            verify(exactly = 0) { listOps.rightPushAll(any(), any<List<String>>()) }
        }

        @Test
        @DisplayName("TTL이 30분으로 설정된다")
        fun saveRecommendationBatch_SetsTTL() {
            // Given: 저장할 콘텐츠 ID 목록
            val contentIds = List(10) { UUID.randomUUID() }
            val contentIdStrings = contentIds.map { it.toString() }
            val key = "feed:rec:$userId:batch:$batchNumber"

            every { listOps.rightPushAll(key, contentIdStrings) } returns Mono.just(10L)
            every { reactiveRedisTemplate.expire(key, Duration.ofMinutes(30)) } returns Mono.just(true)

            // When: Redis에 저장
            feedCacheService.saveRecommendationBatch(userId, batchNumber, contentIds).block()

            // Then: 30분 TTL로 expire 호출
            verify(exactly = 1) { reactiveRedisTemplate.expire(key, Duration.ofMinutes(30)) }
        }
    }

    @Nested
    @DisplayName("getBatchSize - 배치 크기 조회")
    inner class GetBatchSize {

        @Test
        @DisplayName("저장된 배치의 크기를 반환한다")
        fun getBatchSize_WithCachedData_ReturnsSize() {
            // Given: 250개의 콘텐츠 ID 저장됨
            val key = "feed:rec:$userId:batch:$batchNumber"
            every { listOps.size(key) } returns Mono.just(250L)

            // When: 배치 크기 조회
            val result = feedCacheService.getBatchSize(userId, batchNumber)

            // Then: 250 반환
            StepVerifier.create(result)
                .assertNext { size ->
                    assertEquals(250L, size)
                }
                .verifyComplete()

            verify(exactly = 1) { listOps.size(key) }
        }

        @Test
        @DisplayName("캐시에 데이터가 없는 경우, 0을 반환한다")
        fun getBatchSize_WithoutCachedData_ReturnsZero() {
            // Given: 캐시에 데이터 없음
            val key = "feed:rec:$userId:batch:$batchNumber"
            every { listOps.size(key) } returns Mono.just(0L)

            // When: 존재하지 않는 배치 크기 조회
            val result = feedCacheService.getBatchSize(userId, batchNumber)

            // Then: 0 반환
            StepVerifier.create(result)
                .assertNext { size ->
                    assertEquals(0L, size)
                }
                .verifyComplete()
        }
    }

    @Nested
    @DisplayName("clearUserCache - 사용자 캐시 삭제")
    inner class ClearUserCache {

        @Test
        @DisplayName("사용자의 모든 배치를 삭제하고 true를 반환한다")
        fun clearUserCache_DeletesAllBatchesAndReturnsTrue() {
            // Given: 사용자의 여러 배치가 있음
            val keys = listOf(
                "feed:rec:$userId:batch:0",
                "feed:rec:$userId:batch:1",
                "feed:rec:$userId:batch:2"
            )

            every { reactiveRedisTemplate.scan(any()) } returns Flux.fromIterable(keys)
            every { reactiveRedisTemplate.delete(any<Flux<String>>()) } returns Mono.just(3L)

            // When: 사용자 캐시 삭제
            val result = feedCacheService.clearUserCache(userId)

            // Then: true 반환
            StepVerifier.create(result)
                .assertNext { success ->
                    assertTrue(success)
                }
                .verifyComplete()

            verify(exactly = 1) { reactiveRedisTemplate.scan(any()) }
            verify(exactly = 1) { reactiveRedisTemplate.delete(any<Flux<String>>()) }
        }

        @Test
        @DisplayName("삭제할 캐시가 없는 경우에도 true를 반환한다")
        fun clearUserCache_WithoutCachedData_ReturnsTrue() {
            // Given: 캐시에 데이터 없음
            every { reactiveRedisTemplate.scan(any()) } returns Flux.empty()

            // When: 캐시 삭제 시도
            val result = feedCacheService.clearUserCache(userId)

            // Then: true 반환
            StepVerifier.create(result)
                .assertNext { success ->
                    assertTrue(success)
                }
                .verifyComplete()

            verify(exactly = 1) { reactiveRedisTemplate.scan(any()) }
            verify(exactly = 0) { reactiveRedisTemplate.delete(any<Flux<String>>()) }
        }

        @Test
        @DisplayName("다른 사용자의 캐시는 삭제되지 않는다")
        fun clearUserCache_DoesNotDeleteOtherUsersCache() {
            // Given: user1의 캐시만 있음
            val user1 = UUID.randomUUID()
            val keys = listOf("feed:rec:$user1:batch:0")

            every { reactiveRedisTemplate.scan(any()) } returns Flux.fromIterable(keys)
            every { reactiveRedisTemplate.delete(any<Flux<String>>()) } returns Mono.just(1L)

            // When: user1의 캐시만 삭제
            feedCacheService.clearUserCache(user1).block()

            // Then: scan과 delete 호출 확인
            verify(exactly = 1) { reactiveRedisTemplate.scan(any()) }
            verify(exactly = 1) { reactiveRedisTemplate.delete(any<Flux<String>>()) }
        }
    }
}
