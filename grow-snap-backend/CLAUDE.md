# GrowSnap Backend 개발 원칙 (Claude용)

> Claude가 grow-snap-backend 개발 시 반드시 준수해야 할 원칙

## 🎯 핵심 원칙 (절대 준수)

### 1. TDD 필수
- **항상 테스트 코드를 먼저 작성**
- 순서: Red (실패하는 테스트) → Green (통과하는 최소 코드) → Refactor (리팩토링)
- 모든 public 메서드는 테스트 필수
- **모든 테스트는 시나리오 기반으로 작성**: 테스트만 보고 즉시 기능을 파악할 수 있어야 함
- **Given-When-Then 주석 필수**: 모든 테스트에 `// Given`, `// When`, `// Then` 주석 작성
- **DisplayName 필수**: 테스트 시나리오를 명확히 설명하는 한글 설명 (예: "유효한 요청으로 비디오 생성 시, 201과 비디오 정보를 반환한다")

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

### 4. 테스트 작성 규칙
- **단위 테스트는 MockK로 모킹 필수**
- **시나리오 기반 테스트**: 테스트 이름만 보고 무엇을 검증하는지 즉시 파악 가능해야 함
- **Given-When-Then 주석 필수**: 모든 테스트에 명시적으로 작성
- **DisplayName 필수**: 한글로 명확한 시나리오 설명 (예: "유효한 요청으로 비디오 생성 시, 201과 비디오 정보를 반환한다")
- **테스트 완료 후 빌드/테스트 실행**: 모든 테스트가 통과해야만 작업 완료
- **통합, 단위 테스트 모두 작성할 것 ( 비중은 단위 테스트: 70%, 통합 테스트: 30%)**

### 5. Git Convention
- 커밋 : /docs/GIT_CONVENTION.md 준수

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

### 서비스 간 의존성 패턴 (Service-to-Service Dependency)

#### ✅ 허용되는 패턴 (Best Practice)

서비스에서 다른 서비스를 의존하는 것은 안티패턴은 아닙니다

```kotlin
// ✅ GOOD: 단방향 의존성
@Service
class UserProfileServiceImpl(
    private val userProfileRepository: UserProfileRepository,
    private val userService: UserService  // OK! 다른 서비스 의존
) : UserProfileService {

    override fun getProfile(userId: UUID): Mono<UserProfileResponse> {
        // 다른 서비스 호출은 문제없음
        return userService.findById(userId)
            .flatMap { user ->
                userProfileRepository.findByUserId(userId)
                    .map { profile -> profile.toResponse(user) }
            }
    }
}
```

#### ⚠️ 주의사항

**1. 순환 의존성 금지 (Circular Dependency)**

```kotlin
// ❌ BAD: 순환 의존성 발생
class UserService(private val profileService: ProfileService)
class ProfileService(private val userService: UserService)  // 순환!
```

**2. 복잡도 관리 - Facade 패턴 활용**

```kotlin
// ⚠️ 3개 이상의 서비스 의존 시 Facade 패턴 고려
@Service
class UserProfileFacade(
    private val userService: UserService,
    private val profileService: UserProfileService,
    private val imageService: ImageUploadService,
    private val followService: FollowService
) {
    /**
     * 여러 서비스를 조율하는 복잡한 로직은 Facade에서 처리
     */
    fun createCompleteProfile(request: CreateProfileRequest): Mono<CompleteProfileResponse> {
        return userService.create(request.user)
            .flatMap { user ->
                profileService.create(user.id, request.profile)
                    .flatMap { profile ->
                        imageService.upload(user.id, request.image)
                            .map { CompleteProfileResponse(user, profile, it) }
                    }
            }
    }
}
```

#### 📋 서비스 의존성 체크리스트

- [ ] **단방향 의존성**: A → B는 허용, 하지만 B → A는 금지 (순환 의존 방지)
- [ ] **의존 이유 명확**: 왜 이 서비스가 필요한가? 책임이 명확한가?
- [ ] **복잡도 관리**: 3개 이상 서비스 의존 시 Facade/Orchestration 패턴 고려
- [ ] **SRP 준수**: 각 서비스의 단일 책임 원칙이 지켜지는가?
- [ ] **테스트 가능성**: 의존성 때문에 테스트가 어려워지지 않는가?

---

## 🔄 개발 프로세스 (항상 이 순서로)

```
1. 📝 테스트 코드 작성 (Controller + Service)
   ↓
2. ✅ 테스트 통과하는 최소 코드 작성 (SOLID 원칙 준수)
   ↓
3. 🔧 리팩토링 (SOLID 원칙 적용)
   ↓
4. 📚 KDoc + REST Docs 작성
   ↓
5. 📚 빌드 및 테스트 ( 모두 정상이여야함, 일부 실패 용인하지 않음 )
   ↓
6. ✨ 커밋 (feat(scope): message)
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

```
org.springframework.http.ResponseEntity 사용할것
```

### WebFlux Controller 반환 타입 패턴

**원칙: 일관성 있게 `Mono<ResponseEntity<T>>` 패턴 사용**

#### 1. `Mono<ResponseEntity<T>>` - **권장 패턴** ✅

**사용 시기**: 대부분의 경우 (기본 패턴)

- HTTP 상태 코드, 헤더, 바디 모두를 비동기적으로 결정
- 비동기 처리 결과에 따라 상태 코드를 다르게 반환 가능
- 에러 핸들링이 유연함

```kotlin
@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {

    /**
     * ✅ GOOD: Mono<ResponseEntity<T>> 패턴
     *
     * 비동기 처리 결과에 따라 상태 코드를 다르게 반환 가능
     */
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID): Mono<ResponseEntity<UserResponse>> {
        return userService.findById(id)
            .map { user -> ResponseEntity.ok(user) }              // 200 OK
            .defaultIfEmpty(ResponseEntity.notFound().build())     // 404 Not Found
    }

    @PostMapping
    fun createUser(@RequestBody request: UserCreateRequest): Mono<ResponseEntity<UserResponse>> {
        return userService.create(request)
            .map { user -> ResponseEntity.status(HttpStatus.CREATED).body(user) }  // 201 Created
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: UUID): Mono<ResponseEntity<Void>> {
        return userService.delete(id)
            .then(Mono.just(ResponseEntity.noContent().build<Void>()))  // 204 No Content
    }
}
```

#### 2. `ResponseEntity<Mono<T>>` - **제한적 사용** ⚠️

**사용 시기**: 상태 코드와 헤더를 즉시 결정할 수 있고, 바디만 비동기 처리

- 상태 코드와 헤더가 미리 확정됨
- 바디 데이터만 비동기로 제공
- **대부분의 경우 `Mono<ResponseEntity<T>>`가 더 적합**

```kotlin
// ⚠️ 제한적 사용: 상태 코드가 항상 200 OK로 확정된 경우
@GetMapping("/stats")
fun getStats(): ResponseEntity<Mono<StatsResponse>> {
    // 상태 코드는 즉시 200 OK로 결정, 바디만 비동기 처리
    return ResponseEntity.ok(userService.calculateStats())
}
```

#### 3. `Mono<T>` - **간단한 경우** ⚠️

**사용 시기**: 항상 200 OK를 반환하는 단순한 경우

- HTTP 상태 코드를 커스터마이즈할 필요가 없을 때
- Spring WebFlux가 자동으로 200 OK 반환
- **하지만 명시적으로 `Mono<ResponseEntity<T>>`를 사용하는 것이 더 명확함**

```kotlin
// ⚠️ 간단하지만 명시적이지 않음
@GetMapping("/simple")
fun getSimple(): Mono<UserResponse> {
    return userService.findById(userId)  // 자동으로 200 OK
}

// ✅ BETTER: 명시적으로 상태 코드 지정
@GetMapping("/simple")
fun getSimple(): Mono<ResponseEntity<UserResponse>> {
    return userService.findById(userId)
        .map { ResponseEntity.ok(it) }  // 명시적으로 200 OK
}
```

#### 4. `Flux<T>` vs `Mono<ResponseEntity<Flux<T>>>` - **스트리밍**

**스트리밍 응답 (Server-Sent Events, Streaming JSON)**

```kotlin
// ✅ GOOD: 스트리밍 응답 (여러 개의 데이터를 순차적으로 전송)
@GetMapping(value = ["/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
fun streamUsers(): Flux<UserResponse> {
    return userService.findAll()  // 스트리밍으로 전송
}

// ✅ GOOD: 컬렉션을 한 번에 반환
@GetMapping("/all")
fun getAllUsers(): Mono<ResponseEntity<List<UserResponse>>> {
    return userService.findAll()
        .collectList()
        .map { ResponseEntity.ok(it) }
}
```

#### 📋 WebFlux Controller 반환 타입 선택 가이드

| 상황 | 권장 반환 타입 | 이유 |
|------|--------------|------|
| **일반적인 CRUD API** | `Mono<ResponseEntity<T>>` | 상태 코드, 헤더, 바디 모두 비동기 결정 |
| **조건부 상태 코드** (200/404) | `Mono<ResponseEntity<T>>` | `defaultIfEmpty()`로 404 처리 |
| **리스트 반환** | `Mono<ResponseEntity<List<T>>>` | `collectList()`로 변환 후 반환 |
| **스트리밍 응답** (SSE) | `Flux<T>` | Server-Sent Events 스트리밍 |
| **삭제 API** (바디 없음) | `Mono<ResponseEntity<Void>>` | 204 No Content |
| **생성 API** | `Mono<ResponseEntity<T>>` | 201 Created |

#### ❌ 피해야 할 패턴

```kotlin
// ❌ BAD: 블로킹 호출
@GetMapping("/{id}")
fun getUser(@PathVariable id: UUID): ResponseEntity<UserResponse> {
    val user = userService.findById(id).block()!!  // 블로킹!
    return ResponseEntity.ok(user)
}

// ❌ BAD: ResponseEntity를 Mono로 감싸지 않음 (비일관성)
@GetMapping("/inconsistent")
fun inconsistentReturn(): UserResponse {
    // 상태 코드 제어 불가
}
```

#### 💡 정리

1. **기본 패턴**: `Mono<ResponseEntity<T>>` 사용 (가장 유연함)
2. **상태 코드 제어**: `.map { ResponseEntity.status(...).body(it) }`
3. **404 처리**: `.defaultIfEmpty(ResponseEntity.notFound().build())`
4. **스트리밍**: `Flux<T>`만 사용 (SSE, Streaming JSON)
5. **일관성 유지**: 프로젝트 전체에서 동일한 패턴 사용

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

## ✔️ 코드 리뷰 체크리스트

### Claude가 PR 전에 반드시 확인할 항목

- [ ] **TDD**: 테스트 코드를 먼저 작성했는가?
- [ ] **테스트 통과**: 모든 테스트가 통과하는가?
- [ ] **빌드 성공**: ./gradlew build가 성공하는가?
- [ ] **모킹**: 단위 테스트에서 MockK를 사용했는가?
- [ ] **시나리오 기반**: 테스트만 보고 기능을 즉시 파악할 수 있는가?
- [ ] **Given-When-Then**: 모든 테스트에 주석을 작성했는가?
- [ ] **DisplayName**: 시나리오를 명확히 설명하는 한글 설명을 작성했는가?
- [ ] **KDoc**: 모든 public 함수/클래스에 작성했는가?
- [ ] **REST Docs**: 모든 API에 document()를 추가했는가?
- [ ] **SOLID**: 단일 책임 원칙을 지켰는가?
- [ ] **RESTful**: URL 설계가 RESTful한가?
- [ ] **HTTP 상태 코드**: 올바르게 사용했는가?
- [ ] **커밋 메시지**: Convention을 따랐는가?
- [ ] **성능**: 성능 문제는 없는가?
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

1. **TDD**: 테스트 → 구현 → 리팩토링 (시나리오 기반, Given-When-Then 주석 필수)
2. **테스트 검증**: 구현 후 반드시 빌드/테스트 실행, 통과해야만 완료
3. **SOLID**: 단일 책임, 인터페이스 분리, 의존성 역전
4. **KDoc**: 모든 public 함수/클래스
5. **REST Docs**: 모든 API
6. **DisplayName**: 시나리오를 명확히 설명하는 한글 설명
7. **MockK**: 단위 테스트 모킹
8. **Git Convention**: `feat(scope): subject`
9. **MVC 패턴**: Controller → Service → Repository
10. **성능 vs 가독성**: 가독성 우선, 필요시 최적화
11. **RESTful API**: 동사 금지, 적절한 HTTP 메서드/상태 코드

---

**작성일**: 2024-10-22
**버전**: 1.0.0
**대상**: Claude (AI 개발 어시스턴트)
