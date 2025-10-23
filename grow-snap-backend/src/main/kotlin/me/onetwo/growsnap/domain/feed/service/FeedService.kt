package me.onetwo.growsnap.domain.feed.service

import me.onetwo.growsnap.infrastructure.common.dto.CursorPageRequest
import me.onetwo.growsnap.domain.feed.dto.FeedResponse
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 피드 서비스 인터페이스
 *
 * 피드 관련 비즈니스 로직을 처리합니다.
 */
interface FeedService {

    /**
     * 메인 피드 조회
     *
     * 추천 알고리즘 기반으로 사용자에게 맞춤화된 피드를 제공합니다.
     * 최근 본 콘텐츠는 제외하여 중복을 방지합니다.
     *
     * @param userId 사용자 ID
     * @param pageRequest 페이지네이션 요청
     * @return 피드 응답
     */
    fun getMainFeed(userId: UUID, pageRequest: CursorPageRequest): Mono<FeedResponse>

    /**
     * 팔로잉 피드 조회
     *
     * 사용자가 팔로우한 크리에이터의 최신 콘텐츠를 제공합니다.
     *
     * @param userId 사용자 ID
     * @param pageRequest 페이지네이션 요청
     * @return 피드 응답
     */
    fun getFollowingFeed(userId: UUID, pageRequest: CursorPageRequest): Mono<FeedResponse>
}
