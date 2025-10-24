package me.onetwo.growsnap.domain.auth.service

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import me.onetwo.growsnap.domain.auth.dto.RefreshTokenResponse
import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserRole
import me.onetwo.growsnap.domain.user.service.UserService
import me.onetwo.growsnap.infrastructure.redis.RefreshTokenRepository
import me.onetwo.growsnap.infrastructure.security.jwt.JwtTokenProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

/**
 * AuthService 단위 테스트
 */
@ExtendWith(MockKExtension::class)
@DisplayName("인증 Service 테스트")
class AuthServiceTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var userService: UserService
    private lateinit var authService: AuthService

    private lateinit var testUser: User
    private lateinit var testUserId: UUID

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = mockk()
        refreshTokenRepository = mockk()
        userService = mockk()
        authService = AuthServiceImpl(jwtTokenProvider, refreshTokenRepository, userService)

        testUserId = UUID.randomUUID()
        testUser = User(
            id = testUserId,
            email = "test@example.com",
            provider = OAuthProvider.GOOGLE,
            providerId = "google-123",
            role = UserRole.USER
        )
    }

    @Test
    @DisplayName("Access Token 갱신 성공")
    fun refreshAccessToken_ValidRefreshToken_ReturnsNewAccessToken() {
        // Given
        val refreshToken = "valid-refresh-token"
        val newAccessToken = "new-access-token"

        every { jwtTokenProvider.validateToken(refreshToken) } returns true
        every { jwtTokenProvider.getUserIdFromToken(refreshToken) } returns testUserId
        every { refreshTokenRepository.findByUserId(testUserId) } returns refreshToken
        every { userService.getUserById(testUserId) } returns testUser
        every {
            jwtTokenProvider.generateAccessToken(testUserId, testUser.email, testUser.role)
        } returns newAccessToken

        // When
        val result = authService.refreshAccessToken(refreshToken)

        // Then
        assertNotNull(result)
        assertEquals(newAccessToken, result.accessToken)

        verify(exactly = 1) { jwtTokenProvider.validateToken(refreshToken) }
        verify(exactly = 1) { jwtTokenProvider.getUserIdFromToken(refreshToken) }
        verify(exactly = 1) { refreshTokenRepository.findByUserId(testUserId) }
        verify(exactly = 1) { userService.getUserById(testUserId) }
        verify(exactly = 1) {
            jwtTokenProvider.generateAccessToken(testUserId, testUser.email, testUser.role)
        }
    }

    @Test
    @DisplayName("Access Token 갱신 실패 - 유효하지 않은 Refresh Token")
    fun refreshAccessToken_InvalidToken_ThrowsException() {
        // Given
        val invalidToken = "invalid-token"

        every { jwtTokenProvider.validateToken(invalidToken) } returns false

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.refreshAccessToken(invalidToken)
        }

        assertEquals("유효하지 않은 Refresh Token입니다", exception.message)
        verify(exactly = 1) { jwtTokenProvider.validateToken(invalidToken) }
        verify(exactly = 0) { jwtTokenProvider.getUserIdFromToken(any()) }
    }

    @Test
    @DisplayName("Access Token 갱신 실패 - Redis에 저장된 토큰 없음")
    fun refreshAccessToken_TokenNotFoundInRedis_ThrowsException() {
        // Given
        val refreshToken = "valid-token"

        every { jwtTokenProvider.validateToken(refreshToken) } returns true
        every { jwtTokenProvider.getUserIdFromToken(refreshToken) } returns testUserId
        every { refreshTokenRepository.findByUserId(testUserId) } returns null

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.refreshAccessToken(refreshToken)
        }

        assertEquals("Refresh Token을 찾을 수 없습니다", exception.message)
        verify(exactly = 1) { jwtTokenProvider.validateToken(refreshToken) }
        verify(exactly = 1) { jwtTokenProvider.getUserIdFromToken(refreshToken) }
        verify(exactly = 1) { refreshTokenRepository.findByUserId(testUserId) }
    }

    @Test
    @DisplayName("Access Token 갱신 실패 - Redis 토큰과 불일치")
    fun refreshAccessToken_TokenMismatch_ThrowsException() {
        // Given
        val requestToken = "valid-token"
        val storedToken = "different-token"

        every { jwtTokenProvider.validateToken(requestToken) } returns true
        every { jwtTokenProvider.getUserIdFromToken(requestToken) } returns testUserId
        every { refreshTokenRepository.findByUserId(testUserId) } returns storedToken

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.refreshAccessToken(requestToken)
        }

        assertEquals("Refresh Token이 일치하지 않습니다", exception.message)
        verify(exactly = 1) { jwtTokenProvider.validateToken(requestToken) }
        verify(exactly = 1) { jwtTokenProvider.getUserIdFromToken(requestToken) }
        verify(exactly = 1) { refreshTokenRepository.findByUserId(testUserId) }
    }

    @Test
    @DisplayName("로그아웃 성공")
    fun logout_ValidRefreshToken_DeletesToken() {
        // Given
        val refreshToken = "valid-refresh-token"

        every { jwtTokenProvider.validateToken(refreshToken) } returns true
        every { jwtTokenProvider.getUserIdFromToken(refreshToken) } returns testUserId
        every { refreshTokenRepository.deleteByUserId(testUserId) } returns Unit

        // When
        authService.logout(refreshToken)

        // Then
        verify(exactly = 1) { jwtTokenProvider.validateToken(refreshToken) }
        verify(exactly = 1) { jwtTokenProvider.getUserIdFromToken(refreshToken) }
        verify(exactly = 1) { refreshTokenRepository.deleteByUserId(testUserId) }
    }

    @Test
    @DisplayName("로그아웃 실패 - 유효하지 않은 Refresh Token")
    fun logout_InvalidToken_ThrowsException() {
        // Given
        val invalidToken = "invalid-token"

        every { jwtTokenProvider.validateToken(invalidToken) } returns false

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.logout(invalidToken)
        }

        assertEquals("유효하지 않은 Refresh Token입니다", exception.message)
        verify(exactly = 1) { jwtTokenProvider.validateToken(invalidToken) }
        verify(exactly = 0) { jwtTokenProvider.getUserIdFromToken(any()) }
        verify(exactly = 0) { refreshTokenRepository.deleteByUserId(any()) }
    }

    @Test
    @DisplayName("사용자 ID로 Refresh Token 조회 성공")
    fun getRefreshTokenByUserId_ExistingToken_ReturnsToken() {
        // Given
        val expectedToken = "stored-refresh-token"

        every { refreshTokenRepository.findByUserId(testUserId) } returns expectedToken

        // When
        val result = authService.getRefreshTokenByUserId(testUserId)

        // Then
        assertEquals(expectedToken, result)
        verify(exactly = 1) { refreshTokenRepository.findByUserId(testUserId) }
    }

    @Test
    @DisplayName("사용자 ID로 Refresh Token 조회 - 토큰 없음")
    fun getRefreshTokenByUserId_NoToken_ReturnsNull() {
        // Given
        every { refreshTokenRepository.findByUserId(testUserId) } returns null

        // When
        val result = authService.getRefreshTokenByUserId(testUserId)

        // Then
        assertEquals(null, result)
        verify(exactly = 1) { refreshTokenRepository.findByUserId(testUserId) }
    }
}
