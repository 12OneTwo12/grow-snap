package me.onetwo.growsnap.domain.feed.repository

import me.onetwo.growsnap.domain.content.model.Category
import me.onetwo.growsnap.domain.content.model.ContentStatus
import me.onetwo.growsnap.domain.content.model.ContentType
import me.onetwo.growsnap.domain.user.model.Follow
import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserProfile
import me.onetwo.growsnap.domain.user.model.UserRole
import me.onetwo.growsnap.domain.user.repository.FollowRepository
import me.onetwo.growsnap.domain.user.repository.UserProfileRepository
import me.onetwo.growsnap.domain.user.repository.UserRepository
import me.onetwo.growsnap.jooq.generated.tables.references.CONTENTS
import me.onetwo.growsnap.jooq.generated.tables.references.CONTENT_INTERACTIONS
import me.onetwo.growsnap.jooq.generated.tables.references.CONTENT_METADATA
import me.onetwo.growsnap.jooq.generated.tables.references.CONTENT_SUBTITLES
import me.onetwo.growsnap.jooq.generated.tables.references.USER_VIEW_HISTORY
import org.jooq.DSLContext
import org.jooq.JSON
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

/**
 * FeedRepository 통합 테스트
 *
 * 실제 데이터베이스(H2)를 사용하여 피드 조회 기능을 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("피드 Repository 통합 테스트")
class FeedRepositoryImplTest {

    @Autowired
    private lateinit var feedRepository: FeedRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userProfileRepository: UserProfileRepository

    @Autowired
    private lateinit var followRepository: FollowRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    private lateinit var viewer: User
    private lateinit var creator1: User
    private lateinit var creator2: User
    private lateinit var content1Id: UUID
    private lateinit var content2Id: UUID
    private lateinit var content3Id: UUID

    @BeforeEach
    fun setUp() {
        // Given: 테스트 데이터 준비

        // 사용자 생성
        viewer = userRepository.save(
            User(
                email = "viewer@test.com",
                provider = OAuthProvider.GOOGLE,
                providerId = "viewer-123",
                role = UserRole.USER
            )
        )

        creator1 = userRepository.save(
            User(
                email = "creator1@test.com",
                provider = OAuthProvider.GOOGLE,
                providerId = "creator1-123",
                role = UserRole.USER
            )
        )

        creator2 = userRepository.save(
            User(
                email = "creator2@test.com",
                provider = OAuthProvider.GOOGLE,
                providerId = "creator2-123",
                role = UserRole.USER
            )
        )

        // 프로필 생성
        userProfileRepository.save(
            UserProfile(
                userId = viewer.id!!,
                nickname = "Viewer",
                createdBy = viewer.id!!
            )
        )

        userProfileRepository.save(
            UserProfile(
                userId = creator1.id!!,
                nickname = "Creator1",
                createdBy = creator1.id!!
            )
        )

        userProfileRepository.save(
            UserProfile(
                userId = creator2.id!!,
                nickname = "Creator2",
                createdBy = creator2.id!!
            )
        )

        // 콘텐츠 3개 생성
        content1Id = UUID.randomUUID()
        content2Id = UUID.randomUUID()
        content3Id = UUID.randomUUID()

        // Content 1 (Creator1, 가장 최신)
        insertContent(
            contentId = content1Id,
            creatorId = creator1.id!!,
            title = "Test Video 1",
            createdAt = LocalDateTime.now().minusHours(1)
        )

        // Content 2 (Creator2, 중간)
        insertContent(
            contentId = content2Id,
            creatorId = creator2.id!!,
            title = "Test Video 2",
            createdAt = LocalDateTime.now().minusHours(2)
        )

        // Content 3 (Creator1, 가장 오래됨)
        insertContent(
            contentId = content3Id,
            creatorId = creator1.id!!,
            title = "Test Video 3",
            createdAt = LocalDateTime.now().minusHours(3)
        )
    }

    @Nested
    @DisplayName("findMainFeed - 메인 피드 조회")
    inner class FindMainFeed {

        @Test
        @DisplayName("커서 없이 조회 시, 최신 콘텐츠부터 반환한다")
        fun findMainFeed_WithoutCursor_ReturnsLatestContent() {
            // When
            val result = feedRepository.findMainFeed(
                userId = viewer.id!!,
                cursor = null,
                limit = 10,
                excludeContentIds = emptyList()
            ).collectList().block()!!

            // Then
            assertEquals(3, result.size)
            assertEquals(content1Id, result[0].contentId) // 가장 최신
            assertEquals(content2Id, result[1].contentId)
            assertEquals(content3Id, result[2].contentId)
            assertEquals("Test Video 1", result[0].title)
        }

        @Test
        @DisplayName("커서와 함께 조회 시, 커서 이후 콘텐츠를 반환한다")
        fun findMainFeed_WithCursor_ReturnsContentAfterCursor() {
            // When: content1을 커서로 사용
            val result = feedRepository.findMainFeed(
                userId = viewer.id!!,
                cursor = content1Id,
                limit = 10,
                excludeContentIds = emptyList()
            ).collectList().block()!!

            // Then: content1 이후의 콘텐츠만 반환
            assertEquals(2, result.size)
            assertEquals(content2Id, result[0].contentId)
            assertEquals(content3Id, result[1].contentId)
        }

        @Test
        @DisplayName("제외할 콘텐츠 ID 목록이 있으면, 해당 콘텐츠를 제외한다")
        fun findMainFeed_WithExcludeIds_ExcludesSpecifiedContent() {
            // When: content1과 content3을 제외
            val result = feedRepository.findMainFeed(
                userId = viewer.id!!,
                cursor = null,
                limit = 10,
                excludeContentIds = listOf(content1Id, content3Id)
            ).collectList().block()!!

            // Then: content2만 반환
            assertEquals(1, result.size)
            assertEquals(content2Id, result[0].contentId)
        }

        @Test
        @DisplayName("limit 이하의 결과만 반환한다")
        fun findMainFeed_WithLimit_ReturnsLimitedResults() {
            // When: limit=2
            val result = feedRepository.findMainFeed(
                userId = viewer.id!!,
                cursor = null,
                limit = 2,
                excludeContentIds = emptyList()
            ).collectList().block()!!

            // Then: 2개만 반환
            assertEquals(2, result.size)
            assertEquals(content1Id, result[0].contentId)
            assertEquals(content2Id, result[1].contentId)
        }

        @Test
        @DisplayName("자막 정보가 있으면 함께 반환한다")
        fun findMainFeed_WithSubtitles_ReturnsSubtitles() {
            // Given: content1에 자막 추가
            insertSubtitle(
                contentId = content1Id,
                language = "ko",
                subtitleUrl = "https://example.com/subtitle-ko.vtt"
            )
            insertSubtitle(
                contentId = content1Id,
                language = "en",
                subtitleUrl = "https://example.com/subtitle-en.vtt"
            )

            // When
            val result = feedRepository.findMainFeed(
                userId = viewer.id!!,
                cursor = null,
                limit = 10,
                excludeContentIds = emptyList()
            ).collectList().block()!!

            // Then
            val content1 = result.find { it.contentId == content1Id }!!
            assertEquals(2, content1.subtitles.size)
            assertTrue(content1.subtitles.any { it.language == "ko" })
            assertTrue(content1.subtitles.any { it.language == "en" })
        }
    }

    @Nested
    @DisplayName("findFollowingFeed - 팔로잉 피드 조회")
    inner class FindFollowingFeed {

        @Test
        @DisplayName("팔로우한 크리에이터의 콘텐츠만 반환한다")
        fun findFollowingFeed_ReturnsOnlyFollowedCreatorsContent() {
            // Given: viewer가 creator1을 팔로우
            followRepository.save(
                Follow(
                    followerId = viewer.id!!,
                    followingId = creator1.id!!,
                    createdBy = viewer.id!!
                )
            )

            // When
            val result = feedRepository.findFollowingFeed(
                userId = viewer.id!!,
                cursor = null,
                limit = 10
            ).collectList().block()!!

            // Then: creator1의 콘텐츠만 반환 (content1, content3)
            assertEquals(2, result.size)
            assertEquals(content1Id, result[0].contentId)
            assertEquals(content3Id, result[1].contentId)
        }

        @Test
        @DisplayName("팔로우하지 않은 경우, 빈 목록을 반환한다")
        fun findFollowingFeed_WithNoFollowing_ReturnsEmptyList() {
            // When: 아무도 팔로우하지 않음
            val result = feedRepository.findFollowingFeed(
                userId = viewer.id!!,
                cursor = null,
                limit = 10
            ).collectList().block()!!

            // Then: 빈 목록
            assertEquals(0, result.size)
        }

        @Test
        @DisplayName("여러 크리에이터를 팔로우하면, 모든 크리에이터의 콘텐츠를 반환한다")
        fun findFollowingFeed_WithMultipleFollowing_ReturnsAllContent() {
            // Given: viewer가 creator1과 creator2를 팔로우
            followRepository.save(
                Follow(
                    followerId = viewer.id!!,
                    followingId = creator1.id!!,
                    createdBy = viewer.id!!
                )
            )
            followRepository.save(
                Follow(
                    followerId = viewer.id!!,
                    followingId = creator2.id!!,
                    createdBy = viewer.id!!
                )
            )

            // When
            val result = feedRepository.findFollowingFeed(
                userId = viewer.id!!,
                cursor = null,
                limit = 10
            ).collectList().block()!!

            // Then: 모든 콘텐츠 반환 (content1, content2, content3)
            assertEquals(3, result.size)
        }
    }

    @Nested
    @DisplayName("findRecentlyViewedContentIds - 최근 본 콘텐츠 ID 조회")
    inner class FindRecentlyViewedContentIds {

        @Test
        @DisplayName("최근 본 콘텐츠 ID 목록을 시간 역순으로 반환한다")
        fun findRecentlyViewedContentIds_ReturnsRecentlyViewedInDescOrder() {
            // Given: 시청 기록 추가 (content3 → content1 → content2 순서로 시청)
            insertViewHistory(
                userId = viewer.id!!,
                contentId = content3Id,
                watchedAt = LocalDateTime.now().minusHours(3)
            )
            insertViewHistory(
                userId = viewer.id!!,
                contentId = content1Id,
                watchedAt = LocalDateTime.now().minusHours(1)
            )
            insertViewHistory(
                userId = viewer.id!!,
                contentId = content2Id,
                watchedAt = LocalDateTime.now()
            )

            // When
            val result = feedRepository.findRecentlyViewedContentIds(
                userId = viewer.id!!,
                limit = 10
            ).collectList().block()!!

            // Then: 최근 시청 순서대로 반환 (content2 → content1 → content3)
            assertEquals(3, result.size)
            assertEquals(content2Id, result[0])
            assertEquals(content1Id, result[1])
            assertEquals(content3Id, result[2])
        }

        @Test
        @DisplayName("limit 이하의 결과만 반환한다")
        fun findRecentlyViewedContentIds_WithLimit_ReturnsLimitedResults() {
            // Given: 시청 기록 3개
            insertViewHistory(viewer.id!!, content1Id, LocalDateTime.now().minusHours(3))
            insertViewHistory(viewer.id!!, content2Id, LocalDateTime.now().minusHours(2))
            insertViewHistory(viewer.id!!, content3Id, LocalDateTime.now().minusHours(1))

            // When: limit=2
            val result = feedRepository.findRecentlyViewedContentIds(
                userId = viewer.id!!,
                limit = 2
            ).collectList().block()!!

            // Then: 최근 2개만 반환
            assertEquals(2, result.size)
        }

        @Test
        @DisplayName("시청 기록이 없으면, 빈 목록을 반환한다")
        fun findRecentlyViewedContentIds_WithNoHistory_ReturnsEmptyList() {
            // When: 시청 기록 없음
            val result = feedRepository.findRecentlyViewedContentIds(
                userId = viewer.id!!,
                limit = 10
            ).collectList().block()!!

            // Then: 빈 목록
            assertEquals(0, result.size)
        }
    }

    /**
     * 콘텐츠 삽입 헬퍼 메서드
     */
    private fun insertContent(
        contentId: UUID,
        creatorId: UUID,
        title: String,
        createdAt: LocalDateTime
    ) {
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
            .set(CONTENTS.CREATED_AT, createdAt)
            .set(CONTENTS.CREATED_BY, creatorId.toString())
            .set(CONTENTS.UPDATED_AT, createdAt)
            .set(CONTENTS.UPDATED_BY, creatorId.toString())
            .execute()

        // Content_Metadata 테이블
        dslContext.insertInto(CONTENT_METADATA)
            .set(CONTENT_METADATA.CONTENT_ID, contentId.toString())
            .set(CONTENT_METADATA.TITLE, title)
            .set(CONTENT_METADATA.DESCRIPTION, "Test Description")
            .set(CONTENT_METADATA.CATEGORY, Category.PROGRAMMING.name)
            .set(CONTENT_METADATA.TAGS, JSON.valueOf("[\"test\", \"video\"]"))
            .set(CONTENT_METADATA.LANGUAGE, "ko")
            .set(CONTENT_METADATA.CREATED_AT, createdAt)
            .set(CONTENT_METADATA.CREATED_BY, creatorId.toString())
            .set(CONTENT_METADATA.UPDATED_AT, createdAt)
            .set(CONTENT_METADATA.UPDATED_BY, creatorId.toString())
            .execute()

        // Content_Interactions 테이블
        dslContext.insertInto(CONTENT_INTERACTIONS)
            .set(CONTENT_INTERACTIONS.CONTENT_ID, contentId.toString())
            .set(CONTENT_INTERACTIONS.LIKE_COUNT, 100)
            .set(CONTENT_INTERACTIONS.COMMENT_COUNT, 50)
            .set(CONTENT_INTERACTIONS.SAVE_COUNT, 30)
            .set(CONTENT_INTERACTIONS.SHARE_COUNT, 20)
            .set(CONTENT_INTERACTIONS.VIEW_COUNT, 1000)
            .set(CONTENT_INTERACTIONS.CREATED_AT, createdAt)
            .set(CONTENT_INTERACTIONS.CREATED_BY, creatorId.toString())
            .set(CONTENT_INTERACTIONS.UPDATED_AT, createdAt)
            .set(CONTENT_INTERACTIONS.UPDATED_BY, creatorId.toString())
            .execute()
    }

    /**
     * 자막 삽입 헬퍼 메서드
     */
    private fun insertSubtitle(
        contentId: UUID,
        language: String,
        subtitleUrl: String
    ) {
        dslContext.insertInto(CONTENT_SUBTITLES)
            .set(CONTENT_SUBTITLES.CONTENT_ID, contentId.toString())
            .set(CONTENT_SUBTITLES.LANGUAGE, language)
            .set(CONTENT_SUBTITLES.SUBTITLE_URL, subtitleUrl)
            .set(CONTENT_SUBTITLES.CREATED_AT, LocalDateTime.now())
            .set(CONTENT_SUBTITLES.UPDATED_AT, LocalDateTime.now())
            .execute()
    }

    /**
     * 시청 기록 삽입 헬퍼 메서드
     */
    private fun insertViewHistory(
        userId: UUID,
        contentId: UUID,
        watchedAt: LocalDateTime
    ) {
        dslContext.insertInto(USER_VIEW_HISTORY)
            .set(USER_VIEW_HISTORY.USER_ID, userId.toString())
            .set(USER_VIEW_HISTORY.CONTENT_ID, contentId.toString())
            .set(USER_VIEW_HISTORY.WATCHED_AT, watchedAt)
            .set(USER_VIEW_HISTORY.COMPLETION_RATE, 100)
            .set(USER_VIEW_HISTORY.CREATED_AT, watchedAt)
            .set(USER_VIEW_HISTORY.CREATED_BY, userId.toString())
            .set(USER_VIEW_HISTORY.UPDATED_AT, watchedAt)
            .set(USER_VIEW_HISTORY.UPDATED_BY, userId.toString())
            .execute()
    }
}
