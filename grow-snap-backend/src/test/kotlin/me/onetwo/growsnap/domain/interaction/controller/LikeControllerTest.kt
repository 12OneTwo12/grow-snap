package me.onetwo.growsnap.domain.interaction.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import me.onetwo.growsnap.config.TestSecurityConfig
import me.onetwo.growsnap.domain.interaction.dto.LikeCountResponse
import me.onetwo.growsnap.domain.interaction.dto.LikeResponse
import me.onetwo.growsnap.domain.interaction.service.LikeService
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

@WebFluxTest(LikeController::class)
@Import(TestSecurityConfig::class)
@ActiveProfiles("test")
@DisplayName("좋아요 Controller 테스트")
class LikeControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var likeService: LikeService

    @Nested
    @DisplayName("POST /api/v1/videos/{videoId}/like - 좋아요")
    inner class LikeVideo {

        @Test
        @DisplayName("유효한 요청으로 좋아요 시, 200 OK와 좋아요 응답을 반환한다")
        fun likeVideo_WithValidRequest_ReturnsLikeResponse() {
            // Given: 좋아요 요청
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()
            val response = LikeResponse(
                contentId = videoId,
                likeCount = 10,
                isLiked = true
            )

            every { likeService.likeContent(userId, UUID.fromString(videoId)) } returns Mono.just(response)

            // When & Then: API 호출 및 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .post()
                .uri("/api/v1/videos/$videoId/like")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.contentId").isEqualTo(videoId)
                .jsonPath("$.isLiked").isEqualTo(true)
                .jsonPath("$.likeCount").isEqualTo(10)

            verify(exactly = 1) { likeService.likeContent(userId, UUID.fromString(videoId)) }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/videos/{videoId}/like - 좋아요 취소")
    inner class UnlikeVideo {

        @Test
        @DisplayName("유효한 요청으로 좋아요 취소 시, 200 OK와 좋아요 응답을 반환한다")
        fun unlikeVideo_WithValidRequest_ReturnsLikeResponse() {
            // Given: 좋아요 취소 요청
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()
            val response = LikeResponse(
                contentId = videoId,
                likeCount = 9,
                isLiked = false
            )

            every { likeService.unlikeContent(userId, UUID.fromString(videoId)) } returns Mono.just(response)

            // When & Then: API 호출 및 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .delete()
                .uri("/api/v1/videos/$videoId/like")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.contentId").isEqualTo(videoId)
                .jsonPath("$.isLiked").isEqualTo(false)
                .jsonPath("$.likeCount").isEqualTo(9)

            verify(exactly = 1) { likeService.unlikeContent(userId, UUID.fromString(videoId)) }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/videos/{videoId}/likes - 좋아요 수 조회")
    inner class GetLikeCount {

        @Test
        @DisplayName("좋아요 수 조회 시, 200 OK와 좋아요 수를 반환한다")
        fun getLikeCount_WithValidRequest_ReturnsLikeCount() {
            // Given: 좋아요 수 조회 요청
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()
            val response = LikeCountResponse(
                contentId = videoId,
                likeCount = 15
            )

            every { likeService.getLikeCount(UUID.fromString(videoId)) } returns Mono.just(response)

            // When & Then: API 호출 및 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .get()
                .uri("/api/v1/videos/$videoId/likes")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.contentId").isEqualTo(videoId)
                .jsonPath("$.likeCount").isEqualTo(15)

            verify(exactly = 1) { likeService.getLikeCount(UUID.fromString(videoId)) }
        }
    }
}
