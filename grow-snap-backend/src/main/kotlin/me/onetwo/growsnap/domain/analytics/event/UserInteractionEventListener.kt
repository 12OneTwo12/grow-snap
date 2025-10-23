package me.onetwo.growsnap.domain.analytics.event

import me.onetwo.growsnap.domain.analytics.repository.UserContentInteractionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 사용자 인터랙션 이벤트 리스너
 *
 * UserInteractionEvent를 비동기로 처리하여
 * user_content_interactions 테이블에 저장합니다.
 *
 * ## Spring Event 패턴 (Best Practice)
 *
 * ### @TransactionalEventListener의 역할
 * - **TransactionPhase.AFTER_COMMIT**: 메인 트랜잭션 커밋 후에 이벤트 처리
 * - 메인 트랜잭션 실패 시 이벤트가 발행되지 않음 (데이터 일관성 보장)
 *
 * ### @Async의 역할
 * - 이벤트 처리를 별도 스레드에서 비동기로 실행
 * - 메인 요청의 응답 시간에 영향을 주지 않음
 *
 * ### 장점
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
 * 5. 트랜잭션 커밋 ✅
 * 6. [비동기] UserInteractionEventListener.handleUserInteractionEvent() 실행
 * 7. [비동기] user_content_interactions 저장
 * ```
 *
 * ### 에러 처리
 * - user_content_interactions 저장 실패 시 로그만 남기고 예외를 전파하지 않음
 * - 메인 요청은 이미 성공했으므로 사용자에게 에러를 노출하지 않음
 *
 * @property userContentInteractionRepository 사용자별 콘텐츠 인터랙션 레포지토리
 */
@Component
class UserInteractionEventListener(
    private val userContentInteractionRepository: UserContentInteractionRepository
) {

    /**
     * 사용자 인터랙션 이벤트 처리
     *
     * 메인 트랜잭션 커밋 후 비동기로 user_content_interactions 테이블에 저장합니다.
     *
     * ### TransactionPhase.AFTER_COMMIT
     * - 메인 트랜잭션이 성공적으로 커밋된 후에만 실행
     * - 메인 트랜잭션 실패 시 이벤트가 발행되지 않음
     *
     * ### Async 처리
     * - 별도 스레드에서 실행되어 메인 응답 시간에 영향을 주지 않음
     *
     * @param event 사용자 인터랙션 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    fun handleUserInteractionEvent(event: UserInteractionEvent) {
        logger.debug(
            "Handling UserInteractionEvent: userId={}, contentId={}, type={}",
            event.userId,
            event.contentId,
            event.interactionType
        )

        userContentInteractionRepository.save(
            event.userId,
            event.contentId,
            event.interactionType
        ).subscribe(
            {
                logger.debug(
                    "User interaction saved successfully: userId={}, contentId={}, type={}",
                    event.userId,
                    event.contentId,
                    event.interactionType
                )
            },
            { error ->
                logger.error(
                    "Failed to save user interaction: userId={}, contentId={}, type={}",
                    event.userId,
                    event.contentId,
                    event.interactionType,
                    error
                )
            }
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserInteractionEventListener::class.java)
    }
}
