package me.onetwo.growsnap.domain.interaction.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import me.onetwo.growsnap.domain.analytics.dto.InteractionEventRequest
import me.onetwo.growsnap.domain.analytics.dto.InteractionType
import me.onetwo.growsnap.domain.analytics.repository.ContentInteractionRepository
import me.onetwo.growsnap.domain.analytics.service.AnalyticsService
import me.onetwo.growsnap.domain.interaction.model.UserLike
import me.onetwo.growsnap.domain.interaction.repository.UserLikeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime
import java.util.UUID

/**
 * 좋아요 서비스 단위 테스트
 *
 * Repository의 동작을 모킹하여 Service 계층의 비즈니스 로직만 검증합니다.
 */
@ExtendWith(MockKExtension::class)
@DisplayName("LikeService 단위 테스트")
class LikeServiceTest {

    @MockK
    private lateinit var userLikeRepository: UserLikeRepository

    @MockK
    private lateinit var analyticsService: AnalyticsService

    @MockK
    private lateinit var contentInteractionRepository: ContentInteractionRepository

    @InjectMockKs
    private lateinit var likeService: LikeServiceImpl

    private val testUserId = UUID.randomUUID()
    private val testContentId = UUID.randomUUID()

    @Nested
    @DisplayName("likeContent - 좋아요")
    inner class LikeContent {

        @Test
        @DisplayName("새로운 좋아요를 생성하면, Repository에 저장하고 Analytics 이벤트를 발행한다")
        fun likeContent_New_Success() {
            // Given
            val userLike = UserLike(
                id = 1L,
                userId = testUserId,
                contentId = testContentId,
                createdAt = LocalDateTime.now(),
                createdBy = testUserId,
                updatedAt = LocalDateTime.now(),
                updatedBy = testUserId
            )

            every { userLikeRepository.exists(testUserId, testContentId) } returns false
            every { userLikeRepository.save(testUserId, testContentId) } returns userLike
            every { analyticsService.trackInteractionEvent(any(), any()) } returns Mono.empty()
            every { contentInteractionRepository.getLikeCount(testContentId) } returns Mono.just(1)

            // When
            val result = likeService.likeContent(testUserId, testContentId)

            // Then
            StepVerifier.create(result)
                .assertNext { response ->
                    assertEquals(testContentId.toString(), response.contentId)
                    assertEquals(1, response.likeCount)
                    assertEquals(true, response.isLiked)
                }
                .verifyComplete()

            verify(exactly = 1) { userLikeRepository.save(testUserId, testContentId) }
            verify(exactly = 1) {
                analyticsService.trackInteractionEvent(
                    testUserId,
                    InteractionEventRequest(
                        contentId = testContentId,
                        interactionType = InteractionType.LIKE
                    )
                )
            }
        }

        @Test
        @DisplayName("이미 좋아요가 있으면, 중복 생성하지 않는다 (idempotent)")
        fun likeContent_AlreadyExists_Idempotent() {
            // Given
            every { userLikeRepository.exists(testUserId, testContentId) } returns true
            every { contentInteractionRepository.getLikeCount(testContentId) } returns Mono.just(1)

            // When
            val result = likeService.likeContent(testUserId, testContentId)

            // Then
            StepVerifier.create(result)
                .assertNext { response ->
                    assertEquals(testContentId.toString(), response.contentId)
                    assertEquals(1, response.likeCount)
                    assertEquals(true, response.isLiked)
                }
                .verifyComplete()

            verify(exactly = 0) { userLikeRepository.save(any(), any()) }
            verify(exactly = 0) { analyticsService.trackInteractionEvent(any(), any()) }
        }
    }

    @Nested
    @DisplayName("unlikeContent - 좋아요 취소")
    inner class UnlikeContent {

        @Test
        @DisplayName("좋아요를 취소하면, Repository에서 삭제하고 카운트를 감소시킨다")
        fun unlikeContent_Success() {
            // Given
            every { userLikeRepository.exists(testUserId, testContentId) } returns true
            every { userLikeRepository.delete(testUserId, testContentId) } returns Unit
            every { contentInteractionRepository.decrementLikeCount(testContentId) } returns Mono.empty()
            every { contentInteractionRepository.getLikeCount(testContentId) } returns Mono.just(0)

            // When
            val result = likeService.unlikeContent(testUserId, testContentId)

            // Then
            StepVerifier.create(result)
                .assertNext { response ->
                    assertEquals(testContentId.toString(), response.contentId)
                    assertEquals(0, response.likeCount)
                    assertEquals(false, response.isLiked)
                }
                .verifyComplete()

            verify(exactly = 1) { userLikeRepository.delete(testUserId, testContentId) }
            verify(exactly = 1) { contentInteractionRepository.decrementLikeCount(testContentId) }
        }

        @Test
        @DisplayName("좋아요가 없으면, 삭제하지 않는다 (idempotent)")
        fun unlikeContent_NotExists_Idempotent() {
            // Given
            every { userLikeRepository.exists(testUserId, testContentId) } returns false
            every { contentInteractionRepository.getLikeCount(testContentId) } returns Mono.just(0)

            // When
            val result = likeService.unlikeContent(testUserId, testContentId)

            // Then
            StepVerifier.create(result)
                .assertNext { response ->
                    assertEquals(testContentId.toString(), response.contentId)
                    assertEquals(0, response.likeCount)
                    assertEquals(false, response.isLiked)
                }
                .verifyComplete()

            verify(exactly = 0) { userLikeRepository.delete(any(), any()) }
            verify(exactly = 0) { contentInteractionRepository.decrementLikeCount(any()) }
        }
    }

    @Nested
    @DisplayName("getLikeCount - 좋아요 수 조회")
    inner class GetLikeCount {

        @Test
        @DisplayName("좋아요 수를 조회하면, ContentInteractionRepository에서 가져온다")
        fun getLikeCount_Success() {
            // Given
            every { contentInteractionRepository.getLikeCount(testContentId) } returns Mono.just(42)

            // When
            val result = likeService.getLikeCount(testContentId)

            // Then
            StepVerifier.create(result)
                .assertNext { response ->
                    assertEquals(testContentId.toString(), response.contentId)
                    assertEquals(42, response.likeCount)
                }
                .verifyComplete()
        }
    }
}
