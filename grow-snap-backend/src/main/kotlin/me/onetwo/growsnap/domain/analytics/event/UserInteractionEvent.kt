package me.onetwo.growsnap.domain.analytics.event

import me.onetwo.growsnap.domain.analytics.dto.InteractionType
import java.time.LocalDateTime
import java.util.UUID

/**
 * 사용자 인터랙션 이벤트
 *
 * 사용자가 콘텐츠에 대해 인터랙션(좋아요, 저장, 공유)을 수행했을 때 발생하는 이벤트입니다.
 * 이 이벤트는 메인 트랜잭션 커밋 후 비동기적으로 처리되어
 * user_content_interactions 테이블에 저장됩니다.
 *
 * ## 설계 이유
 *
 * ### Spring Event 패턴 사용 이유
 * 1. **성능 향상**: 메인 응답 시간 단축 (비동기 처리)
 * 2. **낮은 결합도**: Analytics 로직과 협업 필터링 로직 분리
 * 3. **장애 격리**: user_content_interactions 저장 실패해도 메인 요청 성공
 * 4. **확장성**: 나중에 다른 이벤트 리스너 추가 가능 (예: 알림, 통계)
 *
 * ### 처리 흐름
 * ```
 * 1. 사용자가 좋아요 클릭
 * 2. AnalyticsService.trackInteractionEvent() 호출
 * 3. content_interactions 테이블의 like_count 증가 (메인 트랜잭션)
 * 4. UserInteractionEvent 발행
 * 5. 트랜잭션 커밋
 * 6. UserInteractionEventListener가 비동기로 user_content_interactions 저장
 * ```
 *
 * @property userId 사용자 ID
 * @property contentId 콘텐츠 ID
 * @property interactionType 인터랙션 타입 (LIKE, SAVE, SHARE)
 * @property timestamp 이벤트 발생 시각
 */
data class UserInteractionEvent(
    val userId: UUID,
    val contentId: UUID,
    val interactionType: InteractionType,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
