package me.onetwo.growsnap.domain.feed.service.recommendation

import me.onetwo.growsnap.domain.feed.repository.FeedRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 추천 서비스 구현체
 *
 * 요구사항 명세서 섹션 2.2.1의 추천 알고리즘을 구현합니다.
 * - 협업 추천 (40%): 유사 사용자 기반 (Phase 2에서 구현, 현재는 인기 콘텐츠로 대체)
 * - 인기 콘텐츠 (30%): 높은 인터랙션
 * - 신규 콘텐츠 (10%): 최근 업로드
 * - 랜덤 콘텐츠 (20%): 다양성 확보
 *
 * @property feedRepository 피드 데이터 조회를 위한 레포지토리
 */
@Service
class RecommendationServiceImpl(
    private val feedRepository: FeedRepository
) : RecommendationService {

    /**
     * 추천 콘텐츠 ID 목록 조회
     *
     * 여러 추천 전략을 병렬로 실행하여 콘텐츠 ID 목록을 반환합니다.
     *
     * ### 처리 흐름
     * 1. 각 전략별로 가져올 콘텐츠 수 계산
     * 2. **병렬로** 각 전략별 콘텐츠 조회 (Mono.zip 사용)
     * 3. 결과 합치기 및 무작위 섞기
     *
     * ### 성능 최적화
     * - Mono.zip을 사용하여 4개 전략을 동시에 실행
     * - 순차 처리(Flux.concat) 대비 약 4배 빠른 응답 시간
     *
     * @param userId 사용자 ID
     * @param limit 조회할 콘텐츠 수
     * @param excludeContentIds 제외할 콘텐츠 ID 목록
     * @return 추천 콘텐츠 ID 목록 (순서 무작위)
     */
    override fun getRecommendedContentIds(
        userId: UUID,
        limit: Int,
        excludeContentIds: List<UUID>
    ): Flux<UUID> {
        val strategyLimits = RecommendationStrategy.calculateLimits(limit)

        // 병렬로 모든 전략 실행
        return Mono.zip(
            getCollaborativeContentIds(userId, strategyLimits[RecommendationStrategy.COLLABORATIVE]!!, excludeContentIds).collectList(),
            getPopularContentIds(strategyLimits[RecommendationStrategy.POPULAR]!!, excludeContentIds).collectList(),
            getNewContentIds(strategyLimits[RecommendationStrategy.NEW]!!, excludeContentIds).collectList(),
            getRandomContentIds(strategyLimits[RecommendationStrategy.RANDOM]!!, excludeContentIds).collectList()
        ).flatMapMany { tuple ->
            // 모든 결과 합치기 및 무작위 섞기
            val allIds = tuple.t1 + tuple.t2 + tuple.t3 + tuple.t4
            Flux.fromIterable(allIds.shuffled())
        }
    }

    /**
     * 협업 추천 콘텐츠 ID 조회
     *
     * **Phase 2 구현 예정**: 현재는 인기 콘텐츠로 대체
     *
     * ### 향후 구현 계획
     * - 유사 사용자 찾기 (코사인 유사도)
     * - Matrix Factorization
     * - Neural Collaborative Filtering
     *
     * @param userId 사용자 ID
     * @param limit 조회할 콘텐츠 수
     * @param excludeContentIds 제외할 콘텐츠 ID 목록
     * @return 협업 추천 콘텐츠 ID 목록 (현재는 인기 콘텐츠)
     */
    private fun getCollaborativeContentIds(
        userId: UUID,
        limit: Int,
        excludeContentIds: List<UUID>
    ): Flux<UUID> {
        // Phase 2: 협업 필터링 알고리즘 구현 예정
        // 현재는 인기 콘텐츠로 대체
        return getPopularContentIds(limit, excludeContentIds)
    }

    /**
     * 인기 콘텐츠 ID 조회
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
     * @param limit 조회할 콘텐츠 수
     * @param excludeContentIds 제외할 콘텐츠 ID 목록
     * @return 인기 콘텐츠 ID 목록 (인기도 순 정렬)
     */
    private fun getPopularContentIds(
        limit: Int,
        excludeContentIds: List<UUID>
    ): Flux<UUID> {
        return feedRepository.findPopularContentIds(limit, excludeContentIds)
    }

    /**
     * 신규 콘텐츠 ID 조회
     *
     * 최근 업로드된 콘텐츠를 조회합니다.
     *
     * @param limit 조회할 콘텐츠 수
     * @param excludeContentIds 제외할 콘텐츠 ID 목록
     * @return 신규 콘텐츠 ID 목록 (최신순 정렬)
     */
    private fun getNewContentIds(
        limit: Int,
        excludeContentIds: List<UUID>
    ): Flux<UUID> {
        return feedRepository.findNewContentIds(limit, excludeContentIds)
    }

    /**
     * 랜덤 콘텐츠 ID 조회
     *
     * 무작위 콘텐츠를 조회하여 다양성을 확보합니다.
     *
     * @param limit 조회할 콘텐츠 수
     * @param excludeContentIds 제외할 콘텐츠 ID 목록
     * @return 랜덤 콘텐츠 ID 목록 (무작위 정렬)
     */
    private fun getRandomContentIds(
        limit: Int,
        excludeContentIds: List<UUID>
    ): Flux<UUID> {
        return feedRepository.findRandomContentIds(limit, excludeContentIds)
    }
}
