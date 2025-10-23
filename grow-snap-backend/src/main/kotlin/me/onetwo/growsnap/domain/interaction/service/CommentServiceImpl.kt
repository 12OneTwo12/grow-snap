package me.onetwo.growsnap.domain.interaction.service

import me.onetwo.growsnap.domain.analytics.dto.InteractionEventRequest
import me.onetwo.growsnap.domain.analytics.dto.InteractionType
import me.onetwo.growsnap.domain.analytics.repository.ContentInteractionRepository
import me.onetwo.growsnap.domain.analytics.service.AnalyticsService
import me.onetwo.growsnap.domain.interaction.dto.CommentRequest
import me.onetwo.growsnap.domain.interaction.dto.CommentResponse
import me.onetwo.growsnap.domain.interaction.exception.CommentException
import me.onetwo.growsnap.domain.interaction.model.Comment
import me.onetwo.growsnap.domain.interaction.repository.CommentRepository
import me.onetwo.growsnap.jooq.generated.tables.UserProfiles.Companion.USER_PROFILES
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 댓글 서비스 구현체
 *
 * 댓글 작성, 조회, 삭제 비즈니스 로직을 처리합니다.
 *
 * ### 처리 흐름
 * 1. 댓글 작성 (comments 테이블)
 * 2. AnalyticsService를 통한 이벤트 발행
 *    - 카운터 증가 (content_interactions.comment_count)
 *    - Spring Event 발행 (UserInteractionEvent)
 *    - user_content_interactions 테이블 저장 (협업 필터링용)
 *
 * @property commentRepository 댓글 레포지토리
 * @property analyticsService Analytics 서비스 (이벤트 발행)
 * @property contentInteractionRepository 콘텐츠 인터랙션 레포지토리
 * @property dslContext JOOQ DSL Context
 */
@Service
class CommentServiceImpl(
    private val commentRepository: CommentRepository,
    private val analyticsService: AnalyticsService,
    private val contentInteractionRepository: ContentInteractionRepository,
    private val dslContext: DSLContext
) : CommentService {

    /**
     * 댓글 작성
     *
     * ### 처리 흐름
     * 1. 부모 댓글 존재 여부 확인 (대댓글인 경우)
     * 2. comments 테이블에 댓글 저장
     * 3. AnalyticsService.trackInteractionEvent(COMMENT) 호출
     *    - content_interactions의 comment_count 증가
     *    - UserInteractionEvent 발행
     *    - UserInteractionEventListener가 user_content_interactions 저장 (협업 필터링용)
     * 4. 사용자 정보 조회 후 응답 반환
     *
     * @param userId 작성자 ID
     * @param contentId 콘텐츠 ID
     * @param request 댓글 작성 요청
     * @return 작성된 댓글
     */
    override fun createComment(userId: UUID, contentId: UUID, request: CommentRequest): Mono<CommentResponse> {
        logger.debug("Creating comment: userId={}, contentId={}", userId, contentId)

        // 대댓글인 경우 부모 댓글 존재 확인
        val parentCommentId = request.parentCommentId?.let { UUID.fromString(it) }
        val validateParent = if (parentCommentId != null) {
            commentRepository.findById(parentCommentId)
                .switchIfEmpty(
                    Mono.error(CommentException.ParentCommentNotFoundException(parentCommentId))
                )
                .then()
        } else {
            Mono.empty()
        }

        return validateParent
            .then(
                commentRepository.save(
                    Comment(
                        contentId = contentId,
                        userId = userId,
                        parentCommentId = parentCommentId,
                        content = request.content,
                        timestampSeconds = request.timestampSeconds
                    )
                )
            )
            .flatMap { savedComment ->
                // AnalyticsService로 이벤트 발행 (카운터 증가 + user_content_interactions 저장)
                analyticsService.trackInteractionEvent(
                    userId,
                    InteractionEventRequest(
                        contentId = contentId,
                        interactionType = InteractionType.COMMENT
                    )
                )
                    .then(Mono.just(savedComment))
            }
            .flatMap { savedComment ->
                getUserInfo(userId).map { (nickname, profileImageUrl) ->
                    CommentResponse(
                        id = savedComment.id!!.toString(),
                        contentId = savedComment.contentId.toString(),
                        userId = savedComment.userId.toString(),
                        userNickname = nickname,
                        userProfileImageUrl = profileImageUrl,
                        content = savedComment.content,
                        timestampSeconds = savedComment.timestampSeconds,
                        parentCommentId = savedComment.parentCommentId?.toString(),
                        createdAt = savedComment.createdAt.toString()
                    )
                }
            }
            .doOnSuccess { logger.debug("Comment created successfully: commentId={}", it.id) }
            .doOnError { error ->
                logger.error("Failed to create comment: userId={}, contentId={}", userId, contentId, error)
            }
            .onErrorMap { error ->
                if (error is CommentException) error
                else CommentException.CommentCreationException(error.message)
            }
    }

    /**
     * 콘텐츠의 댓글 목록 조회
     *
     * ### 처리 흐름
     * 1. comments 테이블에서 해당 콘텐츠의 모든 댓글 조회
     * 2. user_profiles와 조인하여 작성자 정보 포함
     * 3. 계층 구조로 변환 (부모 댓글 - 대댓글)
     *
     * @param contentId 콘텐츠 ID
     * @return 댓글 목록 (계층 구조)
     */
    override fun getComments(contentId: UUID): Flux<CommentResponse> {
        logger.debug("Getting comments: contentId={}", contentId)

        return commentRepository.findByContentId(contentId)
            .collectList()
            .flatMapMany { comments ->
                val parentComments = comments.filter { it.parentCommentId == null }

                Flux.fromIterable(parentComments)
                    .flatMap { parentComment ->
                        getUserInfo(parentComment.userId).flatMap { (nickname, profileImageUrl) ->
                            val replies = comments
                                .filter { it.parentCommentId == parentComment.id }
                                .map { reply ->
                                    val (replyNickname, replyProfileImageUrl) = getUserInfo(reply.userId).block()
                                        ?: Pair("Unknown", null)
                                    CommentResponse(
                                        id = reply.id!!.toString(),
                                        contentId = reply.contentId.toString(),
                                        userId = reply.userId.toString(),
                                        userNickname = replyNickname,
                                        userProfileImageUrl = replyProfileImageUrl,
                                        content = reply.content,
                                        timestampSeconds = reply.timestampSeconds,
                                        parentCommentId = reply.parentCommentId?.toString(),
                                        createdAt = reply.createdAt.toString()
                                    )
                                }

                            Mono.just(
                                CommentResponse(
                                    id = parentComment.id!!.toString(),
                                    contentId = parentComment.contentId.toString(),
                                    userId = parentComment.userId.toString(),
                                    userNickname = nickname,
                                    userProfileImageUrl = profileImageUrl,
                                    content = parentComment.content,
                                    timestampSeconds = parentComment.timestampSeconds,
                                    parentCommentId = null,
                                    createdAt = parentComment.createdAt.toString(),
                                    replies = replies
                                )
                            )
                        }
                    }
            }
            .doOnComplete { logger.debug("Comments retrieved successfully: contentId={}", contentId) }
    }

    /**
     * 댓글 삭제
     *
     * ### 처리 흐름
     * 1. 댓글 존재 여부 및 소유권 확인
     * 2. comments 테이블에서 Soft Delete
     * 3. content_interactions의 comment_count 감소
     *
     * ### 비즈니스 규칙
     * - 자신의 댓글만 삭제 가능 (소유권 검증)
     * - user_content_interactions는 삭제하지 않음 (협업 필터링 데이터 보존)
     *
     * @param userId 요청한 사용자 ID
     * @param commentId 댓글 ID
     * @return Void
     */
    override fun deleteComment(userId: UUID, commentId: UUID): Mono<Void> {
        logger.debug("Deleting comment: userId={}, commentId={}", userId, commentId)

        return commentRepository.findById(commentId)
            .switchIfEmpty(Mono.error(CommentException.CommentNotFoundException(commentId)))
            .flatMap { comment ->
                // 소유권 확인
                if (comment.userId != userId) {
                    return@flatMap Mono.error<Void>(CommentException.CommentAccessDeniedException(commentId))
                }

                // 댓글 삭제 및 카운터 감소
                commentRepository.delete(commentId, userId)
                    .then(contentInteractionRepository.decrementCommentCount(comment.contentId))
            }
            .doOnSuccess { logger.debug("Comment deleted successfully: commentId={}", commentId) }
            .doOnError { error ->
                logger.error("Failed to delete comment: userId={}, commentId={}", userId, commentId, error)
            }
            .onErrorMap { error ->
                if (error is CommentException) error
                else CommentException.CommentDeletionException(error.message)
            }
    }

    /**
     * 사용자 정보 조회
     *
     * @param userId 사용자 ID
     * @return (닉네임, 프로필 이미지 URL)
     */
    private fun getUserInfo(userId: UUID): Mono<Pair<String, String?>> {
        return Mono.fromCallable {
            dslContext
                .select(USER_PROFILES.NICKNAME, USER_PROFILES.PROFILE_IMAGE_URL)
                .from(USER_PROFILES)
                .where(USER_PROFILES.USER_ID.eq(userId.toString()))
                .and(USER_PROFILES.DELETED_AT.isNull)
                .fetchOne()
                ?.let {
                    Pair(
                        it.getValue(USER_PROFILES.NICKNAME) ?: "Unknown",
                        it.getValue(USER_PROFILES.PROFILE_IMAGE_URL)
                    )
                } ?: Pair("Unknown", null)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommentServiceImpl::class.java)
    }
}
