package me.onetwo.growsnap.domain.user.service

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.onetwo.growsnap.domain.user.exception.UserNotFoundException
import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserRole
import me.onetwo.growsnap.domain.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

/**
 * UserService 단위 테스트
 */
@ExtendWith(MockKExtension::class)
@DisplayName("사용자 서비스 테스트")
class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userService = UserService(userRepository)
    }

    @Test
    @DisplayName("OAuth 사용자 조회 또는 생성 - 기존 사용자 반환")
    fun findOrCreateOAuthUser_ExistingUser_ReturnsUser() {
        // Given
        val email = "test@example.com"
        val provider = OAuthProvider.GOOGLE
        val providerId = "google-123"

        val existingUser = User(
            id = 1L,
            email = email,
            provider = provider,
            providerId = providerId,
            role = UserRole.USER
        )

        every {
            userRepository.findByProviderAndProviderId(provider, providerId)
        } returns existingUser

        // When
        val result = userService.findOrCreateOAuthUser(email, provider, providerId)

        // Then
        assertEquals(existingUser, result)
        verify(exactly = 1) {
            userRepository.findByProviderAndProviderId(provider, providerId)
        }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    @DisplayName("OAuth 사용자 조회 또는 생성 - 신규 사용자 생성")
    fun findOrCreateOAuthUser_NewUser_CreatesUser() {
        // Given
        val email = "new@example.com"
        val provider = OAuthProvider.GOOGLE
        val providerId = "google-456"

        val newUser = User(
            id = 2L,
            email = email,
            provider = provider,
            providerId = providerId,
            role = UserRole.USER,
            isCreator = false
        )

        every {
            userRepository.findByProviderAndProviderId(provider, providerId)
        } returns null

        every { userRepository.save(any()) } returns newUser

        // When
        val result = userService.findOrCreateOAuthUser(email, provider, providerId)

        // Then
        assertEquals(newUser, result)
        verify(exactly = 1) {
            userRepository.findByProviderAndProviderId(provider, providerId)
        }
        verify(exactly = 1) { userRepository.save(any()) }
    }

    @Test
    @DisplayName("사용자 ID로 조회 성공")
    fun getUserById_ExistingUser_ReturnsUser() {
        // Given
        val userId = 1L
        val user = User(
            id = userId,
            email = "test@example.com",
            provider = OAuthProvider.GOOGLE,
            providerId = "google-123",
            role = UserRole.USER
        )

        every { userRepository.findById(userId) } returns user

        // When
        val result = userService.getUserById(userId)

        // Then
        assertEquals(user, result)
        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    @DisplayName("사용자 ID로 조회 실패 - UserNotFoundException 발생")
    fun getUserById_NonExistingUser_ThrowsException() {
        // Given
        val userId = 999L

        every { userRepository.findById(userId) } returns null

        // When & Then
        val exception = assertThrows<UserNotFoundException> {
            userService.getUserById(userId)
        }

        assertTrue(exception.message!!.contains("$userId"))
        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    @DisplayName("이메일로 조회 성공")
    fun getUserByEmail_ExistingUser_ReturnsUser() {
        // Given
        val email = "test@example.com"
        val user = User(
            id = 1L,
            email = email,
            provider = OAuthProvider.GOOGLE,
            providerId = "google-123",
            role = UserRole.USER
        )

        every { userRepository.findByEmail(email) } returns user

        // When
        val result = userService.getUserByEmail(email)

        // Then
        assertEquals(user, result)
        verify(exactly = 1) { userRepository.findByEmail(email) }
    }

    @Test
    @DisplayName("이메일로 조회 실패 - UserNotFoundException 발생")
    fun getUserByEmail_NonExistingUser_ThrowsException() {
        // Given
        val email = "nonexistent@example.com"

        every { userRepository.findByEmail(email) } returns null

        // When & Then
        val exception = assertThrows<UserNotFoundException> {
            userService.getUserByEmail(email)
        }

        assertTrue(exception.message!!.contains(email))
        verify(exactly = 1) { userRepository.findByEmail(email) }
    }

    @Test
    @DisplayName("이메일 중복 확인 - 중복됨")
    fun isEmailDuplicated_ExistingEmail_ReturnsTrue() {
        // Given
        val email = "existing@example.com"
        val user = User(
            id = 1L,
            email = email,
            provider = OAuthProvider.GOOGLE,
            providerId = "google-123",
            role = UserRole.USER
        )

        every { userRepository.findByEmail(email) } returns user

        // When
        val result = userService.isEmailDuplicated(email)

        // Then
        assertTrue(result)
        verify(exactly = 1) { userRepository.findByEmail(email) }
    }

    @Test
    @DisplayName("이메일 중복 확인 - 중복되지 않음")
    fun isEmailDuplicated_NonExistingEmail_ReturnsFalse() {
        // Given
        val email = "new@example.com"

        every { userRepository.findByEmail(email) } returns null

        // When
        val result = userService.isEmailDuplicated(email)

        // Then
        assertFalse(result)
        verify(exactly = 1) { userRepository.findByEmail(email) }
    }

    @Test
    @DisplayName("크리에이터 전환 성공")
    fun convertToCreator_ExistingUser_ConvertsToCreator() {
        // Given
        val userId = 1L
        val user = User(
            id = userId,
            email = "test@example.com",
            provider = OAuthProvider.GOOGLE,
            providerId = "google-123",
            role = UserRole.USER,
            isCreator = false
        )

        val creatorUser = user.copy(
            role = UserRole.CREATOR,
            isCreator = true
        )

        every { userRepository.findById(userId) } returns user
        every { userRepository.update(any()) } returns creatorUser

        // When
        val result = userService.convertToCreator(userId)

        // Then
        assertEquals(UserRole.CREATOR, result.role)
        assertTrue(result.isCreator)
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { userRepository.update(any()) }
    }

    @Test
    @DisplayName("크리에이터 전환 - 이미 크리에이터인 경우")
    fun convertToCreator_AlreadyCreator_ReturnsUser() {
        // Given
        val userId = 1L
        val creatorUser = User(
            id = userId,
            email = "test@example.com",
            provider = OAuthProvider.GOOGLE,
            providerId = "google-123",
            role = UserRole.CREATOR,
            isCreator = true
        )

        every { userRepository.findById(userId) } returns creatorUser

        // When
        val result = userService.convertToCreator(userId)

        // Then
        assertEquals(creatorUser, result)
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 0) { userRepository.update(any()) }
    }

    @Test
    @DisplayName("사용자 정보 업데이트 성공")
    fun updateUser_ExistingUser_UpdatesUser() {
        // Given
        val userId = 1L
        val user = User(
            id = userId,
            email = "test@example.com",
            provider = OAuthProvider.GOOGLE,
            providerId = "google-123",
            role = UserRole.CREATOR,
            isCreator = true
        )

        every { userRepository.findById(userId) } returns user
        every { userRepository.update(user) } returns user

        // When
        val result = userService.updateUser(user)

        // Then
        assertEquals(user, result)
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { userRepository.update(user) }
    }

    @Test
    @DisplayName("사용자 정보 업데이트 실패 - 사용자 없음")
    fun updateUser_NonExistingUser_ThrowsException() {
        // Given
        val userId = 999L
        val user = User(
            id = userId,
            email = "test@example.com",
            provider = OAuthProvider.GOOGLE,
            providerId = "google-123",
            role = UserRole.USER
        )

        every { userRepository.findById(userId) } returns null

        // When & Then
        assertThrows<UserNotFoundException> {
            userService.updateUser(user)
        }

        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 0) { userRepository.update(any()) }
    }
}
