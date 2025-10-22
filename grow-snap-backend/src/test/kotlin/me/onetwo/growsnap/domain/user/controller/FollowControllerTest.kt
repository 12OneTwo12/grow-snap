package me.onetwo.growsnap.domain.user.controller

import java.util.UUID

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import me.onetwo.growsnap.config.TestSecurityConfig
import me.onetwo.growsnap.domain.user.exception.AlreadyFollowingException
import me.onetwo.growsnap.domain.user.exception.CannotFollowSelfException
import me.onetwo.growsnap.domain.user.exception.NotFollowingException
import me.onetwo.growsnap.domain.user.model.Follow
import me.onetwo.growsnap.domain.user.service.FollowService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * FollowController 통합 테스트 + Spring Rest Docs
 */
@WebFluxTest(controllers = [FollowController::class])
@Import(TestSecurityConfig::class)
@AutoConfigureRestDocs
@ActiveProfiles("test")
@DisplayName("팔로우 Controller 테스트")
class FollowControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var followService: FollowService

    private val testUserId = UUID.randomUUID()
    private val testFollowingId = UUID.randomUUID()

    @Test
    @DisplayName("팔로우 성공")
    fun follow_Success() {
        // Given
        val follow = Follow(
            id = 1L,
            followerId = testUserId,
            followingId = testFollowingId
        )

        every { followService.follow(testUserId, testFollowingId) } returns follow

        // When & Then
        webTestClient.post()
            .uri("/api/v1/follows/{followingId}", testFollowingId)
            .header("X-User-Id", testUserId.toString())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.followerId").isEqualTo(testUserId.toString())
            .jsonPath("$.followingId").isEqualTo(testFollowingId.toString())
            .consumeWith(
                document(
                    "follow-create",
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("followingId").description("팔로우할 사용자 ID (UUID)")
                    ),
                    responseFields(
                        fieldWithPath("id").description("팔로우 관계 ID"),
                        fieldWithPath("followerId").description("팔로우하는 사용자 ID"),
                        fieldWithPath("followingId").description("팔로우받는 사용자 ID"),
                        fieldWithPath("createdAt").description("팔로우 생성일시")
                    )
                )
            )

        verify(exactly = 1) { followService.follow(testUserId, testFollowingId) }
    }

    @Test
    @DisplayName("팔로우 실패 - 자기 자신 팔로우")
    fun follow_SelfFollow_ThrowsException() {
        // Given
        every { followService.follow(testUserId, testUserId) } throws CannotFollowSelfException()

        // When & Then
        webTestClient.post()
            .uri("/api/v1/follows/{followingId}", testUserId)
            .header("X-User-Id", testUserId.toString())
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @DisplayName("팔로우 실패 - 이미 팔로우 중")
    fun follow_AlreadyFollowing_ThrowsException() {
        // Given
        every {
            followService.follow(testUserId, testFollowingId)
        } throws AlreadyFollowingException(testFollowingId)

        // When & Then
        webTestClient.post()
            .uri("/api/v1/follows/{followingId}", testFollowingId)
            .header("X-User-Id", testUserId.toString())
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @DisplayName("언팔로우 성공")
    fun unfollow_Success() {
        // Given
        justRun { followService.unfollow(testUserId, testFollowingId) }

        // When & Then
        webTestClient.delete()
            .uri("/api/v1/follows/{followingId}", testFollowingId)
            .header("X-User-Id", testUserId.toString())
            .exchange()
            .expectStatus().isNoContent
            .expectBody()
            .consumeWith(
                document(
                    "follow-delete",
                    pathParameters(
                        parameterWithName("followingId").description("언팔로우할 사용자 ID (UUID)")
                    )
                )
            )

        verify(exactly = 1) { followService.unfollow(testUserId, testFollowingId) }
    }

    @Test
    @DisplayName("언팔로우 실패 - 팔로우하지 않음")
    fun unfollow_NotFollowing_ThrowsException() {
        // Given
        every {
            followService.unfollow(testUserId, testFollowingId)
        } throws NotFollowingException(testFollowingId)

        // When & Then
        webTestClient.delete()
            .uri("/api/v1/follows/{followingId}", testFollowingId)
            .header("X-User-Id", testUserId.toString())
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @DisplayName("팔로우 관계 확인 - 팔로우 중")
    fun checkFollowing_Following_ReturnsTrue() {
        // Given
        every { followService.isFollowing(testUserId, testFollowingId) } returns true

        // When & Then
        webTestClient.get()
            .uri("/api/v1/follows/check/{followingId}", testFollowingId)
            .header("X-User-Id", testUserId.toString())
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.followerId").isEqualTo(testUserId.toString())
            .jsonPath("$.followingId").isEqualTo(testFollowingId.toString())
            .jsonPath("$.isFollowing").isEqualTo(true)
            .consumeWith(
                document(
                    "follow-check",
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("followingId").description("확인할 사용자 ID (UUID)")
                    ),
                    responseFields(
                        fieldWithPath("followerId").description("팔로우하는 사용자 ID"),
                        fieldWithPath("followingId").description("팔로우 대상 사용자 ID"),
                        fieldWithPath("isFollowing").description("팔로우 여부 (true: 팔로우 중, false: 팔로우 안함)")
                    )
                )
            )
    }

    @Test
    @DisplayName("팔로우 관계 확인 - 팔로우하지 않음")
    fun checkFollowing_NotFollowing_ReturnsFalse() {
        // Given
        every { followService.isFollowing(testUserId, testFollowingId) } returns false

        // When & Then
        webTestClient.get()
            .uri("/api/v1/follows/check/{followingId}", testFollowingId)
            .header("X-User-Id", testUserId.toString())
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.isFollowing").isEqualTo(false)
    }

    @Test
    @DisplayName("팔로우 통계 조회 - 특정 사용자")
    fun getFollowStats_Success() {
        // Given
        val targetUserId = UUID.randomUUID()
        every { followService.getFollowerCount(targetUserId) } returns 100
        every { followService.getFollowingCount(targetUserId) } returns 50

        // When & Then
        webTestClient.get()
            .uri("/api/v1/follows/stats/{targetUserId}", targetUserId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.userId").isEqualTo(targetUserId.toString())
            .jsonPath("$.followerCount").isEqualTo(100)
            .jsonPath("$.followingCount").isEqualTo(50)
            .consumeWith(
                document(
                    "follow-stats-get",
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("targetUserId").description("조회할 사용자 ID (UUID)")
                    ),
                    responseFields(
                        fieldWithPath("userId").description("사용자 ID"),
                        fieldWithPath("followerCount").description("팔로워 수"),
                        fieldWithPath("followingCount").description("팔로잉 수")
                    )
                )
            )
    }

    @Test
    @DisplayName("내 팔로우 통계 조회")
    fun getMyFollowStats_Success() {
        // Given
        every { followService.getFollowerCount(testUserId) } returns 25
        every { followService.getFollowingCount(testUserId) } returns 30

        // When & Then
        webTestClient.get()
            .uri("/api/v1/follows/stats/me")
            .header("X-User-Id", testUserId.toString())
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.userId").isEqualTo(testUserId.toString())
            .jsonPath("$.followerCount").isEqualTo(25)
            .jsonPath("$.followingCount").isEqualTo(30)
            .consumeWith(
                document(
                    "follow-stats-get-me",
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("userId").description("사용자 ID"),
                        fieldWithPath("followerCount").description("팔로워 수"),
                        fieldWithPath("followingCount").description("팔로잉 수")
                    )
                )
            )
    }
}
