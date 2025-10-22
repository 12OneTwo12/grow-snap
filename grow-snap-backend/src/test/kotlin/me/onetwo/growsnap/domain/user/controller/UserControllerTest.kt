package me.onetwo.growsnap.domain.user.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import me.onetwo.growsnap.config.TestSecurityConfig
import me.onetwo.growsnap.domain.user.exception.UserNotFoundException
import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserRole
import me.onetwo.growsnap.domain.user.service.UserService
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
 * UserController 통합 테스트 + Spring Rest Docs
 */
@WebFluxTest(controllers = [UserController::class])
@Import(TestSecurityConfig::class)
@AutoConfigureRestDocs
@ActiveProfiles("test")
@DisplayName("사용자 Controller 테스트")
class UserControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var userService: UserService

    private val testUserId = 1L
    private val testUser = User(
        id = testUserId,
        email = "test@example.com",
        provider = OAuthProvider.GOOGLE,
        providerId = "google-123",
        role = UserRole.USER,
        isCreator = false
    )

    @Test
    @DisplayName("내 정보 조회 성공")
    fun getMe_Success() {
        // Given
        every { userService.getUserById(testUserId) } returns testUser

        // When & Then
        webTestClient.get()
            .uri("/api/v1/users/me")
            .header("X-User-Id", testUserId.toString())
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(testUserId)
            .jsonPath("$.email").isEqualTo("test@example.com")
            .jsonPath("$.provider").isEqualTo("GOOGLE")
            .jsonPath("$.role").isEqualTo("USER")
            .jsonPath("$.isCreator").isEqualTo(false)
            .consumeWith(
                document(
                    "user-get-me",
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("id").description("사용자 ID"),
                        fieldWithPath("email").description("이메일"),
                        fieldWithPath("provider").description("OAuth 제공자 (GOOGLE, NAVER, KAKAO)"),
                        fieldWithPath("role").description("사용자 역할 (USER, CREATOR, ADMIN)"),
                        fieldWithPath("isCreator").description("크리에이터 여부"),
                        fieldWithPath("createdAt").description("가입일시"),
                        fieldWithPath("updatedAt").description("수정일시")
                    )
                )
            )

        verify(exactly = 1) { userService.getUserById(testUserId) }
    }

    @Test
    @DisplayName("사용자 ID로 조회 성공")
    fun getUserById_Success() {
        // Given
        val targetUserId = 2L
        val targetUser = testUser.copy(id = targetUserId, email = "target@example.com")
        every { userService.getUserById(targetUserId) } returns targetUser

        // When & Then
        webTestClient.get()
            .uri("/api/v1/users/{targetUserId}", targetUserId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(targetUserId)
            .jsonPath("$.email").isEqualTo("target@example.com")
            .consumeWith(
                document(
                    "user-get-by-id",
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("targetUserId").description("조회할 사용자 ID")
                    ),
                    responseFields(
                        fieldWithPath("id").description("사용자 ID"),
                        fieldWithPath("email").description("이메일"),
                        fieldWithPath("provider").description("OAuth 제공자"),
                        fieldWithPath("role").description("사용자 역할"),
                        fieldWithPath("isCreator").description("크리에이터 여부"),
                        fieldWithPath("createdAt").description("가입일시"),
                        fieldWithPath("updatedAt").description("수정일시")
                    )
                )
            )
    }

    @Test
    @DisplayName("사용자 조회 실패 - 사용자 없음")
    fun getUserById_NotFound() {
        // Given
        every { userService.getUserById(999L) } throws
                UserNotFoundException("사용자를 찾을 수 없습니다.")

        // When & Then
        webTestClient.get()
            .uri("/api/v1/users/{targetUserId}", 999L)
            .exchange()
            .expectStatus().is5xxServerError
    }
}
