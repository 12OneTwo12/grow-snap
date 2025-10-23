package me.onetwo.growsnap.domain.interaction.repository

import me.onetwo.growsnap.domain.interaction.model.UserLike
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 사용자 좋아요 레포지토리 인터페이스
 *
 * 사용자의 콘텐츠 좋아요 상태를 관리합니다.
 */
interface UserLikeRepository {

    /**
     * 좋아요 생성
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 생성된 좋아요
     */
    fun save(userId: UUID, contentId: UUID): Mono<UserLike>

    /**
     * 좋아요 삭제 (Soft Delete)
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 처리 완료 신호
     */
    fun delete(userId: UUID, contentId: UUID): Mono<Void>

    /**
     * 좋아요 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 좋아요 여부 (true: 좋아요, false: 좋아요 안 함)
     */
    fun exists(userId: UUID, contentId: UUID): Mono<Boolean>

    /**
     * 사용자의 좋아요 조회
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @return 좋아요 (없으면 empty)
     */
    fun findByUserIdAndContentId(userId: UUID, contentId: UUID): Mono<UserLike>
}
