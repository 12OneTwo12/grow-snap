package me.onetwo.growsnap.domain.analytics.repository

import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 콘텐츠 인터랙션 레포지토리
 *
 * content_interactions 테이블에 대한 데이터베이스 액세스를 담당합니다.
 * 인터랙션 카운터 (view, like, save, share, comment)를 관리합니다.
 */
interface ContentInteractionRepository {

    /**
     * 조회수 증가
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    fun incrementViewCount(contentId: UUID): Mono<Void>

    /**
     * 좋아요 수 증가
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    fun incrementLikeCount(contentId: UUID): Mono<Void>

    /**
     * 저장 수 증가
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    fun incrementSaveCount(contentId: UUID): Mono<Void>

    /**
     * 공유 수 증가
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    fun incrementShareCount(contentId: UUID): Mono<Void>

    /**
     * 댓글 수 증가
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    fun incrementCommentCount(contentId: UUID): Mono<Void>

    /**
     * 좋아요 수 감소
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    fun decrementLikeCount(contentId: UUID): Mono<Void>

    /**
     * 저장 수 감소
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    fun decrementSaveCount(contentId: UUID): Mono<Void>

    /**
     * 댓글 수 감소
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    fun decrementCommentCount(contentId: UUID): Mono<Void>

    /**
     * 좋아요 수 조회
     *
     * @param contentId 콘텐츠 ID
     * @return 좋아요 수
     */
    fun getLikeCount(contentId: UUID): Mono<Int>

    /**
     * 저장 수 조회
     *
     * @param contentId 콘텐츠 ID
     * @return 저장 수
     */
    fun getSaveCount(contentId: UUID): Mono<Int>
}
