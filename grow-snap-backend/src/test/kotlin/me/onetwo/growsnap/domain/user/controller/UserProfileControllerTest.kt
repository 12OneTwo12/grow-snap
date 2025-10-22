package me.onetwo.growsnap.domain.user.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import me.onetwo.growsnap.config.TestSecurityConfig
import me.onetwo.growsnap.domain.user.dto.CreateProfileRequest
import me.onetwo.growsnap.domain.user.dto.UpdateProfileRequest
import me.onetwo.growsnap.domain.user.exception.DuplicateNicknameException
import me.onetwo.growsnap.domain.user.exception.UserProfileNotFoundException
import me.onetwo.growsnap.domain.user.model.UserProfile
import me.onetwo.growsnap.domain.user.service.UserProfileService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * UserProfileController 통합 테스트 + Spring Rest Docs
 */
@WebFluxTest(controllers = [UserProfileController::class])
@Import(TestSecurityConfig::class)
@AutoConfigureRestDocs
@ActiveProfiles("test")
@DisplayName("사용자 프로필 Controller 테스트")
class UserProfileControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var userProfileService: UserProfileService

    private val testUserId = 1L
    private val testProfile = UserProfile(
        id = 1L,
        userId = testUserId,
        nickname = "testnick",
        profileImageUrl = "https://example.com/profile.jpg",
        bio = "테스트 자기소개",
        followerCount = 10,
        followingCount = 5
    )

    @Test
    @DisplayName("프로필 생성 성공")
    fun createProfile_Success() {
        // Given
        val request = CreateProfileRequest(
            nickname = "newnick",
            profileImageUrl = "https://example.com/new.jpg",
            bio = "새로운 자기소개"
        )

        every {
            userProfileService.createProfile(testUserId, request.nickname, request.profileImageUrl, request.bio)
        } returns testProfile.copy(
            nickname = request.nickname,
            profileImageUrl = request.profileImageUrl,
            bio = request.bio
        )

        // When & Then
        webTestClient.post()
            .uri("/api/v1/profiles")
            .header("X-User-Id", testUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.nickname").isEqualTo("newnick")
            .jsonPath("$.profileImageUrl").isEqualTo("https://example.com/new.jpg")
            .jsonPath("$.bio").isEqualTo("새로운 자기소개")
            .consumeWith(
                document(
                    "profile-create",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("nickname").description("닉네임 (2-20자)"),
                        fieldWithPath("profileImageUrl").description("프로필 이미지 URL (선택)").optional(),
                        fieldWithPath("bio").description("자기소개 (선택, 500자 이하)").optional()
                    ),
                    responseFields(
                        fieldWithPath("id").description("프로필 ID"),
                        fieldWithPath("userId").description("사용자 ID"),
                        fieldWithPath("nickname").description("닉네임"),
                        fieldWithPath("profileImageUrl").description("프로필 이미지 URL"),
                        fieldWithPath("bio").description("자기소개"),
                        fieldWithPath("followerCount").description("팔로워 수"),
                        fieldWithPath("followingCount").description("팔로잉 수"),
                        fieldWithPath("createdAt").description("생성일시"),
                        fieldWithPath("updatedAt").description("수정일시")
                    )
                )
            )

        verify(exactly = 1) {
            userProfileService.createProfile(testUserId, request.nickname, request.profileImageUrl, request.bio)
        }
    }

    @Test
    @DisplayName("프로필 생성 실패 - 중복 닉네임")
    fun createProfile_DuplicateNickname_ThrowsException() {
        // Given
        val request = CreateProfileRequest(nickname = "duplicatenick")

        every {
            userProfileService.createProfile(testUserId, request.nickname, null, null)
        } throws DuplicateNicknameException(request.nickname)

        // When & Then
        webTestClient.post()
            .uri("/api/v1/profiles")
            .header("X-User-Id", testUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @DisplayName("내 프로필 조회 성공")
    fun getMyProfile_Success() {
        // Given
        every { userProfileService.getProfileByUserId(testUserId) } returns testProfile

        // When & Then
        webTestClient.get()
            .uri("/api/v1/profiles/me")
            .header("X-User-Id", testUserId.toString())
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.nickname").isEqualTo("testnick")
            .jsonPath("$.followerCount").isEqualTo(10)
            .jsonPath("$.followingCount").isEqualTo(5)
            .consumeWith(
                document(
                    "profile-get-me",
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("id").description("프로필 ID"),
                        fieldWithPath("userId").description("사용자 ID"),
                        fieldWithPath("nickname").description("닉네임"),
                        fieldWithPath("profileImageUrl").description("프로필 이미지 URL"),
                        fieldWithPath("bio").description("자기소개"),
                        fieldWithPath("followerCount").description("팔로워 수"),
                        fieldWithPath("followingCount").description("팔로잉 수"),
                        fieldWithPath("createdAt").description("생성일시"),
                        fieldWithPath("updatedAt").description("수정일시")
                    )
                )
            )
    }

    @Test
    @DisplayName("사용자 ID로 프로필 조회 성공")
    fun getProfileByUserId_Success() {
        // Given
        val targetUserId = 2L
        every { userProfileService.getProfileByUserId(targetUserId) } returns testProfile

        // When & Then
        webTestClient.get()
            .uri("/api/v1/profiles/{targetUserId}", targetUserId)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.nickname").isEqualTo("testnick")
            .consumeWith(
                document(
                    "profile-get-by-userid",
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("targetUserId").description("조회할 사용자 ID")
                    ),
                    responseFields(
                        fieldWithPath("id").description("프로필 ID"),
                        fieldWithPath("userId").description("사용자 ID"),
                        fieldWithPath("nickname").description("닉네임"),
                        fieldWithPath("profileImageUrl").description("프로필 이미지 URL"),
                        fieldWithPath("bio").description("자기소개"),
                        fieldWithPath("followerCount").description("팔로워 수"),
                        fieldWithPath("followingCount").description("팔로잉 수"),
                        fieldWithPath("createdAt").description("생성일시"),
                        fieldWithPath("updatedAt").description("수정일시")
                    )
                )
            )
    }

    @Test
    @DisplayName("닉네임으로 프로필 조회 성공")
    fun getProfileByNickname_Success() {
        // Given
        val nickname = "testnick"
        every { userProfileService.getProfileByNickname(nickname) } returns testProfile

        // When & Then
        webTestClient.get()
            .uri("/api/v1/profiles/nickname/{nickname}", nickname)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.nickname").isEqualTo(nickname)
            .consumeWith(
                document(
                    "profile-get-by-nickname",
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("nickname").description("조회할 닉네임")
                    ),
                    responseFields(
                        fieldWithPath("id").description("프로필 ID"),
                        fieldWithPath("userId").description("사용자 ID"),
                        fieldWithPath("nickname").description("닉네임"),
                        fieldWithPath("profileImageUrl").description("프로필 이미지 URL"),
                        fieldWithPath("bio").description("자기소개"),
                        fieldWithPath("followerCount").description("팔로워 수"),
                        fieldWithPath("followingCount").description("팔로잉 수"),
                        fieldWithPath("createdAt").description("생성일시"),
                        fieldWithPath("updatedAt").description("수정일시")
                    )
                )
            )
    }

    @Test
    @DisplayName("프로필 조회 실패 - 프로필 없음")
    fun getProfile_NotFound() {
        // Given
        every { userProfileService.getProfileByUserId(999L) } throws
                UserProfileNotFoundException("프로필을 찾을 수 없습니다.")

        // When & Then
        webTestClient.get()
            .uri("/api/v1/profiles/{targetUserId}", 999L)
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    @DisplayName("프로필 수정 성공")
    fun updateProfile_Success() {
        // Given
        val request = UpdateProfileRequest(
            nickname = "updatednick",
            bio = "수정된 자기소개"
        )

        every {
            userProfileService.updateProfile(testUserId, request.nickname, request.profileImageUrl, request.bio)
        } returns testProfile.copy(nickname = "updatednick", bio = "수정된 자기소개")

        // When & Then
        webTestClient.patch()
            .uri("/api/v1/profiles")
            .header("X-User-Id", testUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.nickname").isEqualTo("updatednick")
            .jsonPath("$.bio").isEqualTo("수정된 자기소개")
            .consumeWith(
                document(
                    "profile-update",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("nickname").description("변경할 닉네임 (선택)").optional(),
                        fieldWithPath("profileImageUrl").description("변경할 프로필 이미지 URL (선택)").optional(),
                        fieldWithPath("bio").description("변경할 자기소개 (선택)").optional()
                    ),
                    responseFields(
                        fieldWithPath("id").description("프로필 ID"),
                        fieldWithPath("userId").description("사용자 ID"),
                        fieldWithPath("nickname").description("닉네임"),
                        fieldWithPath("profileImageUrl").description("프로필 이미지 URL"),
                        fieldWithPath("bio").description("자기소개"),
                        fieldWithPath("followerCount").description("팔로워 수"),
                        fieldWithPath("followingCount").description("팔로잉 수"),
                        fieldWithPath("createdAt").description("생성일시"),
                        fieldWithPath("updatedAt").description("수정일시")
                    )
                )
            )
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 중복됨")
    fun checkNickname_Duplicated() {
        // Given
        val nickname = "duplicatenick"
        every { userProfileService.isNicknameDuplicated(nickname) } returns true

        // When & Then
        webTestClient.get()
            .uri("/api/v1/profiles/check/nickname/{nickname}", nickname)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.nickname").isEqualTo(nickname)
            .jsonPath("$.isDuplicated").isEqualTo(true)
            .consumeWith(
                document(
                    "profile-check-nickname",
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("nickname").description("확인할 닉네임")
                    ),
                    responseFields(
                        fieldWithPath("nickname").description("확인한 닉네임"),
                        fieldWithPath("isDuplicated").description("중복 여부 (true: 중복, false: 사용 가능)")
                    )
                )
            )
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 사용 가능")
    fun checkNickname_Available() {
        // Given
        val nickname = "availablenick"
        every { userProfileService.isNicknameDuplicated(nickname) } returns false

        // When & Then
        webTestClient.get()
            .uri("/api/v1/profiles/check/nickname/{nickname}", nickname)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.isDuplicated").isEqualTo(false)
    }
}
