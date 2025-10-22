package me.onetwo.growsnap.infrastructure.security.oauth2

import me.onetwo.growsnap.infrastructure.redis.RefreshTokenRepository
import me.onetwo.growsnap.infrastructure.security.jwt.JwtTokenProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI

/**
 * OAuth2 인증 성공 핸들러
 *
 * OAuth2 로그인 성공 시 JWT 토큰을 생성하고
 * 프론트엔드로 리다이렉트합니다.
 *
 * @property jwtTokenProvider JWT 토큰 생성기
 * @property refreshTokenRepository Refresh Token 저장소
 * @property frontendUrl 프론트엔드 URL
 */
@Component
class OAuth2AuthenticationSuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${app.frontend-url:http://localhost:3000}")
    private val frontendUrl: String
) : ServerAuthenticationSuccessHandler {

    /**
     * OAuth2 인증 성공 시 처리
     *
     * 1. CustomOAuth2User에서 사용자 정보 추출
     * 2. JWT Access Token과 Refresh Token 생성
     * 3. Refresh Token을 Redis에 저장
     * 4. 프론트엔드로 리다이렉트 (토큰을 쿼리 파라미터로 전달)
     *
     * @param webFilterExchange WebFilterExchange
     * @param authentication 인증 정보
     * @return Mono<Void>
     */
    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        val customOAuth2User = authentication.principal as CustomOAuth2User

        // JWT 토큰 생성
        val accessToken = jwtTokenProvider.generateAccessToken(
            userId = customOAuth2User.userId,
            email = customOAuth2User.email,
            role = customOAuth2User.role
        )

        val refreshToken = jwtTokenProvider.generateRefreshToken(customOAuth2User.userId)

        // Refresh Token을 Redis에 저장
        refreshTokenRepository.save(customOAuth2User.userId, refreshToken)

        // 프론트엔드로 리다이렉트 (토큰을 쿼리 파라미터로 전달)
        val redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/auth/oauth2/redirect")
            .queryParam("accessToken", accessToken)
            .queryParam("refreshToken", refreshToken)
            .queryParam("userId", customOAuth2User.userId.toString())
            .queryParam("email", customOAuth2User.email)
            .build()
            .toUriString()

        val exchange = webFilterExchange.exchange
        exchange.response.statusCode = HttpStatus.FOUND
        exchange.response.headers.location = URI.create(redirectUrl)

        return exchange.response.setComplete()
    }
}
