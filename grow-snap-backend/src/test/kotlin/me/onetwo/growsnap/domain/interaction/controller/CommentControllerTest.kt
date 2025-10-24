package me.onetwo.growsnap.domain.interaction.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import me.onetwo.growsnap.config.TestSecurityConfig
import me.onetwo.growsnap.domain.interaction.dto.CommentRequest
import me.onetwo.growsnap.domain.interaction.dto.CommentResponse
import me.onetwo.growsnap.domain.interaction.exception.CommentException
import me.onetwo.growsnap.domain.interaction.service.CommentService
import me.onetwo.growsnap.util.mockUser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@WebFluxTest(CommentController::class)
@Import(TestSecurityConfig::class)
@ActiveProfiles("test")
@DisplayName("댓글 Controller 테스트")
class CommentControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var commentService: CommentService

    @Nested
    @DisplayName("POST /api/v1/videos/{videoId}/comments - 댓글 작성")
    inner class CreateComment {

        @Test
        @DisplayName("유효한 요청으로 댓글 작성 시, 201 Created와 댓글 정보를 반환한다")
        fun createComment_WithValidRequest_ReturnsCreatedComment() {
            // Given: 댓글 작성 요청
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()
            val request = CommentRequest(
                content = "Test comment",
                timestampSeconds = null,
                parentCommentId = null
            )

            val response = CommentResponse(
                id = UUID.randomUUID().toString(),
                contentId = videoId,
                userId = userId.toString(),
                userNickname = "TestUser",
                userProfileImageUrl = null,
                content = "Test comment",
                timestampSeconds = null,
                parentCommentId = null,
                createdAt = "2025-10-23T17:30:00",
                replies = emptyList()
            )

            every { commentService.createComment(userId, UUID.fromString(videoId), request) } returns Mono.just(response)

            // When & Then: API 호출 및 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .post()
                .uri("/api/v1/videos/$videoId/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.id").isEqualTo(response.id)
                .jsonPath("$.content").isEqualTo("Test comment")
                .jsonPath("$.userNickname").isEqualTo("TestUser")

            verify(exactly = 1) { commentService.createComment(userId, UUID.fromString(videoId), request) }
        }

        @Test
        @DisplayName("댓글 내용이 비어있으면, 400 Bad Request를 반환한다")
        fun createComment_WithEmptyContent_ReturnsBadRequest() {
            // Given: 빈 내용
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()
            val request = mapOf(
                "content" to "",
                "timestampSeconds" to null,
                "parentCommentId" to null
            )

            // When & Then: 400 응답 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .post()
                .uri("/api/v1/videos/$videoId/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest

            verify(exactly = 0) { commentService.createComment(any(), any(), any()) }
        }

        @Test
        @DisplayName("대댓글 작성 시, 부모 댓글 ID가 포함된다")
        fun createComment_WithParentId_CreatesReply() {
            // Given: 대댓글 요청
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()
            val parentCommentId = UUID.randomUUID().toString()
            val request = CommentRequest(
                content = "Reply comment",
                timestampSeconds = null,
                parentCommentId = parentCommentId
            )

            val response = CommentResponse(
                id = UUID.randomUUID().toString(),
                contentId = videoId,
                userId = userId.toString(),
                userNickname = "TestUser",
                userProfileImageUrl = null,
                content = "Reply comment",
                timestampSeconds = null,
                parentCommentId = parentCommentId,
                createdAt = "2025-10-23T17:30:00",
                replies = emptyList()
            )

            every { commentService.createComment(userId, UUID.fromString(videoId), request) } returns Mono.just(response)

            // When & Then: 대댓글 작성 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .post()
                .uri("/api/v1/videos/$videoId/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.parentCommentId").isEqualTo(parentCommentId)

            verify(exactly = 1) { commentService.createComment(userId, UUID.fromString(videoId), request) }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/videos/{videoId}/comments - 댓글 목록 조회")
    inner class GetComments {

        @Test
        @DisplayName("콘텐츠의 댓글 목록을 조회하면, 계층 구조로 반환된다")
        fun getComments_ReturnsHierarchicalComments() {
            // Given: 부모 댓글과 대댓글
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()
            val parentCommentId = UUID.randomUUID().toString()

            val reply = CommentResponse(
                id = UUID.randomUUID().toString(),
                contentId = videoId,
                userId = UUID.randomUUID().toString(),
                userNickname = "User2",
                userProfileImageUrl = null,
                content = "Reply comment",
                timestampSeconds = null,
                parentCommentId = parentCommentId,
                createdAt = "2025-10-23T17:31:00",
                replies = emptyList()
            )

            val parentComment = CommentResponse(
                id = parentCommentId,
                contentId = videoId,
                userId = UUID.randomUUID().toString(),
                userNickname = "User1",
                userProfileImageUrl = null,
                content = "Parent comment",
                timestampSeconds = null,
                parentCommentId = null,
                createdAt = "2025-10-23T17:30:00",
                replies = listOf(reply)
            )

            every { commentService.getComments(UUID.fromString(videoId)) } returns Flux.just(parentComment)

            // When & Then: API 호출 및 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .get()
                .uri("/api/v1/videos/$videoId/comments")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(parentCommentId)
                .jsonPath("$[0].content").isEqualTo("Parent comment")
                .jsonPath("$[0].replies.length()").isEqualTo(1)
                .jsonPath("$[0].replies[0].content").isEqualTo("Reply comment")

            verify(exactly = 1) { commentService.getComments(UUID.fromString(videoId)) }
        }

        @Test
        @DisplayName("댓글이 없으면, 빈 배열을 반환한다")
        fun getComments_WithNoComments_ReturnsEmptyArray() {
            // Given: 댓글이 없는 콘텐츠
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()

            every { commentService.getComments(UUID.fromString(videoId)) } returns Flux.empty()

            // When & Then: 빈 배열 반환 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .get()
                .uri("/api/v1/videos/$videoId/comments")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .json("[]")

            verify(exactly = 1) { commentService.getComments(UUID.fromString(videoId)) }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/comments/{commentId} - 댓글 삭제")
    inner class DeleteComment {

        @Test
        @DisplayName("자신의 댓글을 삭제하면, 204 No Content를 반환한다")
        fun deleteComment_OwnComment_ReturnsNoContent() {
            // Given: 댓글 ID
            val userId = UUID.randomUUID()
            val commentId = UUID.randomUUID().toString()

            every { commentService.deleteComment(userId, UUID.fromString(commentId)) } returns Mono.empty()

            // When & Then: API 호출 및 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .delete()
                .uri("/api/v1/comments/$commentId")
                .exchange()
                .expectStatus().isNoContent

            verify(exactly = 1) { commentService.deleteComment(userId, UUID.fromString(commentId)) }
        }

        @Test
        @DisplayName("존재하지 않는 댓글을 삭제하면, 404 Not Found를 반환한다")
        fun deleteComment_NonExistentComment_ReturnsNotFound() {
            // Given: 존재하지 않는 댓글 ID
            val userId = UUID.randomUUID()
            val commentId = UUID.randomUUID().toString()

            every { commentService.deleteComment(userId, UUID.fromString(commentId)) } returns
                Mono.error(CommentException.CommentNotFoundException(UUID.fromString(commentId)))

            // When & Then: 404 응답 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .delete()
                .uri("/api/v1/comments/$commentId")
                .exchange()
                .expectStatus().isNotFound

            verify(exactly = 1) { commentService.deleteComment(userId, UUID.fromString(commentId)) }
        }

        @Test
        @DisplayName("다른 사용자의 댓글을 삭제하면, 403 Forbidden을 반환한다")
        fun deleteComment_OthersComment_ReturnsForbidden() {
            // Given: 다른 사용자의 댓글
            val userId = UUID.randomUUID()
            val commentId = UUID.randomUUID().toString()

            every { commentService.deleteComment(userId, UUID.fromString(commentId)) } returns
                Mono.error(CommentException.CommentAccessDeniedException(UUID.fromString(commentId)))

            // When & Then: 403 응답 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .delete()
                .uri("/api/v1/comments/$commentId")
                .exchange()
                .expectStatus().isForbidden

            verify(exactly = 1) { commentService.deleteComment(userId, UUID.fromString(commentId)) }
        }
    }
}
