package me.onetwo.growsnap.domain.user.repository

import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserRole
import org.jooq.DSLContext
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

    @Autowired
    private lateinit var dslContext: DSLContext

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // 테스트 데이터 초기화
        testUser = User(
            email = "test@example.com",
            provider = OAuthProvider.GOOGLE,
            providerId = "google-12345",
            role = UserRole.USER,
            isCreator = false
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
        assertEquals(testUser.isCreator, savedUser.isCreator)
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
        val foundUser = userRepository.findById(999999L)

        // Then
        assertNull(foundUser)
    }

    @Test
    @DisplayName("사용자 업데이트 성공 - 역할 변경")
    fun update_ChangeRole_Success() {
        // Given
        val savedUser = userRepository.save(testUser)
        val updatedUser = savedUser.copy(role = UserRole.CREATOR, isCreator = true)

        // When
        val result = userRepository.update(updatedUser)

        // Then
        assertEquals(UserRole.CREATOR, result.role)
        assertEquals(true, result.isCreator)

        // 실제 DB에서 확인
        val foundUser = userRepository.findById(savedUser.id!!)
        assertEquals(UserRole.CREATOR, foundUser?.role)
        assertEquals(true, foundUser?.isCreator)
    }

    @Test
    @DisplayName("사용자 업데이트 성공 - 크리에이터 전환")
    fun update_BecomeCreator_Success() {
        // Given
        val savedUser = userRepository.save(testUser)
        val updatedUser = savedUser.copy(isCreator = true)

        // When
        val result = userRepository.update(updatedUser)

        // Then
        assertEquals(true, result.isCreator)

        // 실제 DB에서 확인
        val foundUser = userRepository.findById(savedUser.id!!)
        assertEquals(true, foundUser?.isCreator)
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
