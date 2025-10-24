package me.onetwo.growsnap.domain.user.service

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import me.onetwo.growsnap.domain.user.exception.AlreadyFollowingException
import me.onetwo.growsnap.domain.user.exception.CannotFollowSelfException
import me.onetwo.growsnap.domain.user.exception.NotFollowingException
import me.onetwo.growsnap.domain.user.model.Follow
import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserProfile
import me.onetwo.growsnap.domain.user.model.UserRole
import me.onetwo.growsnap.domain.user.repository.FollowRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

/**
 * FollowService 단위 테스트
 */
@ExtendWith(MockKExtension::class)
@DisplayName("팔로우 Service 테스트")
class FollowServiceTest {

    private lateinit var followRepository: FollowRepository
    private lateinit var userService: UserService
    private lateinit var userProfileService: UserProfileService
    private lateinit var followService: FollowService

    private lateinit var followerUser: User
    private lateinit var followingUser: User
    private lateinit var followerProfile: UserProfile
    private lateinit var followingProfile: UserProfile

    @BeforeEach
    fun setUp() {
        followRepository = mockk()
        userService = mockk()
        userProfileService = mockk()
        followService = FollowServiceImpl(followRepository, userService, userProfileService)

        followerUser = User(
            id = UUID.randomUUID(),
            email = "follower@example.com",
            provider = OAuthProvider.GOOGLE,
            providerId = "follower-123",
            role = UserRole.USER
        )

        followingUser = User(
            id = UUID.randomUUID(),
            email = "following@example.com",
            provider = OAuthProvider.GOOGLE,
            providerId = "following-456",
            role = UserRole.USER
        )

        followerProfile = UserProfile(
            id = null,
            userId = UUID.randomUUID(),
            nickname = "follower",
            followingCount = 0
        )

        followingProfile = UserProfile(
            id = null,
            userId = UUID.randomUUID(),
            nickname = "following",
            followerCount = 0
        )
    }

    @Test
    @DisplayName("팔로우 성공")
    fun follow_Success() {
        // Given
        val followerId = UUID.randomUUID()
        val followingId = UUID.randomUUID()

        val follow = Follow(
            id = null,
            followerId = followerId,
            followingId = followingId
        )

        every { userService.getUserById(followerId) } returns followerUser
        every { userService.getUserById(followingId) } returns followingUser
        every {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        } returns false
        every { followRepository.save(any()) } returns follow
        every { userProfileService.incrementFollowingCount(followerId) } returns
                followerProfile.copy(followingCount = 1)
        every { userProfileService.incrementFollowerCount(followingId) } returns
                followingProfile.copy(followerCount = 1)

        // When
        val result = followService.follow(followerId, followingId)

        // Then
        assertEquals(follow, result)
        verify(exactly = 1) { userService.getUserById(followerId) }
        verify(exactly = 1) { userService.getUserById(followingId) }
        verify(exactly = 1) {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        }
        verify(exactly = 1) { followRepository.save(any()) }
        verify(exactly = 1) { userProfileService.incrementFollowingCount(followerId) }
        verify(exactly = 1) { userProfileService.incrementFollowerCount(followingId) }
    }

    @Test
    @DisplayName("팔로우 실패 - 자기 자신 팔로우")
    fun follow_SelfFollow_ThrowsException() {
        // Given
        val userId = UUID.randomUUID()

        // When & Then
        assertThrows<CannotFollowSelfException> {
            followService.follow(userId, userId)
        }

        verify(exactly = 0) { userService.getUserById(any()) }
        verify(exactly = 0) { followRepository.save(any()) }
    }

    @Test
    @DisplayName("팔로우 실패 - 이미 팔로우 중")
    fun follow_AlreadyFollowing_ThrowsException() {
        // Given
        val followerId = UUID.randomUUID()
        val followingId = UUID.randomUUID()

        every { userService.getUserById(followerId) } returns followerUser
        every { userService.getUserById(followingId) } returns followingUser
        every {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        } returns true

        // When & Then
        val exception = assertThrows<AlreadyFollowingException> {
            followService.follow(followerId, followingId)
        }

        assertEquals(followingId, exception.followingId)
        verify(exactly = 1) { userService.getUserById(followerId) }
        verify(exactly = 1) { userService.getUserById(followingId) }
        verify(exactly = 1) {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        }
        verify(exactly = 0) { followRepository.save(any()) }
    }

    @Test
    @DisplayName("언팔로우 성공")
    fun unfollow_Success() {
        // Given
        val followerId = UUID.randomUUID()
        val followingId = UUID.randomUUID()

        every { userService.getUserById(followerId) } returns followerUser
        every { userService.getUserById(followingId) } returns followingUser
        every {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        } returns true
        every { followRepository.softDelete(followerId, followingId) } returns Unit
        every { userProfileService.decrementFollowingCount(followerId) } returns
                followerProfile.copy(followingCount = 0)
        every { userProfileService.decrementFollowerCount(followingId) } returns
                followingProfile.copy(followerCount = 0)

        // When
        followService.unfollow(followerId, followingId)

        // Then
        verify(exactly = 1) { userService.getUserById(followerId) }
        verify(exactly = 1) { userService.getUserById(followingId) }
        verify(exactly = 1) {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        }
        verify(exactly = 1) { followRepository.softDelete(followerId, followingId) }
        verify(exactly = 1) { userProfileService.decrementFollowingCount(followerId) }
        verify(exactly = 1) { userProfileService.decrementFollowerCount(followingId) }
    }

    @Test
    @DisplayName("언팔로우 실패 - 팔로우하지 않음")
    fun unfollow_NotFollowing_ThrowsException() {
        // Given
        val followerId = UUID.randomUUID()
        val followingId = UUID.randomUUID()

        every { userService.getUserById(followerId) } returns followerUser
        every { userService.getUserById(followingId) } returns followingUser
        every {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        } returns false

        // When & Then
        val exception = assertThrows<NotFollowingException> {
            followService.unfollow(followerId, followingId)
        }

        assertEquals(followingId, exception.followingId)
        verify(exactly = 1) { userService.getUserById(followerId) }
        verify(exactly = 1) { userService.getUserById(followingId) }
        verify(exactly = 1) {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        }
        verify(exactly = 0) { followRepository.softDelete(any(), any()) }
    }

    @Test
    @DisplayName("팔로우 관계 확인 - 팔로우 중")
    fun isFollowing_Following_ReturnsTrue() {
        // Given
        val followerId = UUID.randomUUID()
        val followingId = UUID.randomUUID()

        every {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        } returns true

        // When
        val result = followService.isFollowing(followerId, followingId)

        // Then
        assertTrue(result)
        verify(exactly = 1) {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        }
    }

    @Test
    @DisplayName("팔로우 관계 확인 - 팔로우하지 않음")
    fun isFollowing_NotFollowing_ReturnsFalse() {
        // Given
        val followerId = UUID.randomUUID()
        val followingId = UUID.randomUUID()

        every {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        } returns false

        // When
        val result = followService.isFollowing(followerId, followingId)

        // Then
        assertFalse(result)
        verify(exactly = 1) {
            followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)
        }
    }

    @Test
    @DisplayName("팔로잉 수 조회")
    fun getFollowingCount_ReturnsCount() {
        // Given
        val userId = UUID.randomUUID()
        val count = 5

        every { followRepository.countByFollowerId(userId) } returns count

        // When
        val result = followService.getFollowingCount(userId)

        // Then
        assertEquals(count, result)
        verify(exactly = 1) { followRepository.countByFollowerId(userId) }
    }

    @Test
    @DisplayName("팔로워 수 조회")
    fun getFollowerCount_ReturnsCount() {
        // Given
        val userId = UUID.randomUUID()
        val count = 10

        every { followRepository.countByFollowingId(userId) } returns count

        // When
        val result = followService.getFollowerCount(userId)

        // Then
        assertEquals(count, result)
        verify(exactly = 1) { followRepository.countByFollowingId(userId) }
    }
}
