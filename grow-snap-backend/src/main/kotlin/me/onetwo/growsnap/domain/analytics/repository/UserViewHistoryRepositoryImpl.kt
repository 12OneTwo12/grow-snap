package me.onetwo.growsnap.domain.analytics.repository

import me.onetwo.growsnap.domain.analytics.dto.ViewHistoryDetail
import me.onetwo.growsnap.jooq.generated.tables.UserViewHistory.Companion.USER_VIEW_HISTORY
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

/**
 * 사용자 시청 기록 레포지토리 구현체
 *
 * JOOQ를 사용하여 user_view_history 테이블에 접근합니다.
 *
 * @property dslContext JOOQ DSL Context
 */
@Repository
class UserViewHistoryRepositoryImpl(
    private val dslContext: DSLContext
) : UserViewHistoryRepository {

    /**
     * 시청 기록 저장
     *
     * user_view_history 테이블에 새로운 시청 기록을 삽입합니다.
     * Audit Trail 필드 (created_at, created_by 등)도 함께 저장됩니다.
     *
     * @param userId 사용자 ID
     * @param contentId 콘텐츠 ID
     * @param watchedDuration 시청 시간 (초)
     * @param completionRate 완료율 (0-100)
     * @return 저장 완료 신호
     */
    override fun save(
        userId: UUID,
        contentId: UUID,
        watchedDuration: Int,
        completionRate: Int
    ): Mono<Void> {
        return Mono.fromCallable {
            dslContext.insertInto(USER_VIEW_HISTORY)
                .set(USER_VIEW_HISTORY.USER_ID, userId.toString())
                .set(USER_VIEW_HISTORY.CONTENT_ID, contentId.toString())
                .set(USER_VIEW_HISTORY.WATCHED_AT, LocalDateTime.now())
                .set(USER_VIEW_HISTORY.WATCHED_DURATION, watchedDuration)
                .set(USER_VIEW_HISTORY.COMPLETION_RATE, completionRate)
                .set(USER_VIEW_HISTORY.CREATED_AT, LocalDateTime.now())
                .set(USER_VIEW_HISTORY.CREATED_BY, userId.toString())
                .set(USER_VIEW_HISTORY.UPDATED_AT, LocalDateTime.now())
                .set(USER_VIEW_HISTORY.UPDATED_BY, userId.toString())
                .execute()
        }.then()
    }

    /**
     * 사용자의 최근 시청 기록 조회 (카테고리 선호도 분석용)
     *
     * 지정된 기간 이후의 시청 기록을 조회합니다.
     * Soft Delete된 데이터는 제외합니다.
     *
     * @param userId 사용자 ID
     * @param since 조회 시작 시각
     * @param limit 조회 개수
     * @return 시청한 콘텐츠 ID 목록 (최신순 정렬)
     */
    override fun findRecentViewedContentIds(
        userId: UUID,
        since: LocalDateTime,
        limit: Int
    ): Flux<UUID> {
        return Mono.fromCallable {
            dslContext
                .select(USER_VIEW_HISTORY.CONTENT_ID)
                .from(USER_VIEW_HISTORY)
                .where(USER_VIEW_HISTORY.USER_ID.eq(userId.toString()))
                .and(USER_VIEW_HISTORY.WATCHED_AT.ge(since))
                .and(USER_VIEW_HISTORY.DELETED_AT.isNull)
                .orderBy(USER_VIEW_HISTORY.WATCHED_AT.desc())
                .limit(limit)
                .fetch()
                .map { UUID.fromString(it.value1()) }
        }.flatMapMany { Flux.fromIterable(it) }
    }

    /**
     * 사용자의 최근 시청 기록 상세 정보 조회 (선호도 점수 계산용)
     *
     * 시청 시간, 완료율, 시청 시각 정보를 포함하여 조회합니다.
     * Soft Delete된 데이터는 제외합니다.
     *
     * @param userId 사용자 ID
     * @param since 조회 시작 시각
     * @param limit 조회 개수
     * @return 시청 기록 상세 정보 목록 (최신순 정렬)
     */
    override fun findRecentViewHistoryDetails(
        userId: UUID,
        since: LocalDateTime,
        limit: Int
    ): Flux<ViewHistoryDetail> {
        return Mono.fromCallable {
            dslContext
                .select(
                    USER_VIEW_HISTORY.CONTENT_ID,
                    USER_VIEW_HISTORY.WATCHED_DURATION,
                    USER_VIEW_HISTORY.COMPLETION_RATE,
                    USER_VIEW_HISTORY.WATCHED_AT
                )
                .from(USER_VIEW_HISTORY)
                .where(USER_VIEW_HISTORY.USER_ID.eq(userId.toString()))
                .and(USER_VIEW_HISTORY.WATCHED_AT.ge(since))
                .and(USER_VIEW_HISTORY.DELETED_AT.isNull)
                .orderBy(USER_VIEW_HISTORY.WATCHED_AT.desc())
                .limit(limit)
                .fetch()
                .map {
                    ViewHistoryDetail(
                        contentId = UUID.fromString(it.value1()),
                        watchedDuration = it.value2()!!,
                        completionRate = it.value3()!!,
                        watchedAt = it.value4()!!
                    )
                }
        }.flatMapMany { Flux.fromIterable(it) }
    }
}
