package me.onetwo.growsnap.domain.feed.service.collaborative

import me.onetwo.growsnap.domain.analytics.dto.InteractionType
import me.onetwo.growsnap.domain.analytics.repository.UserContentInteractionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 협업 필터링 서비스 구현체
 *
 * Item-based Collaborative Filtering 알고리즘을 구현합니다.
 *
 * ## 알고리즘 설명
 *
 * ### Item-based Collaborative Filtering
 * "이 콘텐츠를 좋아한 사람들은 이것도 좋아했습니다"
 *
 * ### 처리 흐름
 * ```
 * 1. 내가 좋아요/저장/공유한 콘텐츠 조회 (seed items)
 *    - 최대 100개 조회
 *
 * 2. 각 seed item에 대해:
 *    - 이 콘텐츠를 좋아한 다른 사용자 찾기 (최대 50명)
 *    - 그 사용자들이 좋아한 다른 콘텐츠 찾기 (각 20개)
 *
 * 3. 콘텐츠별 점수 계산:
 *    - 같은 콘텐츠가 여러 번 등장 = 공통 사용자가 많음
 *    - 인터랙션 타입별 가중치:
 *      * LIKE: 1.0
 *      * SAVE: 1.5 (저장 = 나중에 다시 보고 싶음)
 *      * SHARE: 2.0 (공유 = 타인에게 추천할 만큼 좋음)
 *
 * 4. 필터링:
 *    - 이미 내가 인터랙션한 콘텐츠 제외
 *
 * 5. 정렬 및 반환:
 *    - 점수 높은 순으로 정렬
 *    - 상위 N개 반환
 * ```
 *
 * ### 예시
 * ```
 * User A: Content1(좋아요), Content2(저장)
 * User B: Content1(좋아요), Content3(좋아요), Content5(공유)
 * User C: Content2(저장), Content4(좋아요)
 *
 * User A에게 추천:
 * 1. Content1을 좋아한 User B 찾기
 *    → User B가 좋아한 Content3, Content5 후보
 * 2. Content2를 저장한 User C 찾기
 *    → User C가 좋아한 Content4 후보
 * 3. 점수 계산:
 *    - Content3: 1.0 (LIKE)
 *    - Content5: 2.0 (SHARE)
 *    - Content4: 1.0 (LIKE)
 * 4. 추천: Content5 > Content3 = Content4
 * ```
 *
 * @property userContentInteractionRepository 사용자별 콘텐츠 인터랙션 레포지토리
 */
@Service
class CollaborativeFilteringServiceImpl(
    private val userContentInteractionRepository: UserContentInteractionRepository
) : CollaborativeFilteringService {

    /**
     * 협업 필터링 기반 콘텐츠 추천
     *
     * Item-based CF 알고리즘을 사용하여 추천합니다.
     *
     * @param userId 사용자 ID
     * @param limit 추천 개수
     * @return 추천 콘텐츠 ID 목록 (점수 높은 순)
     */
    override fun getRecommendedContents(userId: UUID, limit: Int): Flux<UUID> {
        // 1. 내가 인터랙션한 콘텐츠 조회 (seed items)
        val myInteractionsMono = userContentInteractionRepository
            .findAllInteractionsByUser(userId, MAX_SEED_ITEMS)
            .collectList()

        return myInteractionsMono.flatMapMany outer@{ myInteractions ->
            if (myInteractions.isEmpty()) {
                logger.debug("No interactions found for user {}", userId)
                return@outer Flux.empty<UUID>()
            }

            val myContentIds = myInteractions.map { it.first }.toSet()
            logger.debug("Found {} seed items for user {}", myContentIds.size, userId)

            // 2. 각 seed item을 좋아한 다른 사용자 찾기
            val similarUsersMono = Flux.fromIterable(myContentIds)
                .flatMap { contentId ->
                    userContentInteractionRepository
                        .findUsersByContent(contentId, interactionType = null, limit = MAX_SIMILAR_USERS_PER_ITEM)
                }
                .filter { it != userId }  // 자신 제외
                .distinct()
                .collectList()

            similarUsersMono.flatMapMany inner@{ similarUsers ->
                if (similarUsers.isEmpty()) {
                    logger.debug("No similar users found for user {}", userId)
                    return@inner Flux.empty<UUID>()
                }

                logger.debug("Found {} similar users for user {}", similarUsers.size, userId)

                // 3. 그 사용자들이 좋아한 콘텐츠 찾기
                Flux.fromIterable(similarUsers)
                    .flatMap { similarUserId ->
                        userContentInteractionRepository
                            .findAllInteractionsByUser(similarUserId, MAX_ITEMS_PER_SIMILAR_USER)
                    }
                    .collectList()
                    .flatMapMany { candidateInteractions ->
                        // 4. 콘텐츠별 점수 계산
                        val contentScores = mutableMapOf<UUID, Double>()

                        for ((contentId, interactionType) in candidateInteractions) {
                            // 이미 내가 인터랙션한 콘텐츠는 제외
                            if (myContentIds.contains(contentId)) {
                                continue
                            }

                            // 인터랙션 타입별 가중치 적용
                            val weight = when (interactionType) {
                                InteractionType.LIKE -> LIKE_WEIGHT
                                InteractionType.SAVE -> SAVE_WEIGHT
                                InteractionType.SHARE -> SHARE_WEIGHT
                                InteractionType.COMMENT -> 0.0  // COMMENT는 점수에 포함 안 함
                            }

                            contentScores[contentId] = (contentScores[contentId] ?: 0.0) + weight
                        }

                        logger.debug(
                            "Calculated scores for {} candidate contents for user {}",
                            contentScores.size,
                            userId
                        )

                        // 5. 점수 높은 순으로 정렬하여 반환 (점수가 0보다 큰 것만)
                        val recommendedContents = contentScores
                            .entries
                            .filter { it.value > 0.0 }  // COMMENT (점수 0.0) 제외
                            .sortedByDescending { it.value }
                            .take(limit)
                            .map { it.key }

                        logger.debug(
                            "Returning {} recommended contents for user {}",
                            recommendedContents.size,
                            userId
                        )

                        Flux.fromIterable(recommendedContents)
                    }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CollaborativeFilteringServiceImpl::class.java)

        /**
         * 분석할 최대 seed item 수
         */
        private const val MAX_SEED_ITEMS = 100

        /**
         * 각 seed item당 찾을 최대 유사 사용자 수
         */
        private const val MAX_SIMILAR_USERS_PER_ITEM = 50

        /**
         * 각 유사 사용자가 좋아한 콘텐츠 조회 개수
         */
        private const val MAX_ITEMS_PER_SIMILAR_USER = 20

        /**
         * 인터랙션 타입별 가중치
         */
        private const val LIKE_WEIGHT = 1.0
        private const val SAVE_WEIGHT = 1.5
        private const val SHARE_WEIGHT = 2.0
    }
}
