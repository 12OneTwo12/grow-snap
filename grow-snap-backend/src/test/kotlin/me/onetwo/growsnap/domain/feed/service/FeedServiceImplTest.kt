package me.onetwo.growsnap.domain.feed.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import me.onetwo.growsnap.domain.content.model.Category
import me.onetwo.growsnap.domain.content.model.ContentType
import me.onetwo.growsnap.domain.feed.dto.CreatorInfoResponse
import me.onetwo.growsnap.domain.feed.dto.FeedItemResponse
import me.onetwo.growsnap.domain.feed.dto.InteractionInfoResponse
import me.onetwo.growsnap.domain.feed.repository.FeedRepository
import me.onetwo.growsnap.infrastructure.common.dto.CursorPageRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("피드 서비스 테스트")
class FeedServiceImplTest {

    @MockK
    private lateinit var feedRepository: FeedRepository

    @InjectMockKs
    private lateinit var feedService: FeedServiceImpl

    @Nested
    @DisplayName("getMainFeed - 메인 피드 조회")
    inner class GetMainFeed {

        @Test
        @DisplayName("유효한 요청으로 조회 시, 피드 아이템 목록을 반환한다")
        fun getMainFeed_WithValidRequest_ReturnsFeedItems() {
            // Given: 테스트 데이터
            val userId = UUID.randomUUID()
            val pageRequest = CursorPageRequest(cursor = null, limit = 10)
            val recentlyViewedIds = emptyList<UUID>()
            val feedItems = createMockFeedItems(10)

            every { feedRepository.findRecentlyViewedContentIds(userId, any()) } returns Flux.fromIterable(recentlyViewedIds)
            every { feedRepository.findMainFeed(userId, null, 11, recentlyViewedIds) } returns Flux.fromIterable(feedItems)

            // When: 메인 피드 조회
            val result = feedService.getMainFeed(userId, pageRequest)

            // Then: 피드 아이템 목록을 반환한다
            StepVerifier.create(result)
                .assertNext { response ->
                    assertThat(response.content).hasSize(10)
                    assertThat(response.hasNext).isFalse
                    assertThat(response.nextCursor).isNull()
                }
                .verifyComplete()

            verify(exactly = 1) { feedRepository.findRecentlyViewedContentIds(userId, any()) }
            verify(exactly = 1) { feedRepository.findMainFeed(userId, null, 11, recentlyViewedIds) }
        }

        @Test
        @DisplayName("limit + 1개의 데이터가 있는 경우, hasNext가 true이고 nextCursor가 설정된다")
        fun getMainFeed_WithMoreData_ReturnsHasNextTrue() {
            // Given: limit + 1개의 데이터
            val userId = UUID.randomUUID()
            val pageRequest = CursorPageRequest(cursor = null, limit = 10)
            val recentlyViewedIds = emptyList<UUID>()
            val feedItems = createMockFeedItems(11)

            every { feedRepository.findRecentlyViewedContentIds(userId, any()) } returns Flux.fromIterable(recentlyViewedIds)
            every { feedRepository.findMainFeed(userId, null, 11, recentlyViewedIds) } returns Flux.fromIterable(feedItems)

            // When: 메인 피드 조회
            val result = feedService.getMainFeed(userId, pageRequest)

            // Then: hasNext가 true이고 nextCursor가 설정된다
            StepVerifier.create(result)
                .assertNext { response ->
                    assertThat(response.content).hasSize(10)
                    assertThat(response.hasNext).isTrue
                    assertThat(response.nextCursor).isNotNull
                    assertThat(response.nextCursor).isEqualTo(response.content.last().contentId.toString())
                }
                .verifyComplete()
        }

        @Test
        @DisplayName("커서가 있는 경우, 다음 페이지를 반환한다")
        fun getMainFeed_WithCursor_ReturnsNextPage() {
            // Given: 커서가 있는 요청
            val userId = UUID.randomUUID()
            val cursor = UUID.randomUUID()
            val pageRequest = CursorPageRequest(cursor = cursor.toString(), limit = 10)
            val recentlyViewedIds = emptyList<UUID>()
            val feedItems = createMockFeedItems(10)

            every { feedRepository.findRecentlyViewedContentIds(userId, any()) } returns Flux.fromIterable(recentlyViewedIds)
            every { feedRepository.findMainFeed(userId, cursor, 11, recentlyViewedIds) } returns Flux.fromIterable(feedItems)

            // When: 메인 피드 조회
            val result = feedService.getMainFeed(userId, pageRequest)

            // Then: 다음 페이지를 반환한다
            StepVerifier.create(result)
                .assertNext { response ->
                    assertThat(response.content).hasSize(10)
                }
                .verifyComplete()

            verify(exactly = 1) { feedRepository.findMainFeed(userId, cursor, 11, recentlyViewedIds) }
        }

        @Test
        @DisplayName("최근 본 콘텐츠가 있는 경우, 제외하고 조회한다")
        fun getMainFeed_WithRecentlyViewed_ExcludesThoseContents() {
            // Given: 최근 본 콘텐츠가 있는 경우
            val userId = UUID.randomUUID()
            val pageRequest = CursorPageRequest(cursor = null, limit = 10)
            val recentlyViewedIds = listOf(UUID.randomUUID(), UUID.randomUUID())
            val feedItems = createMockFeedItems(10)

            every { feedRepository.findRecentlyViewedContentIds(userId, any()) } returns Flux.fromIterable(recentlyViewedIds)
            every { feedRepository.findMainFeed(userId, null, 11, recentlyViewedIds) } returns Flux.fromIterable(feedItems)

            // When: 메인 피드 조회
            val result = feedService.getMainFeed(userId, pageRequest)

            // Then: 최근 본 콘텐츠를 제외하고 조회한다
            StepVerifier.create(result)
                .assertNext { response ->
                    assertThat(response.content).hasSize(10)
                }
                .verifyComplete()

            verify(exactly = 1) { feedRepository.findMainFeed(userId, null, 11, recentlyViewedIds) }
        }
    }

    @Nested
    @DisplayName("getFollowingFeed - 팔로잉 피드 조회")
    inner class GetFollowingFeed {

        @Test
        @DisplayName("유효한 요청으로 조회 시, 팔로잉 피드 목록을 반환한다")
        fun getFollowingFeed_WithValidRequest_ReturnsFeedItems() {
            // Given: 테스트 데이터
            val userId = UUID.randomUUID()
            val pageRequest = CursorPageRequest(cursor = null, limit = 10)
            val feedItems = createMockFeedItems(10)

            every { feedRepository.findFollowingFeed(userId, null, 11) } returns Flux.fromIterable(feedItems)

            // When: 팔로잉 피드 조회
            val result = feedService.getFollowingFeed(userId, pageRequest)

            // Then: 팔로잉 피드 목록을 반환한다
            StepVerifier.create(result)
                .assertNext { response ->
                    assertThat(response.content).hasSize(10)
                    assertThat(response.hasNext).isFalse
                    assertThat(response.nextCursor).isNull()
                }
                .verifyComplete()

            verify(exactly = 1) { feedRepository.findFollowingFeed(userId, null, 11) }
        }

        @Test
        @DisplayName("limit + 1개의 데이터가 있는 경우, hasNext가 true이고 nextCursor가 설정된다")
        fun getFollowingFeed_WithMoreData_ReturnsHasNextTrue() {
            // Given: limit + 1개의 데이터
            val userId = UUID.randomUUID()
            val pageRequest = CursorPageRequest(cursor = null, limit = 10)
            val feedItems = createMockFeedItems(11)

            every { feedRepository.findFollowingFeed(userId, null, 11) } returns Flux.fromIterable(feedItems)

            // When: 팔로잉 피드 조회
            val result = feedService.getFollowingFeed(userId, pageRequest)

            // Then: hasNext가 true이고 nextCursor가 설정된다
            StepVerifier.create(result)
                .assertNext { response ->
                    assertThat(response.content).hasSize(10)
                    assertThat(response.hasNext).isTrue
                    assertThat(response.nextCursor).isNotNull
                }
                .verifyComplete()
        }
    }

    /**
     * 테스트용 피드 아이템 목록 생성
     */
    private fun createMockFeedItems(count: Int): List<FeedItemResponse> {
        return (1..count).map {
            FeedItemResponse(
                contentId = UUID.randomUUID(),
                contentType = ContentType.VIDEO,
                url = "https://example.com/video$it.mp4",
                thumbnailUrl = "https://example.com/thumbnail$it.jpg",
                duration = 60,
                width = 1920,
                height = 1080,
                title = "Test Video $it",
                description = "Test Description $it",
                category = Category.PROGRAMMING,
                tags = listOf("test", "video"),
                creator = CreatorInfoResponse(
                    userId = UUID.randomUUID(),
                    nickname = "Creator $it",
                    profileImageUrl = "https://example.com/profile$it.jpg",
                    followerCount = 1000
                ),
                interactions = InteractionInfoResponse(
                    likeCount = 100,
                    commentCount = 50,
                    saveCount = 30,
                    shareCount = 20,
                    viewCount = 1000
                ),
                subtitles = emptyList()
            )
        }
    }
}
