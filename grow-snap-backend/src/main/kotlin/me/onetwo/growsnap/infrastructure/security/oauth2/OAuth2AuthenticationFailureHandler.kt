package me.onetwo.growsnap.infrastructure.security.oauth2

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI

/**
 * OAuth2 인증 실패 핸들러
 *
 * OAuth2 로그인 실패 시 에러 정보를 포함하여
 * 프론트엔드로 리다이렉트합니다.
 *
 * @property frontendUrl 프론트엔드 URL
 */
@Component
class OAuth2AuthenticationFailureHandler(
    @Value("\${app.frontend-url:http://localhost:3000}")
    private val frontendUrl: String
) : ServerAuthenticationFailureHandler {

    /**
     * OAuth2 인증 실패 시 처리
     *
     * 에러 정보를 쿼리 파라미터로 포함하여 프론트엔드로 리다이렉트합니다.
     *
     * @param webFilterExchange WebFilterExchange
     * @param exception 인증 예외
     * @return Mono<Void>
     */
    override fun onAuthenticationFailure(
        webFilterExchange: WebFilterExchange,
        exception: AuthenticationException
    ): Mono<Void> {
        // 프론트엔드 에러 페이지로 리다이렉트
        val redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/auth/oauth2/error")
            .queryParam("error", exception.message ?: "인증에 실패했습니다")
            .build()
            .toUriString()

        val exchange = webFilterExchange.exchange
        exchange.response.statusCode = HttpStatus.FOUND
        exchange.response.headers.location = URI.create(redirectUrl)

        return exchange.response.setComplete()
    }
}
