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

### 6. 엔티티 Audit Trail 필드 (필수)
- **모든 엔티티는 5가지 Audit Trail 필드 필수**
- 물리적 삭제 금지, 논리적 삭제(Soft Delete)만 허용
- 데이터 변경 이력 추적을 위한 감사 필드 필수

#### 필수 Audit Trail 필드

모든 엔티티는 다음 5가지 필드를 반드시 포함해야 합니다:

1. **createdAt**: `LocalDateTime` - 생성 시각
2. **createdBy**: `UUID?` - 생성한 사용자 ID
3. **updatedAt**: `LocalDateTime` - 최종 수정 시각
4. **updatedBy**: `UUID?` - 최종 수정한 사용자 ID
5. **deletedAt**: `LocalDateTime?` - 삭제 시각 (Soft Delete)

#### Audit Trail 구현 예시

```kotlin
// ✅ GOOD: 완전한 Audit Trail이 적용된 엔티티
data class User(
    val id: UUID,
    val email: String,
    val name: String,
    // Audit Trail 필드 (필수)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: UUID? = null,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val updatedBy: UUID? = null,
    val deletedAt: LocalDateTime? = null  // Soft Delete
)

// ✅ GOOD: 조회 시 삭제된 데이터 제외
fun findActiveUsers(): List<User> {
    return dslContext
        .select(USER.ID, USER.EMAIL, USER.NAME)  // 필요한 컬럼만 명시
        .from(USER)
        .where(USER.DELETED_AT.isNull)  // ✅ 삭제된 데이터 제외
        .fetchInto(User::class.java)
}

// ✅ GOOD: 생성 시 createdAt, createdBy 설정
fun createUser(userId: UUID, email: String): User {
    return dslContext
        .insertInto(USER)
        .set(USER.ID, UUID.randomUUID())
        .set(USER.EMAIL, email)
        .set(USER.CREATED_AT, LocalDateTime.now())
        .set(USER.CREATED_BY, userId)  // 생성자 기록
        .set(USER.UPDATED_AT, LocalDateTime.now())
        .set(USER.UPDATED_BY, userId)
        .returning()
        .fetchOne()!!
        .into(User::class.java)
}

// ✅ GOOD: 수정 시 updatedAt, updatedBy 설정
fun updateUser(userId: UUID, updatedBy: UUID, email: String) {
    dslContext
        .update(USER)
        .set(USER.EMAIL, email)
        .set(USER.UPDATED_AT, LocalDateTime.now())
        .set(USER.UPDATED_BY, updatedBy)  // 수정자 기록
        .where(USER.ID.eq(userId))
        .and(USER.DELETED_AT.isNull)
        .execute()
}

// ✅ GOOD: 삭제는 UPDATE로 구현 (Soft Delete)
fun deleteUser(userId: UUID, deletedBy: UUID) {
    dslContext
        .update(USER)
        .set(USER.DELETED_AT, LocalDateTime.now())
        .set(USER.UPDATED_AT, LocalDateTime.now())
        .set(USER.UPDATED_BY, deletedBy)  // 삭제자 기록
        .where(USER.ID.eq(userId))
        .and(USER.DELETED_AT.isNull)  // 이미 삭제된 데이터는 제외
        .execute()
}

// ❌ BAD: 물리적 삭제
fun deleteUser(userId: UUID) {
    dslContext
        .deleteFrom(USER)
        .where(USER.ID.eq(userId))
        .execute()  // ❌ 물리적 삭제 금지!
}
```

#### Audit Trail 체크리스트

- [ ] **모든 엔티티에 5가지 필드 존재**: `createdAt`, `createdBy`, `updatedAt`, `updatedBy`, `deletedAt`
- [ ] **조회 쿼리에 `deletedAt IS NULL` 조건 포함**
- [ ] **생성 시 `createdAt`, `createdBy` 설정**
- [ ] **수정 시 `updatedAt`, `updatedBy` 갱신**
- [ ] **삭제는 UPDATE로 구현**: `deletedAt`, `updatedAt`, `updatedBy` 설정
- [ ] **삭제된 데이터는 복구 가능하도록 보관**
- [ ] **데이터베이스 스키마에 모든 필드 존재 및 인덱스 설정**

#### 데이터베이스 스키마 예시

```sql
CREATE TABLE users (
    id CHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    -- Audit Trail 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    deleted_at TIMESTAMP NULL
);

-- 성능을 위한 인덱스
CREATE INDEX idx_deleted_at ON users(deleted_at);
```

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

### 계층별 역할 (MVC Layer Responsibilities)

> **중요**: 각 계층은 자신의 책임에만 집중해야 합니다. 계층 간 책임을 명확히 분리하지 않으면 유지보수가 어려워지고 테스트가 복잡해집니다.

#### Controller (컨트롤러)

**역할**: HTTP 요청/응답 처리만 담당

- ✅ HTTP 요청 수신 및 파라미터 추출
- ✅ Bean Validation (요청 검증)
- ✅ Service 호출 (비즈니스 로직 위임)
- ✅ HTTP 응답 생성 (상태 코드, 헤더, 바디)
- ✅ DTO 변환 (Entity → Response DTO)
- ❌ 비즈니스 로직 처리 금지
- ❌ 데이터베이스 접근 금지
- ❌ 복잡한 데이터 처리 금지 (FilePart 처리, 파일 변환 등)

##### @AuthenticationPrincipal 사용 규칙 (Spring Security 인증/인가)

**원칙**: userId를 파라미터로 받아야 한다면 Spring Security Context에서 `@AuthenticationPrincipal`로 추출해야 합니다.

**중요**: 이 프로젝트는 Spring Security를 사용하여 인증/인가를 처리합니다. 사용자 ID는 JWT 토큰에서 추출되어 Spring Security Context에 저장됩니다.

#### ✅ GOOD: @AuthenticationPrincipal 사용

```kotlin
@RestController
@RequestMapping("/api/v1/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService
) {
    @PostMapping("/views")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun trackViewEvent(
        @AuthenticationPrincipal userId: UUID,  // ✅ Spring Security Context에서 추출
        @Valid @RequestBody request: ViewEventRequest
    ): Mono<Void> {
        return analyticsService.trackViewEvent(userId, request)
    }
}
```

#### ❌ BAD: Request Body나 Path Variable로 userId 받기

```kotlin
// ❌ BAD: userId를 Request Body에서 받음 (보안 취약)
@PostMapping("/views")
fun trackViewEvent(
    @RequestBody request: ViewEventRequest  // userId가 request 안에 포함
): Mono<Void> {
    return analyticsService.trackViewEvent(request.userId, request)
}

// ❌ BAD: userId를 Path Variable로 받음 (변조 가능)
@PostMapping("/users/{userId}/views")
fun trackViewEvent(
    @PathVariable userId: UUID,  // 클라이언트가 임의로 변경 가능
    @RequestBody request: ViewEventRequest
): Mono<Void> {
    return analyticsService.trackViewEvent(userId, request)
}
```

#### 📋 @AuthenticationPrincipal 체크리스트

- [ ] **userId는 @AuthenticationPrincipal로 추출**: Request Body나 Path Variable로 받지 않기
- [ ] **JWT 토큰 검증 의존**: Spring Security가 토큰을 검증한 후 userId 제공
- [ ] **보안 우선**: 클라이언트가 userId를 임의로 변경할 수 없도록 설계

#### Service (서비스)

**역할**: 비즈니스 로직 처리

- ✅ 비즈니스 로직 구현
- ✅ 트랜잭션 관리 (@Transactional)
- ✅ 복잡한 데이터 처리 (FilePart 처리, 이미지 변환 등)
- ✅ 다른 서비스 호출 (서비스 간 조율)
- ✅ Repository 호출 (데이터베이스 접근)
- ✅ 예외 처리 및 변환
- ❌ HTTP 요청/응답 처리 금지
- ❌ HTTP 상태 코드 결정 금지

#### Repository (레포지토리)

**역할**: 데이터베이스 CRUD

- ✅ 데이터베이스 쿼리 실행 (JOOQ 사용)
- ✅ Entity 저장/조회/수정/삭제
- ❌ 비즈니스 로직 금지
- ❌ 다른 Repository 호출 최소화

#### Model (모델)

**역할**: 도메인 엔티티

- ✅ 도메인 객체 표현
- ✅ 간단한 비즈니스 규칙 (validation, 계산)

---

### 계층별 책임 예시: 프로필 이미지 업로드

#### ❌ 잘못된 설계 (Controller에 비즈니스 로직)

```kotlin
// ❌ BAD: Controller가 FilePart 처리 (비즈니스 로직)를 수행
@RestController
class UserProfileController(
    private val imageUploadService: ImageUploadService  // Infrastructure 계층 직접 의존
) {
    @PostMapping("/image")
    fun uploadProfileImage(
        @RequestAttribute userId: UUID,
        @RequestPart("file") filePart: Mono<FilePart>
    ): Mono<ResponseEntity<ImageUploadResponse>> {
        // ❌ Controller가 FilePart 처리 - 비즈니스 로직!
        return filePart.content()
            .map { dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                bytes
            }
            .reduce { acc, bytes -> acc + bytes }
            .flatMap { imageBytes ->
                val contentType = filePart.headers().contentType?.toString() ?: "application/octet-stream"
                imageUploadService.uploadProfileImage(userId, imageBytes, contentType)
            }
            .map { imageUrl ->
                ResponseEntity.status(HttpStatus.CREATED).body(ImageUploadResponse(imageUrl))
            }
    }
}
```

**문제점**:
- Controller가 FilePart 처리 (바이트 배열 변환, Content-Type 추출) 수행
- Controller가 Infrastructure 계층 (ImageUploadService)에 직접 의존
- 비즈니스 로직이 Controller에 있어 재사용 불가능
- 테스트가 복잡해짐 (HTTP 계층과 비즈니스 로직이 섞임)

#### ✅ 올바른 설계 (Service에 비즈니스 로직)

```kotlin
// ✅ GOOD: Controller는 HTTP 처리만, Service에 비즈니스 로직 위임
@RestController
class UserProfileController(
    private val userProfileService: UserProfileService  // Service 계층 의존
) {
    @PostMapping("/image")
    fun uploadProfileImage(
        @RequestAttribute userId: UUID,
        @RequestPart("file") filePart: Mono<FilePart>
    ): Mono<ResponseEntity<ImageUploadResponse>> {
        // ✅ Service에 비즈니스 로직 위임
        return filePart
            .flatMap { file ->
                userProfileService.uploadProfileImage(userId, file)
            }
            .map { imageUrl ->
                ResponseEntity.status(HttpStatus.CREATED).body(ImageUploadResponse(imageUrl))
            }
    }
}

// ✅ GOOD: Service가 비즈니스 로직 처리
@Service
class UserProfileServiceImpl(
    private val userProfileRepository: UserProfileRepository,
    private val imageUploadService: ImageUploadService
) : UserProfileService {
    override fun uploadProfileImage(userId: UUID, filePart: FilePart): Mono<String> {
        // ✅ FilePart 처리 로직은 Service에 위치
        return filePart.content()
            .map { dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                bytes
            }
            .reduce { acc, bytes -> acc + bytes }
            .flatMap { imageBytes ->
                val contentType = filePart.headers().contentType?.toString() ?: "application/octet-stream"
                imageUploadService.uploadProfileImage(userId, imageBytes, contentType)
            }
    }
}
```

**장점**:
- Controller는 HTTP 처리만 담당 (단일 책임)
- Service에 비즈니스 로직이 있어 재사용 가능
- 테스트가 간단해짐 (Controller 테스트는 Service를 mock, Service 테스트는 단위 테스트)
- 계층 간 의존성이 명확함 (Controller → Service → Infrastructure)

---

### 계층별 책임 체크리스트

**코드 작성 전 반드시 확인**:

- [ ] **Controller**: HTTP 요청/응답 처리만 하는가? 비즈니스 로직이 없는가?
- [ ] **Service**: 비즈니스 로직이 Service에 있는가? Controller에 비즈니스 로직이 없는가?
- [ ] **Repository**: 데이터베이스 쿼리만 수행하는가? 비즈니스 로직이 없는가?
- [ ] **테스트**: 각 계층을 독립적으로 테스트할 수 있는가?

---

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

### Repository 테스트 템플릿

**중요**: Repository는 반드시 **통합 테스트 (Integration Test)** 로 작성합니다.

**Why Integration Test?**
- Repository는 실제 데이터베이스와 상호작용하는 계층
- 단위 테스트로는 JOOQ 쿼리, SQL 문법, 데이터베이스 제약조건을 검증할 수 없음
- H2 In-Memory DB를 사용하여 실제 데이터베이스 동작을 검증
- 트랜잭션 격리, Soft Delete, Audit Trail 등 데이터베이스 레벨 기능 검증 필요

```kotlin
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("콘텐츠 인터랙션 Repository 통합 테스트")
class ContentInteractionRepositoryTest {

    @Autowired
    private lateinit var contentInteractionRepository: ContentInteractionRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    private lateinit var testUser: User
    private lateinit var testContentId: UUID

    @BeforeEach
    fun setUp() {
        // Given: 테스트 데이터 준비

        // 사용자 생성
        testUser = userRepository.save(
            User(
                email = "creator@test.com",
                provider = OAuthProvider.GOOGLE,
                providerId = "creator-123",
                role = UserRole.USER
            )
        )

        // 콘텐츠 생성
        testContentId = UUID.randomUUID()
        insertContent(testContentId, testUser.id!!, "Test Video")
    }

    @Nested
    @DisplayName("incrementViewCount - 조회수 증가")
    inner class IncrementViewCount {

        @Test
        @DisplayName("조회수를 1 증가시킨다")
        fun incrementViewCount_IncreasesCountByOne() {
            // Given: 초기 조회수 확인
            val initialCount = getViewCount(testContentId)

            // When: 조회수 증가
            contentInteractionRepository.incrementViewCount(testContentId).block()

            // Then: 1 증가 확인
            val updatedCount = getViewCount(testContentId)
            assertEquals(initialCount + 1, updatedCount)
        }

        @Test
        @DisplayName("여러 번 증가 시, 누적된다")
        fun incrementViewCount_MultipleTimes_Accumulates() {
            // Given: 초기 조회수 확인
            val initialCount = getViewCount(testContentId)

            // When: 3번 증가
            contentInteractionRepository.incrementViewCount(testContentId).block()
            contentInteractionRepository.incrementViewCount(testContentId).block()
            contentInteractionRepository.incrementViewCount(testContentId).block()

            // Then: 3 증가 확인
            val updatedCount = getViewCount(testContentId)
            assertEquals(initialCount + 3, updatedCount)
        }

        @Test
        @DisplayName("삭제된 콘텐츠는 업데이트되지 않는다")
        fun incrementViewCount_DeletedContent_DoesNotUpdate() {
            // Given: 콘텐츠 삭제 (Soft Delete)
            dslContext.update(CONTENT_INTERACTIONS)
                .set(CONTENT_INTERACTIONS.DELETED_AT, LocalDateTime.now())
                .where(CONTENT_INTERACTIONS.CONTENT_ID.eq(testContentId.toString()))
                .execute()

            val initialCount = getViewCount(testContentId)

            // When: 조회수 증가 시도
            contentInteractionRepository.incrementViewCount(testContentId).block()

            // Then: 변경 없음
            val updatedCount = getViewCount(testContentId)
            assertEquals(initialCount, updatedCount)
        }
    }

    /**
     * 콘텐츠 삽입 헬퍼 메서드
     */
    private fun insertContent(
        contentId: UUID,
        creatorId: UUID,
        title: String
    ) {
        val now = LocalDateTime.now()

        // Contents 테이블
        dslContext.insertInto(CONTENTS)
            .set(CONTENTS.ID, contentId.toString())
            .set(CONTENTS.CREATOR_ID, creatorId.toString())
            .set(CONTENTS.CONTENT_TYPE, ContentType.VIDEO.name)
            .set(CONTENTS.URL, "https://example.com/$contentId.mp4")
            .set(CONTENTS.THUMBNAIL_URL, "https://example.com/$contentId-thumb.jpg")
            .set(CONTENTS.DURATION, 60)
            .set(CONTENTS.WIDTH, 1920)
            .set(CONTENTS.HEIGHT, 1080)
            .set(CONTENTS.STATUS, ContentStatus.PUBLISHED.name)
            .set(CONTENTS.CREATED_AT, now)
            .set(CONTENTS.CREATED_BY, creatorId.toString())
            .set(CONTENTS.UPDATED_AT, now)
            .set(CONTENTS.UPDATED_BY, creatorId.toString())
            .execute()

        // Content_Metadata 테이블
        dslContext.insertInto(CONTENT_METADATA)
            .set(CONTENT_METADATA.CONTENT_ID, contentId.toString())
            .set(CONTENT_METADATA.TITLE, title)
            .set(CONTENT_METADATA.DESCRIPTION, "Test Description")
            .set(CONTENT_METADATA.CATEGORY, Category.PROGRAMMING.name)
            .set(CONTENT_METADATA.TAGS, JSON.valueOf("[\"test\"]"))
            .set(CONTENT_METADATA.LANGUAGE, "ko")
            .set(CONTENT_METADATA.CREATED_AT, now)
            .set(CONTENT_METADATA.CREATED_BY, creatorId.toString())
            .set(CONTENT_METADATA.UPDATED_AT, now)
            .set(CONTENT_METADATA.UPDATED_BY, creatorId.toString())
            .execute()

        // Content_Interactions 테이블 (초기값 0)
        dslContext.insertInto(CONTENT_INTERACTIONS)
            .set(CONTENT_INTERACTIONS.CONTENT_ID, contentId.toString())
            .set(CONTENT_INTERACTIONS.VIEW_COUNT, 0)
            .set(CONTENT_INTERACTIONS.LIKE_COUNT, 0)
            .set(CONTENT_INTERACTIONS.COMMENT_COUNT, 0)
            .set(CONTENT_INTERACTIONS.SAVE_COUNT, 0)
            .set(CONTENT_INTERACTIONS.SHARE_COUNT, 0)
            .set(CONTENT_INTERACTIONS.CREATED_AT, now)
            .set(CONTENT_INTERACTIONS.CREATED_BY, creatorId.toString())
            .set(CONTENT_INTERACTIONS.UPDATED_AT, now)
            .set(CONTENT_INTERACTIONS.UPDATED_BY, creatorId.toString())
            .execute()
    }

    /**
     * 조회수 조회 헬퍼 메서드
     */
    private fun getViewCount(contentId: UUID): Int {
        return dslContext.select(CONTENT_INTERACTIONS.VIEW_COUNT)
            .from(CONTENT_INTERACTIONS)
            .where(CONTENT_INTERACTIONS.CONTENT_ID.eq(contentId.toString()))
            .fetchOne(CONTENT_INTERACTIONS.VIEW_COUNT) ?: 0
    }
}
```

#### Repository 테스트 핵심 원칙

**1. 통합 테스트 필수 (Integration Test Required)**
```kotlin
@SpringBootTest          // ✅ Spring 컨텍스트 로드 (H2 DB 포함)
@ActiveProfiles("test")  // ✅ application-test.yml 사용
@Transactional           // ✅ 각 테스트 후 자동 롤백 (테스트 격리)
```

**2. 실제 데이터베이스 검증**
- DSLContext를 사용하여 실제 데이터베이스 상태 확인
- JOOQ 쿼리가 올바르게 실행되는지 검증
- Soft Delete, Audit Trail 등 데이터베이스 레벨 패턴 검증

**3. 헬퍼 메서드 활용**
```kotlin
// ✅ 테스트 데이터 삽입 헬퍼 메서드
private fun insertContent(contentId: UUID, creatorId: UUID, title: String) { /* ... */ }

// ✅ 데이터베이스 상태 확인 헬퍼 메서드
private fun getViewCount(contentId: UUID): Int { /* ... */ }
```

**4. Given-When-Then 패턴**
```kotlin
// Given: 테스트 데이터 준비 (BeforeEach 또는 테스트 메서드 내)
val initialCount = getViewCount(testContentId)

// When: Repository 메서드 실행
contentInteractionRepository.incrementViewCount(testContentId).block()

// Then: 데이터베이스 상태 검증
val updatedCount = getViewCount(testContentId)
assertEquals(initialCount + 1, updatedCount)
```

**5. Reactive 타입 처리**
```kotlin
// ✅ Mono/Flux는 .block() 또는 StepVerifier로 테스트
repository.save(entity).block()

// ✅ Flux는 .collectList().block()으로 변환
val results = repository.findAll().collectList().block()!!
assertEquals(3, results.size)
```

#### Repository 테스트 체크리스트

**모든 Repository는 반드시 다음을 테스트해야 합니다:**

- [ ] **CRUD 기본 동작**: save, findById, update, delete
- [ ] **조회 조건**: where 절, 정렬, 페이징, limit
- [ ] **Soft Delete**: deleted_at이 null인 데이터만 조회되는지 검증
- [ ] **Audit Trail**: created_at, created_by, updated_at, updated_by 자동 설정 검증
- [ ] **엣지 케이스**: 데이터 없을 때, 중복 데이터, null 값 처리
- [ ] **트랜잭션 격리**: 각 테스트가 독립적으로 실행되는지 확인 (@Transactional)

---

### ⚠️ 테스트 작성 필수 계층

**모든 기능 구현 시, 다음 3가지 계층의 테스트를 반드시 작성합니다:**

1. **Controller 테스트** - HTTP 요청/응답, Validation, REST Docs
2. **Service 테스트** - 비즈니스 로직, 예외 처리, 트랜잭션
3. **Repository 테스트** - 데이터베이스 CRUD, 쿼리, Soft Delete, Audit Trail

**❌ Controller + Service 테스트만 작성하고 Repository 테스트를 생략하지 마세요!**
**✅ Repository 테스트가 없으면 데이터베이스 레벨의 버그를 놓칠 수 있습니다!**

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

### 로깅 및 출력 규칙

**절대 준수**: 다음 규칙은 예외 없이 반드시 지켜야 합니다.

#### 1. println 사용 금지

- ❌ **절대 사용 금지**: `println()`, `print()`, `System.out.println()` 등 모든 콘솔 출력
- ✅ **반드시 사용**: SLF4J Logger 사용

```kotlin
// ❌ BAD: println 사용
fun startRedis() {
    redisServer?.start()
    println("Embedded Redis started on port $redisPort")  // 절대 금지!
}

// ✅ GOOD: Logger 사용
@Service
class RedisService {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisService::class.java)
    }

    fun startRedis() {
        redisServer?.start()
        logger.info("Embedded Redis started on port {}", redisPort)
    }
}
```

**이유**:
- println은 로그 레벨 제어 불가
- 프로덕션 환경에서 로그 추적 불가능
- 로그 파일로 저장되지 않음
- 구조화된 로깅 불가능

#### 2. 이모티콘 사용 금지

- ❌ **절대 사용 금지**: 코드, 주석, 로그 메시지에 이모티콘 사용 금지
- ✅ **허용**: 문서 파일 (README.md, CLAUDE.md 등)에서만 사용 가능

```kotlin
// ❌ BAD: 이모티콘 사용
logger.info("✅ Redis started successfully")
logger.warn("⚠️ Redis port already in use")
// 주석에도 이모티콘 사용 금지: // ✅ 성공 케이스

// ✅ GOOD: 텍스트만 사용
logger.info("Redis started successfully")
logger.warn("Redis port already in use")
// 주석도 텍스트만: // 성공 케이스
```

**이유**:
- 로그 파일 인코딩 문제 발생 가능
- 로그 검색 및 파싱 어려움
- 전문성 저하
- CI/CD 환경에서 이모티콘 깨질 수 있음

#### 📋 로깅 체크리스트

코드 작성 전 반드시 확인:

- [ ] **println 사용 금지**: `println`, `print`, `System.out.println` 사용하지 않았는가?
- [ ] **Logger 사용**: SLF4J Logger를 사용했는가?
- [ ] **이모티콘 제거**: 코드, 주석, 로그에 이모티콘(✅, ⚠️, 🔥 등)이 없는가?
- [ ] **로그 레벨 적절성**: 적절한 로그 레벨(info, warn, error, debug)을 사용했는가?

#### 3. FQCN 사용 금지

- ❌ **절대 사용 금지**: Fully Qualified Class Name (FQCN) 사용 금지
- ✅ **반드시 사용**: import 문 사용

```kotlin
// ❌ BAD: FQCN 사용
val scanOptions = org.springframework.data.redis.core.ScanOptions.scanOptions()
    .match(pattern)
    .build()

return redisTemplate.execute { connection ->
    connection.scriptingCommands()
        .eval(
            ByteBuffer.wrap(script.toByteArray()),
            org.springframework.data.redis.connection.ReturnType.INTEGER,  // FQCN 사용 금지!
            1,
            ByteBuffer.wrap(key.toByteArray())
        )
}

// ✅ GOOD: import 사용
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.connection.ReturnType

val scanOptions = ScanOptions.scanOptions()
    .match(pattern)
    .build()

return redisTemplate.execute { connection ->
    connection.scriptingCommands()
        .eval(
            ByteBuffer.wrap(script.toByteArray()),
            ReturnType.INTEGER,  // import한 클래스 사용
            1,
            ByteBuffer.wrap(key.toByteArray())
        )
}
```

**이유**:
- 코드 가독성 저하
- 네임스페이스 오염
- IDE의 자동 import 기능 활용 불가
- 코드 리뷰 및 유지보수 어려움

#### 📋 FQCN 체크리스트

코드 작성 전 반드시 확인:

- [ ] **FQCN 사용 금지**: `package.name.ClassName` 형태로 직접 사용하지 않았는가?
- [ ] **import 문 사용**: 모든 외부 클래스는 import하여 사용했는가?
- [ ] **IDE 자동 import**: IDE의 자동 import 기능을 활용했는가?

---

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
                .select(
                    VIDEO.ID,
                    VIDEO.TITLE,
                    VIDEO.URL,
                    VIDEO.DURATION
                )  // ✅ 필요한 컬럼만 명시적으로 선택
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

### Database Query 작성 규칙 (JOOQ)

**⚠️ 중요**: SELECT 쿼리에서 asterisk (*) 사용 절대 금지

#### ❌ 잘못된 예시

```kotlin
// ❌ BAD: asterisk 사용
dslContext
    .select(CONTENTS.asterisk())
    .from(CONTENTS)
    .fetch()

// ❌ BAD: 여러 테이블에 asterisk 사용
dslContext
    .select(
        CONTENTS.asterisk(),
        CONTENT_METADATA.asterisk(),
        CONTENT_INTERACTIONS.asterisk()
    )
    .from(CONTENTS)
    .join(CONTENT_METADATA).on(CONTENT_METADATA.CONTENT_ID.eq(CONTENTS.ID))
    .fetch()
```

**문제점**:
1. **성능 저하**: 불필요한 컬럼까지 모두 조회하여 DB 및 네트워크 부하 증가
2. **대역폭 낭비**: 사용하지 않는 데이터까지 전송
3. **유지보수 어려움**: 어떤 컬럼을 실제로 사용하는지 코드만 보고 파악 불가
4. **스키마 변경에 취약**: 테이블 컬럼 추가/삭제 시 예상치 못한 오류 발생 가능

#### ✅ 올바른 예시

```kotlin
// ✅ GOOD: 필요한 컬럼만 명시적으로 선택
dslContext
    .select(
        CONTENTS.ID,
        CONTENTS.CONTENT_TYPE,
        CONTENTS.URL,
        CONTENTS.THUMBNAIL_URL,
        CONTENTS.DURATION,
        CONTENTS.WIDTH,
        CONTENTS.HEIGHT
    )
    .from(CONTENTS)
    .fetch()

// ✅ GOOD: 조인 쿼리에서도 필요한 컬럼만 명시
dslContext
    .select(
        // CONTENTS 필요 컬럼
        CONTENTS.ID,
        CONTENTS.CONTENT_TYPE,
        CONTENTS.URL,
        CONTENTS.THUMBNAIL_URL,
        // CONTENT_METADATA 필요 컬럼
        CONTENT_METADATA.TITLE,
        CONTENT_METADATA.DESCRIPTION,
        CONTENT_METADATA.CATEGORY,
        // CONTENT_INTERACTIONS 필요 컬럼
        CONTENT_INTERACTIONS.LIKE_COUNT,
        CONTENT_INTERACTIONS.VIEW_COUNT
    )
    .from(CONTENTS)
    .join(CONTENT_METADATA).on(CONTENT_METADATA.CONTENT_ID.eq(CONTENTS.ID))
    .join(CONTENT_INTERACTIONS).on(CONTENT_INTERACTIONS.CONTENT_ID.eq(CONTENTS.ID))
    .fetch()
```

**장점**:
1. ✅ **성능 최적화**: 필요한 데이터만 조회하여 DB 부하 감소
2. ✅ **명확성**: 코드만 보고 어떤 데이터를 사용하는지 즉시 파악 가능
3. ✅ **안정성**: 스키마 변경 시 영향받는 범위를 명확히 알 수 있음
4. ✅ **대역폭 절약**: 네트워크 트래픽 최소화

#### 📋 Database Query 체크리스트

코드 작성 전 반드시 확인:

- [ ] **asterisk 사용 금지**: `.select(TABLE.asterisk())` 사용하지 않았는가?
- [ ] **명시적 컬럼 선택**: 실제 사용하는 컬럼만 명시적으로 선택했는가?
- [ ] **주석 추가**: 조인이 복잡한 경우, 각 테이블의 컬럼 그룹에 주석을 추가했는가?
- [ ] **불필요한 컬럼 제거**: 조회하지만 사용하지 않는 컬럼은 없는가?

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

## 📨 Spring Event 패턴 (비동기 이벤트 처리)

### 개요

Spring Event는 애플리케이션 내에서 비동기 이벤트 기반 통신을 구현하는 패턴입니다.

**언제 사용하는가?**
- 메인 트랜잭션과 독립적으로 실행되어야 하는 작업
- 실패해도 메인 요청에 영향을 주지 않아야 하는 작업
- 여러 도메인 간 결합도를 낮추고 싶을 때

**GrowSnap에서의 사용 예시**:
- 사용자가 콘텐츠에 좋아요를 누를 때
  1. 메인 트랜잭션: `content_interactions.like_count` 증가
  2. Spring Event 발행: `UserInteractionEvent`
  3. 비동기 처리: `user_content_interactions` 테이블에 저장 (협업 필터링용)

### ✅ Spring Event 패턴 Best Practice

#### 1. 이벤트 클래스 정의

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

#### 2. 이벤트 발행자 (Publisher)

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
}
```

#### 3. 이벤트 리스너 (Listener)

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

#### 4. Spring Async 설정

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

### 📋 Spring Event 패턴 체크리스트

- [ ] **이벤트 클래스**: data class로 정의, 필요한 최소 정보만 포함
- [ ] **이벤트 발행**: `applicationEventPublisher.publishEvent()` 사용
- [ ] **발행 시점**: 메인 트랜잭션 성공 시에만 발행 (`doOnSuccess`)
- [ ] **이벤트 리스너**: `@TransactionalEventListener(phase = AFTER_COMMIT)` 사용
- [ ] **비동기 처리**: `@Async` 사용
- [ ] **장애 격리**: try-catch로 예외 삼킴, 로그만 남김
- [ ] **Spring Async 설정**: `@EnableAsync` + ThreadPoolTaskExecutor 설정

### ⚠️ 주의사항

1. **메인 트랜잭션과 독립성 보장**
   - 이벤트 리스너 실패가 메인 요청에 영향을 주지 않도록 설계
   - `TransactionPhase.AFTER_COMMIT` 사용 필수

2. **멱등성(Idempotency) 고려**
   - 이벤트가 중복 발생할 수 있으므로 멱등성 보장 필요
   - 예: UNIQUE 제약 조건 설정

3. **로깅 충실**
   - 이벤트 발행/처리 시점에 DEBUG 로그 남기기
   - 실패 시 ERROR 로그로 추적 가능하도록

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
12. **Audit Trail**: 모든 엔티티에 5가지 필드 필수 (createdAt, createdBy, updatedAt, updatedBy, deletedAt), 물리적 삭제 금지
13. **Database Query**: SELECT 쿼리에서 asterisk (*) 사용 절대 금지, 필요한 컬럼만 명시적으로 선택
14. **로깅 규칙**: println 절대 금지, SLF4J Logger 필수 사용
15. **이모티콘 금지**: 코드, 주석, 로그에 이모티콘 절대 사용 금지 (문서 파일만 허용)
16. **FQCN 금지**: Fully Qualified Class Name 사용 금지, 반드시 import 문 사용
17. **@AuthenticationPrincipal**: userId는 @AuthenticationPrincipal로 Spring Security Context에서 추출, Request Body/Path Variable 사용 금지
18. **Spring Event 패턴**: 비동기 이벤트 처리 시 @TransactionalEventListener(AFTER_COMMIT) + @Async 사용, 메인 트랜잭션과 독립성 보장

---

**작성일**: 2025-10-23
**버전**: 1.1.0
**대상**: Claude (AI 개발 어시스턴트)
**작성자**: @12OneTwo12
