# GrowSnap Backend 개발 원칙 (Claude용)

> Claude가 grow-snap-backend 개발 시 반드시 준수해야 할 원칙

## 🎯 핵심 원칙 (절대 준수)

### 1. TDD 필수
- **항상 테스트 코드를 먼저 작성**
- 순서: Red (실패하는 테스트) → Green (통과하는 최소 코드) → Refactor (리팩토링)
- 모든 public 메서드는 테스트 필수

### 2. SOLID 원칙
- **S**ingle Responsibility: 한 클래스는 한 가지 책임만
- **O**pen-Closed: 확장에 열려있고, 수정에 닫혀있게
- **L**iskov Substitution: 인터페이스 구현체는 상호 교체 가능하게
- **I**nterface Segregation: 큰 인터페이스를 작은 것들로 분리
- **D**ependency Inversion: 구현체가 아닌 추상화에 의존

### 3. 문서화 필수
- **모든 public 클래스/함수에 KDoc 작성**
- **모든 API 엔드포인트에 REST Docs 작성**
- AsciiDoc 자동 생성 확인

### 4. 테스트
- **단위 테스트는 MockK로 모킹 필수**
- **DisplayName은 시나리오 기반으로 작성** (예: "유효한 요청으로 비디오 생성 시, 201과 비디오 정보를 반환한다")
- Given-When-Then 패턴 준수

### 5. Git Convention
- 커밋 메시지: `feat(video): Add video upload API`
- 브랜치: `feature/video-upload`, `fix/auth-bug`

---

## 📂 프로젝트 구조 (MVC 패턴)

```
grow-snap-backend/
└── src/main/kotlin/me/onetwo/growsnap/
    ├── domain/
    │   ├── video/
    │   │   ├── controller/       # VideoController.kt
    │   │   ├── service/          # VideoService.kt, VideoServiceImpl.kt
    │   │   ├── repository/       # VideoRepository.kt, VideoRepositoryImpl.kt
    │   │   ├── model/            # Video.kt (엔티티)
    │   │   ├── dto/              # Request/Response DTO
    │   │   └── exception/        # VideoException.kt
    │   ├── user/
    │   └── auth/
    └── infrastructure/
        ├── config/               # 설정
        ├── security/             # 보안
        └── redis/                # Redis
```

### 계층별 역할

**Controller**: HTTP 요청/응답, Bean Validation, DTO 변환
**Service**: 비즈니스 로직, 트랜잭션 관리
**Repository**: 데이터베이스 CRUD (JOOQ 사용)
**Model**: 도메인 엔티티

---

## 🔄 개발 프로세스 (항상 이 순서로)

```
1. 📝 테스트 코드 작성 (Controller + Service)
   ↓
2. ✅ 테스트 통과하는 최소 코드 작성
   ↓
3. 🔧 리팩토링 (SOLID 원칙 적용)
   ↓
4. 📚 KDoc + REST Docs 작성
   ↓
5. ✨ 커밋 (feat(scope): message)
```

---

## ✅ 테스트 작성 규칙

### Controller 테스트 템플릿

```kotlin
@WebFluxTest(VideoController::class)
@Import(SecurityConfig::class, RestDocsConfiguration::class)
@AutoConfigureRestDocs
@DisplayName("비디오 컨트롤러 테스트")
class VideoControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var videoService: VideoService

    @Nested
    @DisplayName("POST /api/v1/videos - 비디오 생성")
    inner class CreateVideo {

        @Test
        @DisplayName("유효한 요청으로 생성 시, 201 Created와 비디오 정보를 반환한다")
        fun createVideo_WithValidRequest_ReturnsCreatedVideo() {
            // Given: 테스트 데이터 준비
            val request = VideoCreateRequest(/* ... */)
            val expected = VideoResponse(/* ... */)
            every { videoService.createVideo(any()) } returns Mono.just(expected)

            // When & Then: API 호출 및 검증
            webTestClient.post()
                .uri("/api/v1/videos")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated
                .expectBody<VideoResponse>()
                .isEqualTo(expected)
                .consumeWith(
                    document("video-create",
                        requestFields(/* ... */),
                        responseFields(/* ... */)
                    )
                )

            verify(exactly = 1) { videoService.createVideo(request) }
        }

        @Test
        @DisplayName("제목이 비어있는 경우, 400 Bad Request를 반환한다")
        fun createVideo_WithEmptyTitle_ReturnsBadRequest() {
            // Given: 잘못된 요청
            // When & Then: 400 응답 검증
        }
    }
}
```

### Service 테스트 템플릿

```kotlin
@ExtendWith(MockKExtension::class)
@DisplayName("비디오 서비스 테스트")
class VideoServiceImplTest {

    @MockK
    private lateinit var videoRepository: VideoRepository

    @MockK
    private lateinit var s3Service: S3Service

    @InjectMockKs
    private lateinit var videoService: VideoServiceImpl

    @Nested
    @DisplayName("createVideo - 비디오 생성")
    inner class CreateVideo {

        @Test
        @DisplayName("유효한 요청으로 생성 시, 비디오를 저장하고 응답을 반환한다")
        fun createVideo_WithValidRequest_SavesAndReturnsVideo() {
            // Given: 테스트 데이터
            val request = VideoCreateRequest(/* ... */)
            val savedVideo = Video(/* ... */)
            every { videoRepository.save(any()) } returns Mono.just(savedVideo)
            every { s3Service.generateUploadUrl(any()) } returns Mono.just("url")

            // When: 메서드 실행
            val result = videoService.createVideo(request)

            // Then: 결과 검증
            StepVerifier.create(result)
                .assertNext { response ->
                    assertThat(response.id).isEqualTo(savedVideo.id)
                    assertThat(response.title).isEqualTo(savedVideo.title)
                }
                .verifyComplete()

            verify(exactly = 1) { videoRepository.save(any()) }
            verify(exactly = 1) { s3Service.generateUploadUrl(any()) }
        }

        @Test
        @DisplayName("저장 실패 시, VideoCreationException을 발생시킨다")
        fun createVideo_WhenSaveFails_ThrowsException() {
            // Given: 저장 실패 상황
            every { videoRepository.save(any()) } returns
                Mono.error(RuntimeException("DB error"))

            // When: 메서드 실행
            val result = videoService.createVideo(request)

            // Then: 예외 검증
            StepVerifier.create(result)
                .expectError(VideoException.VideoCreationException::class.java)
                .verify()
        }
    }
}
```

---

## 📝 KDoc 작성 규칙

### 클래스 KDoc 템플릿

```kotlin
/**
 * 비디오 관련 비즈니스 로직을 처리하는 서비스
 *
 * 비디오 생성, 조회, 수정, 삭제 및 S3 업로드 URL 생성 기능을 제공합니다.
 *
 * @property videoRepository 비디오 데이터베이스 액세스를 위한 레포지토리
 * @property s3Service S3 파일 업로드 URL 생성 서비스
 * @since 1.0.0
 */
@Service
class VideoServiceImpl(
    private val videoRepository: VideoRepository,
    private val s3Service: S3Service
) : VideoService {
    // ...
}
```

### 함수 KDoc 템플릿

```kotlin
/**
 * 새로운 비디오를 생성합니다.
 *
 * 비디오 메타데이터를 데이터베이스에 저장하고,
 * S3 업로드를 위한 Presigned URL을 생성하여 반환합니다.
 *
 * ### 처리 흐름
 * 1. 비디오 엔티티 생성 및 저장
 * 2. S3 Presigned URL 생성
 * 3. 비디오 응답 반환
 *
 * @param request 비디오 생성 요청 정보
 * @return 생성된 비디오 정보를 담은 Mono
 * @throws VideoException.VideoCreationException 비디오 생성 실패 시
 */
@Transactional
override fun createVideo(request: VideoCreateRequest): Mono<VideoResponse> {
    logger.info("Creating video: title=${request.title}")

    return videoRepository.save(request.toEntity())
        .flatMap { saved ->
            s3Service.generateUploadUrl(saved.id)
                .map { saved.toResponse(it) }
        }
        .doOnSuccess { logger.info("Video created: id=${it.id}") }
        .doOnError { logger.error("Failed to create video", it) }
        .onErrorMap { VideoException.VideoCreationException(it.message) }
}
```

---

## 🌐 REST API 설계 규칙

### URL 패턴

```
✅ 올바른 예시
GET    /api/v1/videos              # 목록 조회
POST   /api/v1/videos              # 생성
GET    /api/v1/videos/{id}         # 상세 조회
PUT    /api/v1/videos/{id}         # 전체 수정
PATCH  /api/v1/videos/{id}         # 부분 수정
DELETE /api/v1/videos/{id}         # 삭제
POST   /api/v1/videos/{id}/like    # 좋아요 (액션)

❌ 잘못된 예시
GET    /api/v1/getAllVideos        # 동사 사용 금지
POST   /api/v1/createVideo         # 동사 사용 금지
GET    /api/v1/videos/get/{id}     # 불필요한 동사
```

### HTTP 상태 코드

```kotlin
200 OK              // 조회, 수정 성공
201 Created         // 생성 성공
204 No Content      // 삭제 성공
400 Bad Request     // 잘못된 요청
401 Unauthorized    // 인증 실패
403 Forbidden       // 권한 없음
404 Not Found       // 리소스 없음
409 Conflict        // 중복, 충돌
500 Internal Server Error  // 서버 오류
```

### 응답 형식

```kotlin
// 성공 응답
data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T,
    val timestamp: Instant = Instant.now()
)

// 에러 응답
data class ErrorResponse(
    val success: Boolean = false,
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val details: Map<String, Any>? = null
)

// 페이지네이션
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
)
```

---

## 📚 REST Docs 작성 (필수)

### 모든 API 테스트에 document() 추가

```kotlin
@Test
@DisplayName("비디오 목록 조회 시, 페이지네이션된 목록을 반환한다")
fun getVideos_ReturnsPagedList() {
    // Given, When, Then...

    webTestClient.get()
        .uri("/api/v1/videos?page=0&size=10")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .consumeWith(
            document("videos-list",
                queryParameters(
                    parameterWithName("page")
                        .description("페이지 번호 (0부터 시작)")
                        .optional(),
                    parameterWithName("size")
                        .description("페이지 크기 (기본값: 10)")
                        .optional()
                ),
                responseFields(
                    fieldWithPath("content[]").description("비디오 목록"),
                    fieldWithPath("content[].id").description("비디오 ID"),
                    fieldWithPath("content[].title").description("제목"),
                    // ... 모든 필드 문서화
                )
            )
        )
}
```

---

## 🔧 SOLID 원칙 적용 예시

### Single Responsibility (단일 책임)

```kotlin
// ✅ Good: 각 클래스가 하나의 책임만
class VideoService(
    private val videoRepository: VideoRepository,
    private val videoUploadService: VideoUploadService,
    private val notificationService: NotificationService
) {
    fun createVideo(request: VideoCreateRequest): Mono<VideoResponse> {
        return videoRepository.save(request.toEntity())
            .flatMap { video ->
                videoUploadService.generateUploadUrl(video.id)
                    .doOnSuccess { notificationService.notifyVideoCreated(video) }
                    .map { video.toResponse(it) }
            }
    }
}

// ❌ Bad: 한 클래스가 너무 많은 책임
class VideoService {
    fun createVideo() { }
    fun uploadToS3() { }
    fun sendNotification() { }
    fun analyzeVideo() { }
}
```

### Open-Closed (개방-폐쇄)

```kotlin
// ✅ Good: 새로운 전략 추가 시 기존 코드 수정 불필요
interface VideoProcessingStrategy {
    fun process(video: Video): Mono<ProcessedVideo>
}

class CompressionStrategy : VideoProcessingStrategy { }
class WatermarkStrategy : VideoProcessingStrategy { }

class VideoProcessor(private val strategies: List<VideoProcessingStrategy>) {
    fun process(video: Video): Mono<ProcessedVideo> {
        return strategies.fold(Mono.just(video as ProcessedVideo)) { acc, strategy ->
            acc.flatMap { strategy.process(it) }
        }
    }
}
```

### Dependency Inversion (의존성 역전)

```kotlin
// ✅ Good: 인터페이스에 의존
class VideoService(
    private val videoRepository: VideoRepository,      // 인터페이스
    private val fileStorage: FileStorageService        // 인터페이스
) { }

// ❌ Bad: 구현체에 직접 의존
class VideoService {
    private val repository = JooqVideoRepository()  // 구현체
    private val s3Client = AmazonS3Client()        // 구현체
}
```

---

## 🎨 코드 작성 규칙

### 네이밍

```kotlin
// 클래스: PascalCase
class VideoService

// 함수/변수: camelCase
fun createVideo(request: VideoCreateRequest)
val videoTitle = "제목"

// 상수: UPPER_SNAKE_CASE
const val MAX_VIDEO_SIZE = 100_000_000

// 패키지: lowercase
package me.onetwo.growsnap.domain.video
```

### Kotlin 특성 활용

```kotlin
// ✅ data class 활용
data class VideoResponse(val id: String, val title: String)

// ✅ null 안전성
fun getVideo(id: String): Video? = videoRepository.findById(id).orElse(null)
val title = video?.title ?: "기본 제목"

// ✅ 확장 함수
fun Video.toResponse(): VideoResponse = VideoResponse(id, title)

// ✅ when 표현식
val type = when (category) {
    VideoCategory.PROGRAMMING -> "기술"
    VideoCategory.LIFESTYLE -> "라이프스타일"
    else -> "기타"
}
```

### WebFlux Reactive 패턴

```kotlin
// ✅ Good: Reactive 체인
fun getVideoFeed(userId: String): Flux<VideoDto> {
    return userRepository.findById(userId)
        .flatMapMany { user ->
            videoRepository.findRecommendedVideos(user.interests)
        }
        .map { it.toDto() }
}

// ❌ Bad: 블로킹 호출
fun getVideoFeed(userId: String): List<VideoDto> {
    val user = userRepository.findById(userId).block()!!  // 블로킹!
    return videoRepository.findAll().collectList().block()!!
}

// ✅ Good: 배압 제어
fun processVideos(): Flux<ProcessedVideo> {
    return videoRepository.findAll()
        .limitRate(100)  // 한 번에 100개씩
        .flatMap { video -> videoProcessor.process(video) }
}
```

---

## 📦 Git Convention

### 커밋 메시지 형식

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type
- `feat`: 새로운 기능
- `fix`: 버그 수정
- `test`: 테스트 추가/수정
- `refactor`: 리팩토링
- `docs`: 문서 수정
- `chore`: 빌드 설정

### 예시

```bash
feat(video): Add video upload API

비디오 업로드 기능 구현
- S3 Presigned URL 생성
- 비디오 메타데이터 저장
- 업로드 완료 알림

Closes #123
```

### 브랜치 네이밍

```
feature/video-upload
fix/auth-token-bug
refactor/video-service
test/video-controller
```

---

## ✔️ 코드 리뷰 체크리스트

### Claude가 PR 전에 반드시 확인할 항목

- [ ] **TDD**: 테스트 코드를 먼저 작성했는가?
- [ ] **테스트 통과**: 모든 테스트가 통과하는가?
- [ ] **모킹**: 단위 테스트에서 MockK를 사용했는가?
- [ ] **DisplayName**: 시나리오 기반으로 작성했는가?
- [ ] **KDoc**: 모든 public 함수/클래스에 작성했는가?
- [ ] **REST Docs**: 모든 API에 document()를 추가했는가?
- [ ] **SOLID**: 단일 책임 원칙을 지켰는가?
- [ ] **RESTful**: URL 설계가 RESTful한가?
- [ ] **HTTP 상태 코드**: 올바르게 사용했는가?
- [ ] **커밋 메시지**: Convention을 따랐는가?
- [ ] **성능**: N+1 문제는 없는가?
- [ ] **가독성**: 코드를 이해하기 쉬운가?

---

## 🚀 개발 시작 전 체크리스트

### 새로운 기능 개발 시

1. [ ] 요구사항 명확히 파악
2. [ ] API 설계 (URL, HTTP 메서드, 상태 코드)
3. [ ] DTO 설계 (Request, Response)
4. [ ] 테스트 시나리오 작성
5. [ ] **Controller 테스트 먼저 작성**
6. [ ] **Service 테스트 먼저 작성**
7. [ ] 테스트 통과하는 최소 코드 작성
8. [ ] 리팩토링 (SOLID 적용)
9. [ ] KDoc 작성
10. [ ] REST Docs 확인
11. [ ] 커밋

---

## 📌 자주 사용하는 패턴

### Repository 구현 (JOOQ)

```kotlin
@Repository
class VideoRepositoryImpl(
    private val dslContext: DSLContext
) : VideoRepository {

    override fun findById(id: String): Mono<Video> {
        return Mono.fromCallable {
            dslContext
                .select(VIDEO.asterisk())
                .from(VIDEO)
                .where(VIDEO.ID.eq(id))
                .fetchOneInto(Video::class.java)
        }
    }

    override fun save(video: Video): Mono<Video> {
        return Mono.fromCallable {
            dslContext.insertInto(VIDEO)
                .set(VIDEO.ID, video.id)
                .set(VIDEO.TITLE, video.title)
                .execute()
            video
        }
    }
}
```

### 예외 처리

```kotlin
// 도메인 예외 정의
sealed class VideoException(message: String) : RuntimeException(message) {
    class VideoNotFoundException(id: String) :
        VideoException("Video not found: $id")
    class VideoCreationException(reason: String) :
        VideoException("Video creation failed: $reason")
}

// 전역 예외 핸들러
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(VideoException.VideoNotFoundException::class)
    fun handleVideoNotFound(ex: VideoException.VideoNotFoundException):
        ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("VIDEO_NOT_FOUND", ex.message))
    }
}
```

---

## ⚡ 성능 최적화

### 필요한 곳에만 최적화
- 프로파일링으로 병목 찾기
- 가독성을 희생하지 않는 선에서만

### WebFlux 최적화

```kotlin
// 배압 제어
fun processAll(): Flux<Result> {
    return repository.findAll()
        .limitRate(100)
        .flatMap { process(it) }
}

// 병렬 처리
fun processMultiple(ids: List<String>): Flux<Result> {
    return Flux.fromIterable(ids)
        .flatMap({ id -> process(id) }, 4)  // 동시에 4개
}
```

---

## 🎯 요약: Claude가 반드시 지킬 것

1. **TDD**: 테스트 → 구현 → 리팩토링
2. **SOLID**: 단일 책임, 인터페이스 분리, 의존성 역전
3. **KDoc**: 모든 public 함수/클래스
4. **REST Docs**: 모든 API
5. **DisplayName**: 시나리오 기반
6. **MockK**: 단위 테스트 모킹
7. **Git Convention**: `feat(scope): subject`
8. **MVC 패턴**: Controller → Service → Repository
9. **성능 vs 가독성**: 가독성 우선, 필요시 최적화
10. **RESTful API**: 동사 금지, 적절한 HTTP 메서드/상태 코드

---

**작성일**: 2024-10-22
**버전**: 1.0.0
**대상**: Claude (AI 개발 어시스턴트)
