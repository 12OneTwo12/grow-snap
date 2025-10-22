package me.onetwo.growsnap.infrastructure.security.jwt

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * JWT 인증 WebFilter
 *
 * HTTP 요청의 Authorization 헤더에서 JWT를 추출하여 검증하고,
 * 유효한 경우 사용자 정보를 SecurityContext에 저장합니다.
 *
 * ### 처리 흐름
 * 1. Authorization 헤더에서 "Bearer {token}" 추출
 * 2. JWT 유효성 검증 (JwtTokenProvider 사용)
 * 3. 유효한 경우: userId를 Request attribute에 저장 + SecurityContext 설정
 * 4. 유효하지 않은 경우: 401 Unauthorized 반환
 *
 * @property jwtTokenProvider JWT 토큰 검증 Provider
 */
@Component
class JwtAuthenticationWebFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : WebFilter {

    companion object {
        private const val BEARER_PREFIX = "Bearer "

        /**
         * 인증이 필요 없는 공개 경로
         */
        private val PUBLIC_PATHS = listOf(
            "/api/v1/auth/",
            "/oauth2/",
            "/login/",
            "/error"
        )
    }

    /**
     * JWT 인증 필터 로직
     *
     * @param exchange ServerWebExchange
     * @param chain WebFilterChain
     * @return Mono<Void>
     */
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.uri.path

        // 공개 경로는 JWT 검증 생략
        if (PUBLIC_PATHS.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        // Authorization 헤더에서 JWT 추출
        val token = extractToken(exchange)

        // Token이 없으면 SecurityConfig의 .permitAll() 규칙에 따라 통과시킴
        // (공개 API는 인증 없이 접근 가능)
        if (token == null) {
            return chain.filter(exchange)
        }

        // JWT 유효성 검증
        if (!jwtTokenProvider.validateToken(token)) {
            return unauthorizedResponse(exchange, "유효하지 않은 토큰입니다")
        }

        return try {
            // JWT에서 사용자 정보 추출
            val userId = jwtTokenProvider.getUserIdFromToken(token)
            val email = jwtTokenProvider.getEmailFromToken(token)
            val role = jwtTokenProvider.getRoleFromToken(token)

            // JwtAuthenticationToken 생성 및 SecurityContext에 저장
            val authentication = JwtAuthenticationToken(userId, email, role)

            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        } catch (e: Exception) {
            unauthorizedResponse(exchange, "토큰 처리 중 오류가 발생했습니다: ${e.message}")
        }
    }

    /**
     * Authorization 헤더에서 JWT 추출
     *
     * @param exchange ServerWebExchange
     * @return JWT 토큰 (없으면 null)
     */
    private fun extractToken(exchange: ServerWebExchange): String? {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?: return null

        return if (authHeader.startsWith(BEARER_PREFIX)) {
            authHeader.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }

    /**
     * 401 Unauthorized 응답 반환
     *
     * @param exchange ServerWebExchange
     * @param message 에러 메시지
     * @return Mono<Void>
     */
    private fun unauthorizedResponse(exchange: ServerWebExchange, message: String): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        exchange.response.headers.add("Content-Type", "application/json")

        val errorJson = """{"error":"Unauthorized","message":"$message"}"""
        val buffer = exchange.response.bufferFactory().wrap(errorJson.toByteArray())

        return exchange.response.writeWith(Mono.just(buffer))
    }
}
