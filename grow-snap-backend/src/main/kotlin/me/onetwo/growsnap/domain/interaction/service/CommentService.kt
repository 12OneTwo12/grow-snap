package me.onetwo.growsnap.domain.interaction.service

import me.onetwo.growsnap.domain.interaction.dto.CommentRequest
import me.onetwo.growsnap.domain.interaction.dto.CommentResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 댓글 서비스 인터페이스
 */
interface CommentService {

    /**
     * 댓글 작성
     *
     * @param userId 작성자 ID
     * @param contentId 콘텐츠 ID
     * @param request 댓글 작성 요청
     * @return 작성된 댓글
     */
    fun createComment(userId: UUID, contentId: UUID, request: CommentRequest): Mono<CommentResponse>

    /**
     * 콘텐츠의 댓글 목록 조회
     *
     * @param contentId 콘텐츠 ID
     * @return 댓글 목록 (계층 구조, 대댓글 포함)
     */
    fun getComments(contentId: UUID): Flux<CommentResponse>

    /**
     * 댓글 삭제
     *
     * @param userId 요청한 사용자 ID
     * @param commentId 댓글 ID
     * @return Void
     */
    fun deleteComment(userId: UUID, commentId: UUID): Mono<Void>
}
