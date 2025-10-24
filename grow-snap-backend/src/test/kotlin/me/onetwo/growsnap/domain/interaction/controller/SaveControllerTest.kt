package me.onetwo.growsnap.domain.interaction.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import me.onetwo.growsnap.config.TestSecurityConfig
import me.onetwo.growsnap.domain.interaction.dto.SaveResponse
import me.onetwo.growsnap.domain.interaction.dto.SavedContentResponse
import me.onetwo.growsnap.domain.interaction.service.SaveService
import me.onetwo.growsnap.infrastructure.config.RestDocsConfiguration
import me.onetwo.growsnap.util.mockUser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@WebFluxTest(SaveController::class)
@Import(RestDocsConfiguration::class, TestSecurityConfig::class)
@AutoConfigureRestDocs
@ActiveProfiles("test")
@DisplayName("저장 Controller 테스트")
class SaveControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var saveService: SaveService

    @Nested
    @DisplayName("POST /api/v1/videos/{videoId}/save - 콘텐츠 저장")
    inner class SaveVideo {

        @Test
        @DisplayName("유효한 요청으로 콘텐츠 저장 시, 200 OK와 저장 응답을 반환한다")
        fun saveVideo_WithValidRequest_ReturnsSaveResponse() {
            // Given: 저장 요청
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()
            val response = SaveResponse(
                contentId = videoId,
                saveCount = 5,
                isSaved = true
            )

            every { saveService.saveContent(userId, UUID.fromString(videoId)) } returns Mono.just(response)

            // When & Then: API 호출 및 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .post()
                .uri("/api/v1/videos/{videoId}/save", videoId)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.contentId").isEqualTo(videoId)
                .jsonPath("$.isSaved").isEqualTo(true)
                .jsonPath("$.saveCount").isEqualTo(5)
                .consumeWith(
                    document(
                        "save-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("videoId").description("비디오 ID")
                        ),
                        responseFields(
                            fieldWithPath("contentId").description("콘텐츠 ID"),
                            fieldWithPath("isSaved").description("저장 여부"),
                            fieldWithPath("saveCount").description("저장 수")
                        )
                    )
                )

            verify(exactly = 1) { saveService.saveContent(userId, UUID.fromString(videoId)) }
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/videos/{videoId}/save - 콘텐츠 저장 취소")
    inner class UnsaveVideo {

        @Test
        @DisplayName("유효한 요청으로 저장 취소 시, 200 OK와 저장 취소 응답을 반환한다")
        fun unsaveVideo_WithValidRequest_ReturnsSaveResponse() {
            // Given: 저장 취소 요청
            val userId = UUID.randomUUID()
            val videoId = UUID.randomUUID().toString()
            val response = SaveResponse(
                contentId = videoId,
                saveCount = 4,
                isSaved = false
            )

            every { saveService.unsaveContent(userId, UUID.fromString(videoId)) } returns Mono.just(response)

            // When & Then: API 호출 및 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .delete()
                .uri("/api/v1/videos/{videoId}/save", videoId)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.contentId").isEqualTo(videoId)
                .jsonPath("$.isSaved").isEqualTo(false)
                .jsonPath("$.saveCount").isEqualTo(4)
                .consumeWith(
                    document(
                        "save-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                            parameterWithName("videoId").description("비디오 ID")
                        ),
                        responseFields(
                            fieldWithPath("contentId").description("콘텐츠 ID"),
                            fieldWithPath("isSaved").description("저장 여부"),
                            fieldWithPath("saveCount").description("저장 수")
                        )
                    )
                )

            verify(exactly = 1) { saveService.unsaveContent(userId, UUID.fromString(videoId)) }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/saved-videos - 저장한 콘텐츠 목록 조회")
    inner class GetSavedVideos {

        @Test
        @DisplayName("저장한 콘텐츠 목록 조회 시, 200 OK와 콘텐츠 목록을 반환한다")
        fun getSavedVideos_WithValidRequest_ReturnsSavedContentList() {
            // Given: 저장한 콘텐츠 목록
            val userId = UUID.randomUUID()
            val savedContent1 = SavedContentResponse(
                contentId = UUID.randomUUID().toString(),
                title = "Saved Video 1",
                thumbnailUrl = "https://example.com/thumb1.jpg",
                savedAt = "2025-10-23T17:30:00"
            )
            val savedContent2 = SavedContentResponse(
                contentId = UUID.randomUUID().toString(),
                title = "Saved Video 2",
                thumbnailUrl = "https://example.com/thumb2.jpg",
                savedAt = "2025-10-23T17:31:00"
            )

            every { saveService.getSavedContents(userId) } returns Flux.just(savedContent1, savedContent2)

            // When & Then: API 호출 및 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .get()
                .uri("/api/v1/users/me/saved-videos")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].contentId").isEqualTo(savedContent1.contentId)
                .jsonPath("$[0].title").isEqualTo("Saved Video 1")
                .jsonPath("$[1].contentId").isEqualTo(savedContent2.contentId)
                .jsonPath("$[1].title").isEqualTo("Saved Video 2")
                .consumeWith(
                    document(
                        "save-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                            fieldWithPath("[].contentId").description("콘텐츠 ID"),
                            fieldWithPath("[].title").description("콘텐츠 제목"),
                            fieldWithPath("[].thumbnailUrl").description("썸네일 URL"),
                            fieldWithPath("[].savedAt").description("저장 시각")
                        )
                    )
                )

            verify(exactly = 1) { saveService.getSavedContents(userId) }
        }

        @Test
        @DisplayName("저장한 콘텐츠가 없으면, 빈 배열을 반환한다")
        fun getSavedVideos_WithNoSavedContents_ReturnsEmptyArray() {
            // Given: 저장한 콘텐츠가 없음
            val userId = UUID.randomUUID()

            every { saveService.getSavedContents(userId) } returns Flux.empty()

            // When & Then: 빈 배열 반환 검증
            webTestClient
                .mutateWith(mockUser(userId))
                .get()
                .uri("/api/v1/users/me/saved-videos")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .json("[]")

            verify(exactly = 1) { saveService.getSavedContents(userId) }
        }
    }
}
