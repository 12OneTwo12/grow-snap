package me.onetwo.growsnap.domain.interaction.service

import me.onetwo.growsnap.domain.interaction.dto.LikeCountResponse
import me.onetwo.growsnap.domain.interaction.dto.LikeResponse
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 좋아요 서비스 인터페이스
 *
 * 콘텐츠 좋아요 기능을 제공합니다.
 */
interface LikeService {

    /**
     * 좋아요
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 좋아요 응답
     */
    fun likeContent(userId: UUID, contentId: UUID): Mono<LikeResponse>

    /**
     * 좋아요 취소
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 좋아요 응답
     */
    fun unlikeContent(userId: UUID, contentId: UUID): Mono<LikeResponse>

    /**
     * 좋아요 수 조회
     *
     * @param contentId 콘텐츠 ID
     * @return 좋아요 수 응답
     */
    fun getLikeCount(contentId: UUID): Mono<LikeCountResponse>
}
