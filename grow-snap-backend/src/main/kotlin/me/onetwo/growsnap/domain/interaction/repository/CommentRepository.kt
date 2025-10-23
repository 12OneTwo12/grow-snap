package me.onetwo.growsnap.domain.interaction.repository

import me.onetwo.growsnap.domain.interaction.model.Comment
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 댓글 Repository 인터페이스
 */
interface CommentRepository {

    /**
     * 댓글 저장
     *
     * @param comment 저장할 댓글
     * @return 저장된 댓글
     */
    fun save(comment: Comment): Mono<Comment>

    /**
     * 댓글 ID로 조회
     *
     * @param commentId 댓글 ID
     * @return 댓글 (없으면 empty Mono)
     */
    fun findById(commentId: UUID): Mono<Comment>

    /**
     * 콘텐츠의 모든 댓글 조회 (삭제되지 않은 것만)
     *
     * @param contentId 콘텐츠 ID
     * @return 댓글 목록
     */
    fun findByContentId(contentId: UUID): Flux<Comment>

    /**
     * 부모 댓글 ID로 대댓글 조회
     *
     * @param parentCommentId 부모 댓글 ID
     * @return 대댓글 목록
     */
    fun findByParentCommentId(parentCommentId: UUID): Flux<Comment>

    /**
     * 댓글 삭제 (Soft Delete)
     *
     * @param commentId 댓글 ID
     * @param userId 삭제하는 사용자 ID
     * @return Void
     */
    fun delete(commentId: UUID, userId: UUID): Mono<Void>
}
