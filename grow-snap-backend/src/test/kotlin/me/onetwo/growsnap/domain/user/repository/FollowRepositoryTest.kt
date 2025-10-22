package me.onetwo.growsnap.domain.user.repository

import java.util.UUID

import me.onetwo.growsnap.domain.user.model.Follow
import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * FollowRepository 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("팔로우 Repository 테스트")
class FollowRepositoryTest {

    @Autowired
    private lateinit var followRepository: FollowRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var follower: User
    private lateinit var following: User
    private lateinit var anotherUser: User

    @BeforeEach
    fun setUp() {
        // 테스트 사용자들 생성
        follower = userRepository.save(
            User(
                email = "follower@example.com",
                provider = OAuthProvider.GOOGLE,
                providerId = "follower-google-123",
                role = UserRole.USER
            )
        )

        following = userRepository.save(
            User(
                email = "following@example.com",
                provider = OAuthProvider.GOOGLE,
                providerId = "following-google-456",
                role = UserRole.USER
            )
        )

        anotherUser = userRepository.save(
            User(
                email = "another@example.com",
                provider = OAuthProvider.GOOGLE,
                providerId = "another-google-789",
                role = UserRole.USER
            )
        )
    }

    @Test
    @DisplayName("팔로우 저장 성공")
    fun save_Success() {
        // Given
        val follow = Follow(
            followerId = follower.id!!,
            followingId = following.id!!
        )

        // When
        val savedFollow = followRepository.save(follow)

        // Then
        assertNotNull(savedFollow.id)
        assertEquals(follower.id, savedFollow.followerId)
        assertEquals(following.id, savedFollow.followingId)
    }

    @Test
    @DisplayName("팔로우 삭제 성공")
    fun delete_ExistingFollow_Success() {
        // Given
        val follow = Follow(
            followerId = follower.id!!,
            followingId = following.id!!
        )
        followRepository.save(follow)

        // When
        followRepository.delete(follower.id!!, following.id!!)

        // Then
        val exists = followRepository.existsByFollowerIdAndFollowingId(follower.id!!, following.id!!)
        assertFalse(exists)
    }

    @Test
    @DisplayName("팔로우 관계 존재 확인 - 존재하는 경우")
    fun existsByFollowerIdAndFollowingId_ExistingFollow_ReturnsTrue() {
        // Given
        val follow = Follow(
            followerId = follower.id!!,
            followingId = following.id!!
        )
        followRepository.save(follow)

        // When
        val exists = followRepository.existsByFollowerIdAndFollowingId(follower.id!!, following.id!!)

        // Then
        assertTrue(exists)
    }

    @Test
    @DisplayName("팔로우 관계 존재 확인 - 존재하지 않는 경우")
    fun existsByFollowerIdAndFollowingId_NonExistingFollow_ReturnsFalse() {
        // When
        val exists = followRepository.existsByFollowerIdAndFollowingId(follower.id!!, following.id!!)

        // Then
        assertFalse(exists)
    }

    @Test
    @DisplayName("팔로워 ID로 팔로잉 수 조회 - 여러 명 팔로우")
    fun countByFollowerId_MultipleFollowing_ReturnsCorrectCount() {
        // Given
        followRepository.save(Follow(followerId = follower.id!!, followingId = following.id!!))
        followRepository.save(Follow(followerId = follower.id!!, followingId = anotherUser.id!!))

        // When
        val count = followRepository.countByFollowerId(follower.id!!)

        // Then
        assertEquals(2, count)
    }

    @Test
    @DisplayName("팔로워 ID로 팔로잉 수 조회 - 팔로우 없음")
    fun countByFollowerId_NoFollowing_ReturnsZero() {
        // When
        val count = followRepository.countByFollowerId(follower.id!!)

        // Then
        assertEquals(0, count)
    }

    @Test
    @DisplayName("팔로잉 ID로 팔로워 수 조회 - 여러 명의 팔로워")
    fun countByFollowingId_MultipleFollowers_ReturnsCorrectCount() {
        // Given
        followRepository.save(Follow(followerId = follower.id!!, followingId = following.id!!))
        followRepository.save(Follow(followerId = anotherUser.id!!, followingId = following.id!!))

        // When
        val count = followRepository.countByFollowingId(following.id!!)

        // Then
        assertEquals(2, count)
    }

    @Test
    @DisplayName("팔로잉 ID로 팔로워 수 조회 - 팔로워 없음")
    fun countByFollowingId_NoFollowers_ReturnsZero() {
        // When
        val count = followRepository.countByFollowingId(following.id!!)

        // Then
        assertEquals(0, count)
    }

    @Test
    @DisplayName("중복 팔로우 시도 - 예외 발생")
    fun save_DuplicateFollow_ThrowsException() {
        // Given
        val follow = Follow(
            followerId = follower.id!!,
            followingId = following.id!!
        )
        followRepository.save(follow)

        // When & Then
        val duplicateFollow = Follow(
            followerId = follower.id!!,
            followingId = following.id!!
        )
        assertThrows(Exception::class.java) {
            followRepository.save(duplicateFollow)
        }
    }

    @Test
    @DisplayName("팔로우 삭제 후 카운트 확인")
    fun delete_ThenCheckCount_ReturnsDecrementedCount() {
        // Given
        followRepository.save(Follow(followerId = follower.id!!, followingId = following.id!!))
        followRepository.save(Follow(followerId = follower.id!!, followingId = anotherUser.id!!))

        // When
        followRepository.delete(follower.id!!, following.id!!)
        val countAfterDelete = followRepository.countByFollowerId(follower.id!!)

        // Then
        assertEquals(1, countAfterDelete)
    }

    @Test
    @DisplayName("존재하지 않는 팔로우 삭제 시도 - 아무 일도 일어나지 않음")
    fun delete_NonExistingFollow_NoError() {
        // When & Then (예외가 발생하지 않아야 함)
        assertDoesNotThrow {
            followRepository.delete(follower.id!!, following.id!!)
        }
    }

    @Test
    @DisplayName("양방향 팔로우 확인")
    fun save_BidirectionalFollow_Success() {
        // Given & When
        followRepository.save(Follow(followerId = follower.id!!, followingId = following.id!!))
        followRepository.save(Follow(followerId = following.id!!, followingId = follower.id!!))

        // Then
        assertTrue(followRepository.existsByFollowerIdAndFollowingId(follower.id!!, following.id!!))
        assertTrue(followRepository.existsByFollowerIdAndFollowingId(following.id!!, follower.id!!))
        assertEquals(1, followRepository.countByFollowerId(follower.id!!))
        assertEquals(1, followRepository.countByFollowingId(follower.id!!))
    }
}
