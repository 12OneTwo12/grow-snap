package me.onetwo.growsnap.domain.feed.service.recommendation

import reactor.core.publisher.Flux
import java.util.UUID

/**
 * 추천 서비스 인터페이스
 *
 * 콘텐츠 추천 알고리즘을 제공합니다.
 */
interface RecommendationService {

    /**
     * 추천 콘텐츠 ID 목록 조회
     *
     * 여러 추천 전략을 혼합하여 콘텐츠 ID 목록을 반환합니다.
     *
     * @param userId 사용자 ID
     * @param limit 조회할 콘텐츠 수
     * @param excludeContentIds 제외할 콘텐츠 ID 목록
     * @return 추천 콘텐츠 ID 목록 (순서 무작위)
     */
    fun getRecommendedContentIds(
        userId: UUID,
        limit: Int,
        excludeContentIds: List<UUID>
    ): Flux<UUID>
}
