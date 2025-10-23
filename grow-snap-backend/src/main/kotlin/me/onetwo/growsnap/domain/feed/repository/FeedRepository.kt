package me.onetwo.growsnap.domain.feed.repository

import me.onetwo.growsnap.domain.feed.dto.FeedItemResponse
import reactor.core.publisher.Flux
import java.util.UUID

/**
 * 피드 레포지토리 인터페이스
 *
 * 피드 데이터를 조회하는 레포지토리입니다.
 */
interface FeedRepository {

    /**
     * 메인 피드 조회
     *
     * 추천 알고리즘 기반으로 사용자에게 맞춤화된 피드를 제공합니다.
     *
     * @param userId 사용자 ID
     * @param cursor 커서 (마지막 조회 콘텐츠 ID, null이면 첫 페이지)
     * @param limit 조회할 항목 수 (limit + 1개를 조회하여 hasNext 판단)
     * @param excludeContentIds 제외할 콘텐츠 ID 목록 (최근 본 콘텐츠)
     * @return 피드 아이템 목록
     */
    fun findMainFeed(
        userId: UUID,
        cursor: UUID?,
        limit: Int,
        excludeContentIds: List<UUID>
    ): Flux<FeedItemResponse>

    /**
     * 팔로잉 피드 조회
     *
     * 사용자가 팔로우한 크리에이터의 최신 콘텐츠를 제공합니다.
     *
     * @param userId 사용자 ID
     * @param cursor 커서 (마지막 조회 콘텐츠 ID, null이면 첫 페이지)
     * @param limit 조회할 항목 수 (limit + 1개를 조회하여 hasNext 판단)
     * @return 피드 아이템 목록
     */
    fun findFollowingFeed(
        userId: UUID,
        cursor: UUID?,
        limit: Int
    ): Flux<FeedItemResponse>

    /**
     * 최근 본 콘텐츠 ID 목록 조회
     *
     * 중복 콘텐츠 방지를 위해 사용자가 최근 본 콘텐츠 ID 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param limit 조회할 항목 수
     * @return 최근 본 콘텐츠 ID 목록
     */
    fun findRecentlyViewedContentIds(userId: UUID, limit: Int): Flux<UUID>

    /**
     * 인기 콘텐츠 ID 목록 조회
     *
     * 인터랙션 가중치 기반 인기도 점수가 높은 콘텐츠를 조회합니다.
     *
     * ### 인기도 계산 공식
     * ```
     * popularity_score = view_count * 1.0
     *                  + like_count * 5.0
     *                  + comment_count * 3.0
     *                  + save_count * 7.0
     *                  + share_count * 10.0
     * ```
     *
     * @param limit 조회할 항목 수
     * @param excludeIds 제외할 콘텐츠 ID 목록
     * @return 인기 콘텐츠 ID 목록 (인기도 순 정렬)
     */
    fun findPopularContentIds(limit: Int, excludeIds: List<UUID>): Flux<UUID>

    /**
     * 신규 콘텐츠 ID 목록 조회
     *
     * 최근 업로드된 콘텐츠를 조회합니다.
     *
     * @param limit 조회할 항목 수
     * @param excludeIds 제외할 콘텐츠 ID 목록
     * @return 신규 콘텐츠 ID 목록 (최신순 정렬)
     */
    fun findNewContentIds(limit: Int, excludeIds: List<UUID>): Flux<UUID>

    /**
     * 랜덤 콘텐츠 ID 목록 조회
     *
     * 무작위 콘텐츠를 조회하여 다양성을 확보합니다.
     *
     * @param limit 조회할 항목 수
     * @param excludeIds 제외할 콘텐츠 ID 목록
     * @return 랜덤 콘텐츠 ID 목록 (무작위 정렬)
     */
    fun findRandomContentIds(limit: Int, excludeIds: List<UUID>): Flux<UUID>
}
