package me.onetwo.growsnap.domain.interaction.controller

import me.onetwo.growsnap.domain.interaction.dto.SaveResponse
import me.onetwo.growsnap.domain.interaction.dto.SavedContentResponse
import me.onetwo.growsnap.domain.interaction.service.SaveService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.UUID

/**
 * 저장 컨트롤러
 *
 * 콘텐츠 저장 관련 HTTP 요청을 처리합니다.
 *
 * @property saveService 저장 서비스
 */
@RestController
@RequestMapping("/api/v1")
class SaveController(
    private val saveService: SaveService
) {

    /**
     * 콘텐츠 저장
     *
     * POST /api/v1/videos/{videoId}/save
     *
     * @param principal 인증된 사용자 Principal
     * @param videoId 비디오(콘텐츠) ID
     * @return 저장 응답
     */
    @PostMapping("/videos/{videoId}/save")
    fun saveVideo(
        principal: Mono<Principal>,
        @PathVariable videoId: String
    ): Mono<ResponseEntity<SaveResponse>> {
        return principal
            .map { UUID.fromString(it.name) }
            .flatMap { userId ->
                val contentId = UUID.fromString(videoId)
                saveService.saveContent(userId, contentId)
            }
            .map { response -> ResponseEntity.ok(response) }
    }

    /**
     * 콘텐츠 저장 취소
     *
     * DELETE /api/v1/videos/{videoId}/save
     *
     * @param principal 인증된 사용자 Principal
     * @param videoId 비디오(콘텐츠) ID
     * @return 저장 취소 응답
     */
    @DeleteMapping("/videos/{videoId}/save")
    fun unsaveVideo(
        principal: Mono<Principal>,
        @PathVariable videoId: String
    ): Mono<ResponseEntity<SaveResponse>> {
        return principal
            .map { UUID.fromString(it.name) }
            .flatMap { userId ->
                val contentId = UUID.fromString(videoId)
                saveService.unsaveContent(userId, contentId)
            }
            .map { response -> ResponseEntity.ok(response) }
    }

    /**
     * 저장한 콘텐츠 목록 조회
     *
     * GET /api/v1/users/me/saved-videos
     *
     * @param principal 인증된 사용자 Principal
     * @return 저장한 콘텐츠 목록
     */
    @GetMapping("/users/me/saved-videos")
    fun getSavedVideos(
        principal: Mono<Principal>
    ): Flux<SavedContentResponse> {
        return principal
            .map { UUID.fromString(it.name) }
            .flatMapMany { userId ->
                saveService.getSavedContents(userId)
            }
    }
}
