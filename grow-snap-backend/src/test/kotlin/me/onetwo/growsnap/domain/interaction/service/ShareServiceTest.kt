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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.UUID

/**
 * 공유 서비스 단위 테스트
 *
 * Repository의 동작을 모킹하여 Service 계층의 비즈니스 로직만 검증합니다.
 */
@ExtendWith(MockKExtension::class)
@DisplayName("ShareService 단위 테스트")
class ShareServiceTest {

    @MockK
    private lateinit var analyticsService: AnalyticsService

    @MockK
    private lateinit var contentInteractionRepository: ContentInteractionRepository

    @InjectMockKs
    private lateinit var shareService: ShareServiceImpl

    private val testUserId = UUID.randomUUID()
    private val testContentId = UUID.randomUUID()

    @Nested
    @DisplayName("shareContent - 콘텐츠 공유")
    inner class ShareContent {

        @Test
        @DisplayName("콘텐츠를 공유하면, Analytics 이벤트를 발행하고 공유 수를 반환한다")
        fun shareContent_Success() {
            // Given
            every { analyticsService.trackInteractionEvent(any(), any()) } returns Mono.empty()
            every { contentInteractionRepository.getShareCount(testContentId) } returns Mono.just(5)

            // When
            val result = shareService.shareContent(testUserId, testContentId)

            // Then
            StepVerifier.create(result)
                .assertNext { response ->
                    assertEquals(testContentId.toString(), response.contentId)
                    assertEquals(5, response.shareCount)
                }
                .verifyComplete()

            verify(exactly = 1) {
                analyticsService.trackInteractionEvent(
                    testUserId,
                    InteractionEventRequest(
                        contentId = testContentId,
                        interactionType = InteractionType.SHARE
                    )
                )
            }
            verify(exactly = 1) { contentInteractionRepository.getShareCount(testContentId) }
        }
    }

    @Nested
    @DisplayName("getShareLink - 공유 링크 생성")
    inner class GetShareLink {

        @Test
        @DisplayName("공유 링크를 생성하면, 콘텐츠 ID가 포함된 URL을 반환한다")
        fun getShareLink_Success() {
            // When
            val result = shareService.getShareLink(testContentId)

            // Then
            StepVerifier.create(result)
                .assertNext { response ->
                    assertEquals(testContentId.toString(), response.contentId)
                    assertEquals("https://growsnap.com/watch/$testContentId", response.shareUrl)
                }
                .verifyComplete()
        }
    }
}
