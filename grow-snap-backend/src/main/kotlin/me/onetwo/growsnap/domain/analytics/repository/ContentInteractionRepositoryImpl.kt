package me.onetwo.growsnap.domain.analytics.repository

import me.onetwo.growsnap.jooq.generated.tables.ContentInteractions.Companion.CONTENT_INTERACTIONS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

/**
 * 콘텐츠 인터랙션 레포지토리 구현체
 *
 * JOOQ를 사용하여 content_interactions 테이블에 접근합니다.
 * 인터랙션 카운터를 원자적으로 증가시킵니다.
 *
 * @property dslContext JOOQ DSL Context
 */
@Repository
class ContentInteractionRepositoryImpl(
    private val dslContext: DSLContext
) : ContentInteractionRepository {

    /**
     * 조회수 증가
     *
     * view_count를 1 증가시키고, updated_at을 갱신합니다.
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    override fun incrementViewCount(contentId: UUID): Mono<Void> {
        return incrementCount(contentId, CONTENT_INTERACTIONS.VIEW_COUNT)
    }

    /**
     * 좋아요 수 증가
     *
     * like_count를 1 증가시키고, updated_at을 갱신합니다.
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    override fun incrementLikeCount(contentId: UUID): Mono<Void> {
        return incrementCount(contentId, CONTENT_INTERACTIONS.LIKE_COUNT)
    }

    /**
     * 저장 수 증가
     *
     * save_count를 1 증가시키고, updated_at을 갱신합니다.
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    override fun incrementSaveCount(contentId: UUID): Mono<Void> {
        return incrementCount(contentId, CONTENT_INTERACTIONS.SAVE_COUNT)
    }

    /**
     * 공유 수 증가
     *
     * share_count를 1 증가시키고, updated_at을 갱신합니다.
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    override fun incrementShareCount(contentId: UUID): Mono<Void> {
        return incrementCount(contentId, CONTENT_INTERACTIONS.SHARE_COUNT)
    }

    /**
     * 댓글 수 증가
     *
     * comment_count를 1 증가시키고, updated_at을 갱신합니다.
     *
     * @param contentId 콘텐츠 ID
     * @return 업데이트 완료 신호
     */
    override fun incrementCommentCount(contentId: UUID): Mono<Void> {
        return incrementCount(contentId, CONTENT_INTERACTIONS.COMMENT_COUNT)
    }

    /**
     * 카운터 증가 공통 로직
     *
     * 지정된 필드를 1 증가시키고, updated_at을 갱신합니다.
     *
     * @param contentId 콘텐츠 ID
     * @param field 증가시킬 필드
     * @return 업데이트 완료 신호
     */
    private fun incrementCount(contentId: UUID, field: org.jooq.Field<Int?>): Mono<Void> {
        return Mono.fromCallable {
            dslContext.update(CONTENT_INTERACTIONS)
                .set(field, field.plus(1))
                .set(CONTENT_INTERACTIONS.UPDATED_AT, LocalDateTime.now())
                .where(CONTENT_INTERACTIONS.CONTENT_ID.eq(contentId.toString()))
                .and(CONTENT_INTERACTIONS.DELETED_AT.isNull)
                .execute()
        }.then()
    }
}
