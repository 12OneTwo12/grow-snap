# GrowSnap Backend Spring Event 패턴

> 비동기 이벤트 처리를 위한 Spring Event 패턴을 정의합니다.

## 개요

Spring Event는 애플리케이션 내에서 비동기 이벤트 기반 통신을 구현하는 패턴입니다.

### 언제 사용하는가?

- 메인 트랜잭션과 독립적으로 실행되어야 하는 작업
- 실패해도 메인 요청에 영향을 주지 않아야 하는 작업
- 여러 도메인 간 결합도를 낮추고 싶을 때

### GrowSnap에서의 사용 예시

**사용자가 콘텐츠에 좋아요를 누를 때**:

1. **메인 트랜잭션**: `content_interactions.like_count` 증가
2. **Spring Event 발행**: `UserInteractionEvent`
3. **비동기 처리**: `user_content_interactions` 테이블에 저장 (협업 필터링용)

## Spring Event 패턴 Best Practice

### 1. 이벤트 클래스 정의

```kotlin
/**
 * 사용자 인터랙션 이벤트
 *
 * 사용자의 콘텐츠 인터랙션 (LIKE, SAVE, SHARE)을 기록하기 위한 이벤트입니다.
 *
 * @property userId 사용자 ID
 * @property contentId 콘텐츠 ID
 * @property interactionType 인터랙션 타입 (LIKE, SAVE, SHARE)
 */
data class UserInteractionEvent(
    val userId: UUID,
    val contentId: UUID,
    val interactionType: InteractionType
)
```

**이벤트 클래스 규칙**:
- ✅ **data class 사용**: 불변 객체로 정의
- ✅ **필요한 최소 정보만 포함**: 과도한 정보 전달 금지
- ✅ **명확한 이름**: 이벤트 이름만 보고 무엇을 의미하는지 파악 가능
- ✅ **KDoc 작성**: 이벤트의 목적과 사용 시점 명시

### 2. 이벤트 발행자 (Publisher)

```kotlin
@Service
class AnalyticsServiceImpl(
    private val contentInteractionRepository: ContentInteractionRepository,
    private val applicationEventPublisher: ApplicationEventPublisher  // Spring 제공
) : AnalyticsService {

    override fun trackInteractionEvent(userId: UUID, request: InteractionEventRequest): Mono<Void> {
        val contentId = request.contentId!!
        val interactionType = request.interactionType!!

        // 1. 메인 트랜잭션: 카운터 증가
        val incrementCounter = when (interactionType) {
            InteractionType.LIKE -> contentInteractionRepository.incrementLikeCount(contentId)
            InteractionType.SAVE -> contentInteractionRepository.incrementSaveCount(contentId)
            InteractionType.SHARE -> contentInteractionRepository.incrementShareCount(contentId)
        }

        // 2. 이벤트 발행 (doOnSuccess: 메인 트랜잭션 성공 시에만 발행)
        return incrementCounter.doOnSuccess {
            logger.debug(
                "Publishing UserInteractionEvent: userId={}, contentId={}, type={}",
                userId,
                contentId,
                interactionType
            )
            applicationEventPublisher.publishEvent(
                UserInteractionEvent(
                    userId = userId,
                    contentId = contentId,
                    interactionType = interactionType
                )
            )
        }.then()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalyticsServiceImpl::class.java)
    }
}
```

**이벤트 발행 규칙**:
- ✅ **ApplicationEventPublisher 주입**: Spring이 제공하는 인터페이스 사용
- ✅ **메인 트랜잭션 성공 후 발행**: `doOnSuccess`로 성공 시에만 발행
- ✅ **로깅**: 이벤트 발행 시점에 DEBUG 로그 남기기
- ❌ **메인 트랜잭션 실패 시 발행 금지**: 실패 시 이벤트 발행하지 않음

### 3. 이벤트 리스너 (Listener)

```kotlin
@Component
class UserInteractionEventListener(
    private val userContentInteractionRepository: UserContentInteractionRepository
) {

    /**
     * 사용자 인터랙션 이벤트 처리
     *
     * ### 처리 흐름
     * 1. 메인 트랜잭션 커밋 후 실행 (@TransactionalEventListener + AFTER_COMMIT)
     * 2. 비동기로 실행 (@Async)
     * 3. user_content_interactions 테이블에 저장
     *
     * ### 장애 격리
     * - 이 메서드가 실패해도 메인 트랜잭션에 영향 없음
     * - 로그만 남기고 예외를 삼킴
     *
     * @param event 사용자 인터랙션 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleUserInteractionEvent(event: UserInteractionEvent) {
        try {
            logger.debug(
                "Handling UserInteractionEvent: userId={}, contentId={}, type={}",
                event.userId,
                event.contentId,
                event.interactionType
            )

            // user_content_interactions 테이블에 저장 (협업 필터링용)
            userContentInteractionRepository
                .saveInteraction(event.userId, event.contentId, event.interactionType)
                .subscribe(
                    { logger.debug("User interaction saved successfully") },
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
        } catch (e: Exception) {
            // 예외를 삼켜서 메인 트랜잭션에 영향을 주지 않음
            logger.error("Failed to handle UserInteractionEvent", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserInteractionEventListener::class.java)
    }
}
```

**이벤트 리스너 규칙**:
- ✅ **@TransactionalEventListener(AFTER_COMMIT)**: 메인 트랜잭션 커밋 후에만 실행
- ✅ **@Async**: 비동기로 실행하여 메인 요청에 영향 없음
- ✅ **try-catch**: 예외를 삼켜서 메인 트랜잭션에 영향을 주지 않음
- ✅ **로깅**: DEBUG 레벨로 이벤트 처리 시점 로깅, ERROR 레벨로 실패 로깅
- ✅ **KDoc 작성**: 처리 흐름과 장애 격리 방식 명시

### 4. Spring Async 설정

```kotlin
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurerSupport() {

    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 10
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("async-event-")
        executor.initialize()
        return executor
    }
}
```

**Async 설정 규칙**:
- ✅ **@EnableAsync**: 비동기 처리 활성화
- ✅ **ThreadPoolTaskExecutor**: 스레드 풀 설정
- ✅ **적절한 풀 사이즈**: 애플리케이션 부하에 맞게 조정
- ✅ **스레드 이름 접두사**: 로그 추적을 위해 명확한 이름 설정

## Spring Event 패턴 체크리스트

**이벤트 기반 처리 구현 시 반드시 확인**:

- [ ] **이벤트 클래스**: data class로 정의, 필요한 최소 정보만 포함
- [ ] **이벤트 발행**: `applicationEventPublisher.publishEvent()` 사용
- [ ] **발행 시점**: 메인 트랜잭션 성공 시에만 발행 (`doOnSuccess`)
- [ ] **이벤트 리스너**: `@TransactionalEventListener(phase = AFTER_COMMIT)` 사용
- [ ] **비동기 처리**: `@Async` 사용
- [ ] **장애 격리**: try-catch로 예외 삼킴, 로그만 남김
- [ ] **Spring Async 설정**: `@EnableAsync` + ThreadPoolTaskExecutor 설정
- [ ] **KDoc 작성**: 이벤트 목적, 처리 흐름, 장애 격리 방식 명시

## 주의사항

### 1. 메인 트랜잭션과 독립성 보장

**중요**: 이벤트 리스너 실패가 메인 요청에 영향을 주지 않도록 설계

```kotlin
// ✅ GOOD: 메인 트랜잭션 커밋 후 실행
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handleEvent(event: MyEvent) {
    // 이 시점에는 메인 트랜잭션이 이미 커밋됨
}

// ❌ BAD: 메인 트랜잭션과 같은 시점에 실행
@EventListener
fun handleEvent(event: MyEvent) {
    // 메인 트랜잭션 커밋 전에 실행되어 롤백 가능성
}
```

### 2. 멱등성(Idempotency) 고려

**중요**: 이벤트가 중복 발생할 수 있으므로 멱등성 보장 필요

```kotlin
// ✅ GOOD: UNIQUE 제약 조건 설정
CREATE TABLE user_content_interactions (
    user_id CHAR(36) NOT NULL,
    content_id CHAR(36) NOT NULL,
    interaction_type VARCHAR(20) NOT NULL,
    UNIQUE KEY unique_interaction (user_id, content_id, interaction_type)
);

// ✅ GOOD: INSERT ... ON DUPLICATE KEY UPDATE
fun saveInteraction(userId: UUID, contentId: UUID, type: InteractionType) {
    dslContext
        .insertInto(USER_CONTENT_INTERACTIONS)
        .set(USER_CONTENT_INTERACTIONS.USER_ID, userId.toString())
        .set(USER_CONTENT_INTERACTIONS.CONTENT_ID, contentId.toString())
        .set(USER_CONTENT_INTERACTIONS.INTERACTION_TYPE, type.name)
        .onDuplicateKeyUpdate()
        .set(USER_CONTENT_INTERACTIONS.UPDATED_AT, LocalDateTime.now())
        .execute()
}
```

### 3. 로깅 충실

**중요**: 이벤트 발행/처리 시점에 DEBUG 로그 남기기, 실패 시 ERROR 로그로 추적 가능하도록

```kotlin
// ✅ GOOD: 충실한 로깅
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handleEvent(event: UserInteractionEvent) {
    try {
        logger.debug(
            "Handling event: userId={}, contentId={}, type={}",
            event.userId,
            event.contentId,
            event.interactionType
        )

        // 이벤트 처리 로직

        logger.debug("Event handled successfully")
    } catch (e: Exception) {
        logger.error(
            "Failed to handle event: userId={}, contentId={}, type={}",
            event.userId,
            event.contentId,
            event.interactionType,
            e
        )
    }
}
```

## 전체 흐름 예시

### 1. 좋아요 이벤트 발행

```kotlin
@Service
class AnalyticsServiceImpl(
    private val contentInteractionRepository: ContentInteractionRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) : AnalyticsService {

    override fun trackLike(userId: UUID, contentId: UUID): Mono<Void> {
        // 1. 메인 트랜잭션: like_count 증가
        return contentInteractionRepository.incrementLikeCount(contentId)
            .doOnSuccess {
                // 2. 이벤트 발행 (메인 트랜잭션 성공 후)
                logger.debug("Publishing LikeEvent: userId={}, contentId={}", userId, contentId)
                applicationEventPublisher.publishEvent(
                    UserInteractionEvent(userId, contentId, InteractionType.LIKE)
                )
            }
            .then()
    }
}
```

### 2. 이벤트 리스너에서 비동기 처리

```kotlin
@Component
class UserInteractionEventListener(
    private val userContentInteractionRepository: UserContentInteractionRepository,
    private val recommendationService: RecommendationService
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleUserInteractionEvent(event: UserInteractionEvent) {
        try {
            logger.debug("Handling UserInteractionEvent: {}", event)

            // 1. user_content_interactions 테이블에 저장
            userContentInteractionRepository
                .saveInteraction(event.userId, event.contentId, event.interactionType)
                .subscribe()

            // 2. 추천 시스템 업데이트 (선택적)
            recommendationService.updateUserPreference(event.userId, event.contentId)

            logger.debug("Event handled successfully")
        } catch (e: Exception) {
            logger.error("Failed to handle event", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserInteractionEventListener::class.java)
    }
}
```

## 정리

### Spring Event 패턴 핵심

1. **이벤트 클래스**: data class로 정의, 필요한 최소 정보만 포함
2. **이벤트 발행**: `applicationEventPublisher.publishEvent()` + `doOnSuccess`
3. **이벤트 리스너**: `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`
4. **장애 격리**: try-catch로 예외 삼킴, 메인 트랜잭션과 독립성 보장
5. **멱등성**: 중복 이벤트 처리 대비
6. **로깅**: DEBUG/ERROR 레벨로 충실한 로깅

### 언제 사용하는가?

- ✅ **메인 트랜잭션과 독립적인 작업**: 이메일 발송, 알림 전송, 통계 업데이트
- ✅ **실패해도 메인 요청에 영향 없는 작업**: 추천 시스템 업데이트, 로그 저장
- ✅ **도메인 간 결합도 낮추기**: 주문 완료 후 재고 업데이트, 포인트 적립
- ❌ **메인 트랜잭션에 영향을 주는 작업**: 결제 처리, 재고 차감 (동기 처리 필요)
