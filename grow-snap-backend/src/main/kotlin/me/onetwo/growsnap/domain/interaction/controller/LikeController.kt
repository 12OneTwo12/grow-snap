package me.onetwo.growsnap.domain.interaction.controller

import me.onetwo.growsnap.domain.interaction.dto.LikeCountResponse
import me.onetwo.growsnap.domain.interaction.dto.LikeResponse
import me.onetwo.growsnap.domain.interaction.service.LikeService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 좋아요 컨트롤러
 *
 * 콘텐츠 좋아요 관련 HTTP 요청을 처리합니다.
 *
 * @property likeService 좋아요 서비스
 */
@RestController
@RequestMapping("/api/v1/videos")
class LikeController(
    private val likeService: LikeService
) {

    /**
     * 좋아요
     *
     * POST /api/v1/videos/{videoId}/like
     *
     * @param userId 사용자 ID (Spring Security Context에서 추출)
     * @param videoId 비디오(콘텐츠) ID
     * @return 좋아요 응답
     */
    @PostMapping("/{videoId}/like")
    fun likeVideo(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable videoId: String
    ): Mono<ResponseEntity<LikeResponse>> {
        val contentId = UUID.fromString(videoId)

        return likeService.likeContent(userId, contentId)
            .map { response -> ResponseEntity.ok(response) }
    }

    /**
     * 좋아요 취소
     *
     * DELETE /api/v1/videos/{videoId}/like
     *
     * @param userId 사용자 ID (Spring Security Context에서 추출)
     * @param videoId 비디오(콘텐츠) ID
     * @return 좋아요 응답
     */
    @DeleteMapping("/{videoId}/like")
    fun unlikeVideo(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable videoId: String
    ): Mono<ResponseEntity<LikeResponse>> {
        val contentId = UUID.fromString(videoId)

        return likeService.unlikeContent(userId, contentId)
            .map { response -> ResponseEntity.ok(response) }
    }

    /**
     * 좋아요 수 조회
     *
     * GET /api/v1/videos/{videoId}/likes
     *
     * @param videoId 비디오(콘텐츠) ID
     * @return 좋아요 수 응답
     */
    @GetMapping("/{videoId}/likes")
    fun getLikeCount(
        @PathVariable videoId: String
    ): Mono<ResponseEntity<LikeCountResponse>> {
        val contentId = UUID.fromString(videoId)

        return likeService.getLikeCount(contentId)
            .map { response -> ResponseEntity.ok(response) }
    }
}
