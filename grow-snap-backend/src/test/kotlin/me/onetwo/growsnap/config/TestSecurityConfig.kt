package me.onetwo.growsnap.config

import io.mockk.every
import io.mockk.mockk
import me.onetwo.growsnap.domain.user.model.UserRole
import me.onetwo.growsnap.infrastructure.security.jwt.JwtAuthenticationWebFilter
import me.onetwo.growsnap.infrastructure.security.jwt.JwtTokenProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import java.util.UUID

/**
 * 테스트용 Security 설정
 *
 * JWT 인증을 mock으로 시뮬레이션하여 실제와 유사하게 테스트합니다.
 *
 * ### 테스트 인증 방식
 * 1. Authorization 헤더에 "Bearer {token}" 필요
 * 2. Mock JwtTokenProvider가 토큰을 항상 유효하다고 검증
 * 3. X-User-Id 헤더의 값을 userId로 사용 (테스트 편의성)
 *
 * ### 주의사항
 * - 실제 JWT 검증 로직은 mock으로 대체됨
 * - Authorization 헤더가 없으면 401 Unauthorized 반환
 * - 실제 환경과 유사하게 인증 필요 API는 토큰 필수
 */
@TestConfiguration
@EnableWebFluxSecurity
class TestSecurityConfig {

    /**
     * 테스트용 Mock JwtTokenProvider
     *
     * 모든 토큰을 유효하다고 간주하고, 토큰에서 userId를 추출합니다.
     *
     * ### 토큰 형식
     * - "test-token-{userId}": userId를 포함한 테스트 토큰
     * - 예: "test-token-550e8400-e29b-41d4-a716-446655440000"
     */
    @Bean
    @Primary
    fun mockJwtTokenProvider(): JwtTokenProvider {
        val mockProvider = mockk<JwtTokenProvider>(relaxed = true)

        // 모든 토큰을 유효하다고 간주
        every { mockProvider.validateToken(any()) } returns true

        // 토큰에서 userId 추출: "test-token-{uuid}" 형식
        every { mockProvider.getUserIdFromToken(any()) } answers {
            val token = firstArg<String>()
            try {
                // "test-token-" 접두사 제거 후 UUID 파싱
                val uuidStr = token.removePrefix("test-token-")
                UUID.fromString(uuidStr)
            } catch (e: Exception) {
                // 파싱 실패 시 기본 UUID 반환
                UUID.fromString("00000000-0000-0000-0000-000000000000")
            }
        }

        every { mockProvider.getEmailFromToken(any()) } returns "test@example.com"
        every { mockProvider.getRoleFromToken(any()) } returns UserRole.USER

        return mockProvider
    }

    /**
     * 테스트용 JwtAuthenticationWebFilter
     *
     * Mock JwtTokenProvider를 사용하는 JWT 인증 필터입니다.
     */
    @Bean
    fun testJwtAuthenticationWebFilter(jwtTokenProvider: JwtTokenProvider): JwtAuthenticationWebFilter {
        return JwtAuthenticationWebFilter(jwtTokenProvider)
    }

    /**
     * 테스트용 Security 필터 체인
     *
     * 실제 환경과 유사하게 JWT 인증을 요구하지만, mock으로 처리합니다.
     */
    @Bean
    fun testSecurityWebFilterChain(
        http: ServerHttpSecurity,
        jwtAuthenticationWebFilter: JwtAuthenticationWebFilter
    ): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange { authorize ->
                authorize
                    // 인증이 필요 없는 공개 API
                    .pathMatchers(
                        "/api/v1/auth/**",
                        "/oauth2/**",
                        "/login/**",
                        "/error"
                    ).permitAll()
                    // 조회 전용 공개 API (GET 메서드만 허용)
                    .pathMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/v1/users/*",                          // 사용자 ID로 조회
                        "/api/v1/profiles/*",                       // 프로필 ID로 조회 (UUID)
                        "/api/v1/profiles/nickname/*",              // 닉네임으로 프로필 조회
                        "/api/v1/profiles/check/nickname/*",        // 닉네임 중복 확인
                        "/api/v1/follows/stats/*"                   // 팔로우 통계 조회
                    ).permitAll()
                    // 나머지는 JWT 인증 필요
                    .anyExchange().authenticated()
            }
            .build()
    }
}
