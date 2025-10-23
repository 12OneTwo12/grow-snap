package me.onetwo.growsnap.domain.feed.service

import me.onetwo.growsnap.domain.feed.dto.FeedResponse
import me.onetwo.growsnap.domain.feed.repository.FeedRepository
import me.onetwo.growsnap.infrastructure.common.dto.CursorPageRequest
import me.onetwo.growsnap.infrastructure.common.dto.CursorPageResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 피드 서비스 구현체
 *
 * 피드 관련 비즈니스 로직을 처리합니다.
 *
 * @property feedRepository 피드 레포지토리
 */
@Service
class FeedServiceImpl(
    private val feedRepository: FeedRepository
) : FeedService {

    /**
     * 메인 피드 조회
     *
     * 추천 알고리즘 기반으로 사용자에게 맞춤화된 피드를 제공합니다.
     * 최근 본 콘텐츠는 제외하여 중복을 방지합니다.
     *
     * ### 처리 흐름
     * 1. 최근 본 콘텐츠 ID 목록 조회 (최근 100개)
     * 2. 메인 피드 조회 (limit + 1개를 조회하여 hasNext 판단)
     * 3. 커서 기반 페이지네이션 응답 생성
     *
     * @param userId 사용자 ID
     * @param pageRequest 페이지네이션 요청
     * @return 피드 응답
     */
    override fun getMainFeed(userId: UUID, pageRequest: CursorPageRequest): Mono<FeedResponse> {
        val cursor = pageRequest.cursor?.let { UUID.fromString(it) }
        val limit = pageRequest.limit

        return feedRepository.findRecentlyViewedContentIds(userId, RECENTLY_VIEWED_LIMIT)
            .collectList()
            .flatMap { recentlyViewedIds ->
                feedRepository.findMainFeed(
                    userId = userId,
                    cursor = cursor,
                    limit = limit + 1,  // +1 to check if there are more items
                    excludeContentIds = recentlyViewedIds
                )
                    .collectList()
                    .map { feedItems ->
                        CursorPageResponse.of(
                            content = feedItems,
                            limit = limit,
                            getCursor = { it.contentId.toString() }
                        )
                    }
            }
    }

    /**
     * 팔로잉 피드 조회
     *
     * 사용자가 팔로우한 크리에이터의 최신 콘텐츠를 제공합니다.
     *
     * ### 처리 흐름
     * 1. 팔로잉 피드 조회 (limit + 1개를 조회하여 hasNext 판단)
     * 2. 커서 기반 페이지네이션 응답 생성
     *
     * @param userId 사용자 ID
     * @param pageRequest 페이지네이션 요청
     * @return 피드 응답
     */
    override fun getFollowingFeed(userId: UUID, pageRequest: CursorPageRequest): Mono<FeedResponse> {
        val cursor = pageRequest.cursor?.let { UUID.fromString(it) }
        val limit = pageRequest.limit

        return feedRepository.findFollowingFeed(
            userId = userId,
            cursor = cursor,
            limit = limit + 1  // +1 to check if there are more items
        )
            .collectList()
            .map { feedItems ->
                CursorPageResponse.of(
                    content = feedItems,
                    limit = limit,
                    getCursor = { it.contentId.toString() }
                )
            }
    }

    companion object {
        /**
         * 최근 본 콘텐츠 조회 개수 (중복 방지용)
         */
        private const val RECENTLY_VIEWED_LIMIT = 100
    }
}
