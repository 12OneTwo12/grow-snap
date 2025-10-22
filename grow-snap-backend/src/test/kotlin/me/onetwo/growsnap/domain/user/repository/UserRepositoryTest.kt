package me.onetwo.growsnap.domain.user.repository

import java.util.UUID
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
 * UserRepository 통합 테스트
 *
 * 실제 데이터베이스(H2)를 사용하여 CRUD 기능을 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("사용자 Repository 테스트")
class UserRepositoryTest {

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // 테스트 데이터 초기화
        testUser = User(
            email = "test@example.com",
            provider = OAuthProvider.GOOGLE,
            providerId = "google-12345",
            role = UserRole.USER
        )
    }

    @Test
    @DisplayName("사용자 저장 성공")
    fun save_Success() {
        // When
        val savedUser = userRepository.save(testUser)

        // Then
        assertNotNull(savedUser.id)
        assertEquals(testUser.email, savedUser.email)
        assertEquals(testUser.provider, savedUser.provider)
        assertEquals(testUser.providerId, savedUser.providerId)
        assertEquals(testUser.role, savedUser.role)
    }

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    fun findByEmail_ExistingUser_ReturnsUser() {
        // Given
        val savedUser = userRepository.save(testUser)

        // When
        val foundUser = userRepository.findByEmail(testUser.email)

        // Then
        assertNotNull(foundUser)
        assertEquals(savedUser.id, foundUser?.id)
        assertEquals(savedUser.email, foundUser?.email)
    }

    @Test
    @DisplayName("이메일로 사용자 조회 - 존재하지 않는 경우")
    fun findByEmail_NonExistingUser_ReturnsNull() {
        // When
        val foundUser = userRepository.findByEmail("nonexistent@example.com")

        // Then
        assertNull(foundUser)
    }

    @Test
    @DisplayName("Provider와 Provider ID로 사용자 조회 성공")
    fun findByProviderAndProviderId_ExistingUser_ReturnsUser() {
        // Given
        val savedUser = userRepository.save(testUser)

        // When
        val foundUser = userRepository.findByProviderAndProviderId(
            OAuthProvider.GOOGLE,
            testUser.providerId
        )

        // Then
        assertNotNull(foundUser)
        assertEquals(savedUser.id, foundUser?.id)
        assertEquals(savedUser.providerId, foundUser?.providerId)
    }

    @Test
    @DisplayName("Provider와 Provider ID로 사용자 조회 - 존재하지 않는 경우")
    fun findByProviderAndProviderId_NonExistingUser_ReturnsNull() {
        // When
        val foundUser = userRepository.findByProviderAndProviderId(
            OAuthProvider.GOOGLE,
            "nonexistent-id"
        )

        // Then
        assertNull(foundUser)
    }

    @Test
    @DisplayName("ID로 사용자 조회 성공")
    fun findById_ExistingUser_ReturnsUser() {
        // Given
        val savedUser = userRepository.save(testUser)

        // When
        val foundUser = userRepository.findById(savedUser.id!!)

        // Then
        assertNotNull(foundUser)
        assertEquals(savedUser.id, foundUser?.id)
        assertEquals(savedUser.email, foundUser?.email)
    }

    @Test
    @DisplayName("ID로 사용자 조회 - 존재하지 않는 경우")
    fun findById_NonExistingUser_ReturnsNull() {
        // When
        val foundUser = userRepository.findById(UUID.randomUUID())

        // Then
        assertNull(foundUser)
    }

    @Test
    @DisplayName("중복 이메일로 저장 시도 - 예외 발생")
    fun save_DuplicateEmail_ThrowsException() {
        // Given
        userRepository.save(testUser)

        // When & Then
        val duplicateUser = testUser.copy()
        assertThrows(Exception::class.java) {
            userRepository.save(duplicateUser)
        }
    }

    @Test
    @DisplayName("중복 Provider와 Provider ID로 저장 시도 - 예외 발생")
    fun save_DuplicateProviderAndProviderId_ThrowsException() {
        // Given
        userRepository.save(testUser)

        // When & Then
        val duplicateUser = testUser.copy(email = "different@example.com")
        assertThrows(Exception::class.java) {
            userRepository.save(duplicateUser)
        }
    }
}
