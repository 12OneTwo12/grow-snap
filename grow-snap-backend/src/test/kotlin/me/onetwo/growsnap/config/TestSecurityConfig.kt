package me.onetwo.growsnap.config

import java.util.UUID
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain

/**
 * 테스트용 Security 설정
 *
 * 모든 요청을 인증 없이 허용합니다.
 */
@TestConfiguration
@EnableWebFluxSecurity
class TestSecurityConfig {

    @Bean
    fun testSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { it.anyExchange().permitAll() }
            .build()
    }

    /**
     * 테스트용 WebFilter
     *
     * X-User-Id 헤더에서 userId를 읽어 Request attribute로 설정합니다.
     */
    @Bean
    fun testUserIdFilter(): WebFilter {
        return WebFilter { exchange: ServerWebExchange, chain: WebFilterChain ->
            exchange.request.headers.getFirst("X-User-Id")?.let { userIdHeader ->
                try {
                    val userId = UUID.fromString(userIdHeader)
                    exchange.attributes["userId"] = userId
                } catch (e: IllegalArgumentException) {
                    // Ignore invalid userId
                }
            }
            chain.filter(exchange)
        }
    }
}
