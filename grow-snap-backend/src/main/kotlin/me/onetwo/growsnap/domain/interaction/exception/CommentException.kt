package me.onetwo.growsnap.domain.interaction.exception

import java.util.UUID

/**
 * 댓글 관련 예외
 */
sealed class CommentException(message: String) : RuntimeException(message) {

    /**
     * 댓글을 찾을 수 없는 경우
     */
    class CommentNotFoundException(commentId: UUID) :
        CommentException("Comment not found: $commentId")

    /**
     * 댓글 작성 실패
     */
    class CommentCreationException(reason: String?) :
        CommentException("Comment creation failed: ${reason ?: "unknown"}")

    /**
     * 댓글 삭제 실패
     */
    class CommentDeletionException(reason: String?) :
        CommentException("Comment deletion failed: ${reason ?: "unknown"}")

    /**
     * 부모 댓글을 찾을 수 없는 경우
     */
    class ParentCommentNotFoundException(parentCommentId: UUID) :
        CommentException("Parent comment not found: $parentCommentId")

    /**
     * 권한 없음 (다른 사용자의 댓글 삭제 시도)
     */
    class CommentAccessDeniedException(commentId: UUID) :
        CommentException("Access denied to comment: $commentId")
}
