package me.onetwo.growsnap.domain.interaction.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import me.onetwo.growsnap.config.TestSecurityConfig
import me.onetwo.growsnap.domain.interaction.dto.ShareLinkResponse
import me.onetwo.growsnap.domain.interaction.dto.ShareResponse
import me.onetwo.growsnap.domain.interaction.service.ShareService
import me.onetwo.growsnap.util.mockUser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.util.UUID

@WebFluxTest(ShareController::class)
@Import(TestSecurityConfig::class)
@ActiveProfiles("test")
@DisplayName("공유 Controller 테스트")
class ShareControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var shareService: ShareService

    @Nested
    @DisplayName("POST /api/v1/videos/{videoId}/share - 콘텐츠 공유")
    inner class ShareVideo {

        @Test
        @DisplayName("유효한 요청으로 콘텐츠 공유 시, 200 OK와 공유 응답을 반환한다")
        fun shareVideo_WithValidRequest_ReturnsShareResponse() {
            // Given: 공유 요청
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()
            val response = ShareResponse(
                contentId = videoId,
                shareCount = 5
            )

            every { shareService.shareContent(userId, UUID.fromString(videoId)) } returns Mono.just(response)

            // When & Then: API 호출 및 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .post()
                .uri("/api/v1/videos/$videoId/share")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.contentId").isEqualTo(videoId)
                .jsonPath("$.shareCount").isEqualTo(5)

            verify(exactly = 1) { shareService.shareContent(userId, UUID.fromString(videoId)) }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/videos/{videoId}/share-link - 공유 링크 조회")
    inner class GetShareLink {

        @Test
        @DisplayName("공유 링크 조회 시, 200 OK와 공유 링크를 반환한다")
        fun getShareLink_WithValidRequest_ReturnsShareLink() {
            // Given: 공유 링크 조회 요청
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()
            val response = ShareLinkResponse(
                contentId = videoId,
                shareUrl = "https://growsnap.com/share/$videoId"
            )

            every { shareService.getShareLink(UUID.fromString(videoId)) } returns Mono.just(response)

            // When & Then: API 호출 및 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .get()
                .uri("/api/v1/videos/$videoId/share-link")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.contentId").isEqualTo(videoId)
                .jsonPath("$.shareUrl").isEqualTo("https://growsnap.com/share/$videoId")

            verify(exactly = 1) { shareService.getShareLink(UUID.fromString(videoId)) }
        }
    }
}
