package me.onetwo.growsnap.domain.user.service

import java.util.UUID

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.onetwo.growsnap.domain.user.exception.DuplicateNicknameException
import me.onetwo.growsnap.domain.user.exception.UserProfileNotFoundException
import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserProfile
import me.onetwo.growsnap.domain.user.model.UserRole
import me.onetwo.growsnap.domain.user.repository.UserProfileRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

/**
 * UserProfileService 단위 테스트
 */
@ExtendWith(MockKExtension::class)
@DisplayName("사용자 프로필 서비스 테스트")
class UserProfileServiceTest {

    private lateinit var userProfileRepository: UserProfileRepository
    private lateinit var userService: UserService
    private lateinit var userProfileService: UserProfileService

    private lateinit var testUser: User
    private lateinit var testProfile: UserProfile

    @BeforeEach
    fun setUp() {
        userProfileRepository = mockk()
        userService = mockk()
        userProfileService = UserProfileServiceImpl(userProfileRepository, userService)

        val testUserId = UUID.randomUUID()

        testUser = User(
            id = testUserId,
            email = "test@example.com",
            provider = OAuthProvider.GOOGLE,
            providerId = "google-123",
            role = UserRole.USER
        )

        testProfile = UserProfile(
            id = 1L,
            userId = testUserId,
            nickname = "testnick",
            profileImageUrl = "https://example.com/profile.jpg",
            bio = "테스트 자기소개"
        )
    }

    @Test
    @DisplayName("프로필 생성 성공")
    fun createProfile_Success() {
        // Given
        val userId = UUID.randomUUID()
        val nickname = "newnick"

        every { userService.getUserById(userId) } returns testUser
        every { userProfileRepository.existsByNickname(nickname) } returns false
        every { userProfileRepository.save(any()) } returns testProfile.copy(nickname = nickname)

        // When
        val result = userProfileService.createProfile(userId, nickname)

        // Then
        assertEquals(nickname, result.nickname)
        verify(exactly = 1) { userService.getUserById(userId) }
        verify(exactly = 1) { userProfileRepository.existsByNickname(nickname) }
        verify(exactly = 1) { userProfileRepository.save(any()) }
    }

    @Test
    @DisplayName("프로필 생성 실패 - 중복 닉네임")
    fun createProfile_DuplicateNickname_ThrowsException() {
        // Given
        val userId = UUID.randomUUID()
        val nickname = "duplicatenick"

        every { userService.getUserById(userId) } returns testUser
        every { userProfileRepository.existsByNickname(nickname) } returns true

        // When & Then
        val exception = assertThrows<DuplicateNicknameException> {
            userProfileService.createProfile(userId, nickname)
        }

        assertEquals(nickname, exception.nickname)
        verify(exactly = 1) { userService.getUserById(userId) }
        verify(exactly = 1) { userProfileRepository.existsByNickname(nickname) }
        verify(exactly = 0) { userProfileRepository.save(any()) }
    }

    @Test
    @DisplayName("사용자 ID로 프로필 조회 성공")
    fun getProfileByUserId_ExistingProfile_ReturnsProfile() {
        // Given
        val userId = UUID.randomUUID()

        every { userProfileRepository.findByUserId(userId) } returns testProfile

        // When
        val result = userProfileService.getProfileByUserId(userId)

        // Then
        assertEquals(testProfile, result)
        verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
    }

    @Test
    @DisplayName("사용자 ID로 프로필 조회 실패")
    fun getProfileByUserId_NonExistingProfile_ThrowsException() {
        // Given
        val userId = UUID.randomUUID()

        every { userProfileRepository.findByUserId(userId) } returns null

        // When & Then
        val exception = assertThrows<UserProfileNotFoundException> {
            userProfileService.getProfileByUserId(userId)
        }

        assertTrue(exception.message!!.contains("$userId"))
        verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
    }

    @Test
    @DisplayName("닉네임으로 프로필 조회 성공")
    fun getProfileByNickname_ExistingProfile_ReturnsProfile() {
        // Given
        val nickname = "testnick"

        every { userProfileRepository.findByNickname(nickname) } returns testProfile

        // When
        val result = userProfileService.getProfileByNickname(nickname)

        // Then
        assertEquals(testProfile, result)
        verify(exactly = 1) { userProfileRepository.findByNickname(nickname) }
    }

    @Test
    @DisplayName("닉네임으로 프로필 조회 실패")
    fun getProfileByNickname_NonExistingProfile_ThrowsException() {
        // Given
        val nickname = "nonexistent"

        every { userProfileRepository.findByNickname(nickname) } returns null

        // When & Then
        val exception = assertThrows<UserProfileNotFoundException> {
            userProfileService.getProfileByNickname(nickname)
        }

        assertTrue(exception.message!!.contains(nickname))
        verify(exactly = 1) { userProfileRepository.findByNickname(nickname) }
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 중복됨")
    fun isNicknameDuplicated_ExistingNickname_ReturnsTrue() {
        // Given
        val nickname = "existing"

        every { userProfileRepository.existsByNickname(nickname) } returns true

        // When
        val result = userProfileService.isNicknameDuplicated(nickname)

        // Then
        assertTrue(result)
        verify(exactly = 1) { userProfileRepository.existsByNickname(nickname) }
    }

    @Test
    @DisplayName("닉네임 중복 확인 - 중복되지 않음")
    fun isNicknameDuplicated_NonExistingNickname_ReturnsFalse() {
        // Given
        val nickname = "new"

        every { userProfileRepository.existsByNickname(nickname) } returns false

        // When
        val result = userProfileService.isNicknameDuplicated(nickname)

        // Then
        assertFalse(result)
        verify(exactly = 1) { userProfileRepository.existsByNickname(nickname) }
    }

    @Test
    @DisplayName("프로필 업데이트 - 닉네임 변경 성공")
    fun updateProfile_ChangeNickname_Success() {
        // Given
        val userId = UUID.randomUUID()
        val newNickname = "newnick"

        val updatedProfile = testProfile.copy(nickname = newNickname)

        every { userProfileRepository.findByUserId(userId) } returns testProfile
        every { userProfileRepository.existsByNickname(newNickname) } returns false
        every { userProfileRepository.update(any()) } returns updatedProfile

        // When
        val result = userProfileService.updateProfile(userId, nickname = newNickname)

        // Then
        assertEquals(newNickname, result.nickname)
        verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
        verify(exactly = 1) { userProfileRepository.existsByNickname(newNickname) }
        verify(exactly = 1) { userProfileRepository.update(any()) }
    }

    @Test
    @DisplayName("프로필 업데이트 - 닉네임 중복으로 실패")
    fun updateProfile_DuplicateNickname_ThrowsException() {
        // Given
        val userId = UUID.randomUUID()
        val newNickname = "duplicatenick"

        every { userProfileRepository.findByUserId(userId) } returns testProfile
        every { userProfileRepository.existsByNickname(newNickname) } returns true

        // When & Then
        val exception = assertThrows<DuplicateNicknameException> {
            userProfileService.updateProfile(userId, nickname = newNickname)
        }

        assertEquals(newNickname, exception.nickname)
        verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
        verify(exactly = 1) { userProfileRepository.existsByNickname(newNickname) }
        verify(exactly = 0) { userProfileRepository.update(any()) }
    }

    @Test
    @DisplayName("프로필 업데이트 - 같은 닉네임으로 변경 시 중복 검사 안 함")
    fun updateProfile_SameNickname_DoesNotCheckDuplicate() {
        // Given
        val userId = UUID.randomUUID()
        val sameNickname = testProfile.nickname

        every { userProfileRepository.findByUserId(userId) } returns testProfile
        every { userProfileRepository.update(any()) } returns testProfile

        // When
        val result = userProfileService.updateProfile(userId, nickname = sameNickname)

        // Then
        assertEquals(sameNickname, result.nickname)
        verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
        verify(exactly = 0) { userProfileRepository.existsByNickname(any()) }
        verify(exactly = 1) { userProfileRepository.update(any()) }
    }

    @Test
    @DisplayName("팔로워 수 증가")
    fun incrementFollowerCount_Success() {
        // Given
        val userId = UUID.randomUUID()
        val updatedProfile = testProfile.copy(followerCount = testProfile.followerCount + 1)

        every { userProfileRepository.findByUserId(userId) } returns testProfile
        every { userProfileRepository.update(any()) } returns updatedProfile

        // When
        val result = userProfileService.incrementFollowerCount(userId)

        // Then
        assertEquals(testProfile.followerCount + 1, result.followerCount)
        verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
        verify(exactly = 1) { userProfileRepository.update(any()) }
    }

    @Test
    @DisplayName("팔로워 수 감소")
    fun decrementFollowerCount_Success() {
        // Given
        val userId = UUID.randomUUID()
        val profileWithFollowers = testProfile.copy(followerCount = 5)
        val updatedProfile = profileWithFollowers.copy(followerCount = 4)

        every { userProfileRepository.findByUserId(userId) } returns profileWithFollowers
        every { userProfileRepository.update(any()) } returns updatedProfile

        // When
        val result = userProfileService.decrementFollowerCount(userId)

        // Then
        assertEquals(4, result.followerCount)
        verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
        verify(exactly = 1) { userProfileRepository.update(any()) }
    }

    @Test
    @DisplayName("팔로워 수 감소 - 0 이하로 내려가지 않음")
    fun decrementFollowerCount_DoesNotGoBelowZero() {
        // Given
        val userId = UUID.randomUUID()
        val profileWithZeroFollowers = testProfile.copy(followerCount = 0)

        every { userProfileRepository.findByUserId(userId) } returns profileWithZeroFollowers
        every { userProfileRepository.update(any()) } returns profileWithZeroFollowers

        // When
        val result = userProfileService.decrementFollowerCount(userId)

        // Then
        assertEquals(0, result.followerCount)
        verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
        verify(exactly = 1) { userProfileRepository.update(any()) }
    }

    @Test
    @DisplayName("팔로잉 수 증가")
    fun incrementFollowingCount_Success() {
        // Given
        val userId = UUID.randomUUID()
        val updatedProfile = testProfile.copy(followingCount = testProfile.followingCount + 1)

        every { userProfileRepository.findByUserId(userId) } returns testProfile
        every { userProfileRepository.update(any()) } returns updatedProfile

        // When
        val result = userProfileService.incrementFollowingCount(userId)

        // Then
        assertEquals(testProfile.followingCount + 1, result.followingCount)
        verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
        verify(exactly = 1) { userProfileRepository.update(any()) }
    }

    @Test
    @DisplayName("팔로잉 수 감소")
    fun decrementFollowingCount_Success() {
        // Given
        val userId = UUID.randomUUID()
        val profileWithFollowing = testProfile.copy(followingCount = 3)
        val updatedProfile = profileWithFollowing.copy(followingCount = 2)

        every { userProfileRepository.findByUserId(userId) } returns profileWithFollowing
        every { userProfileRepository.update(any()) } returns updatedProfile

        // When
        val result = userProfileService.decrementFollowingCount(userId)

        // Then
        assertEquals(2, result.followingCount)
        verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
        verify(exactly = 1) { userProfileRepository.update(any()) }
    }

    @Test
    @DisplayName("팔로잉 수 감소 - 0 이하로 내려가지 않음")
    fun decrementFollowingCount_DoesNotGoBelowZero() {
        // Given
        val userId = UUID.randomUUID()
        val profileWithZeroFollowing = testProfile.copy(followingCount = 0)

        every { userProfileRepository.findByUserId(userId) } returns profileWithZeroFollowing
        every { userProfileRepository.update(any()) } returns profileWithZeroFollowing

        // When
        val result = userProfileService.decrementFollowingCount(userId)

        // Then
        assertEquals(0, result.followingCount)
        verify(exactly = 1) { userProfileRepository.findByUserId(userId) }
        verify(exactly = 1) { userProfileRepository.update(any()) }
    }
}
