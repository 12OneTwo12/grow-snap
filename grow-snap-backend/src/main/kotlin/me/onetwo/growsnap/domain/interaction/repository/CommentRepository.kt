package me.onetwo.growsnap.domain.interaction.repository

import me.onetwo.growsnap.domain.interaction.model.Comment
import java.util.UUID

/**
 * 댓글 Repository 인터페이스
 *
 * JOOQ를 사용하여 데이터베이스 CRUD 작업을 수행합니다.
 * Reactive 변환은 Service 계층에서 처리합니다.
 */
interface CommentRepository {

    /**
     * 댓글 저장
     *
     * @param comment 저장할 댓글
     * @return 저장된 댓글 (실패 시 null)
     */
    fun save(comment: Comment): Comment?

    /**
     * 댓글 ID로 조회
     *
     * @param commentId 댓글 ID
     * @return 댓글 (없으면 null)
     */
    fun findById(commentId: UUID): Comment?

    /**
     * 콘텐츠의 모든 댓글 조회 (삭제되지 않은 것만)
     *
     * @param contentId 콘텐츠 ID
     * @return 댓글 목록
     */
    fun findByContentId(contentId: UUID): List<Comment>

    /**
     * 부모 댓글 ID로 대댓글 조회
     *
     * @param parentCommentId 부모 댓글 ID
     * @return 대댓글 목록
     */
    fun findByParentCommentId(parentCommentId: UUID): List<Comment>

    /**
     * 댓글 삭제 (Soft Delete)
     *
     * @param commentId 댓글 ID
     * @param userId 삭제하는 사용자 ID
     */
    fun delete(commentId: UUID, userId: UUID)
}
