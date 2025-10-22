package me.onetwo.growsnap.infrastructure.security.config

import me.onetwo.growsnap.infrastructure.security.oauth2.CustomReactiveOAuth2UserService
import me.onetwo.growsnap.infrastructure.security.oauth2.OAuth2AuthenticationFailureHandler
import me.onetwo.growsnap.infrastructure.security.oauth2.OAuth2AuthenticationSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.client.authentication.OAuth2LoginReactiveAuthenticationManager
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

/**
 * Spring Security 설정
 *
 * OAuth2 로그인, CORS, 인증/인가 규칙을 설정합니다.
 *
 * @property customOAuth2UserService 커스텀 OAuth2 사용자 서비스
 * @property oAuth2SuccessHandler OAuth2 인증 성공 핸들러
 * @property oAuth2FailureHandler OAuth2 인증 실패 핸들러
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomReactiveOAuth2UserService,
    private val oAuth2SuccessHandler: OAuth2AuthenticationSuccessHandler,
    private val oAuth2FailureHandler: OAuth2AuthenticationFailureHandler
) {

    /**
     * Spring Security 필터 체인 설정
     *
     * OAuth2 로그인, CORS, CSRF, 인증/인가 규칙을 구성합니다.
     *
     * @param http ServerHttpSecurity
     * @return SecurityWebFilterChain
     */
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeExchange { authorize ->
                authorize
                    // 공개 엔드포인트
                    .pathMatchers(
                        "/api/v1/auth/**",
                        "/oauth2/**",
                        "/login/**",
                        "/error"
                    ).permitAll()
                    // 나머지는 인증 필요
                    .anyExchange().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .authenticationSuccessHandler(oAuth2SuccessHandler)
                    .authenticationFailureHandler(oAuth2FailureHandler)
                    // CustomReactiveOAuth2UserService를 사용하는 AuthenticationManager 등록
                    .authenticationManager(oauth2AuthenticationManager())
            }
            .build()
    }

    /**
     * OAuth2 인증 매니저 설정
     *
     * CustomReactiveOAuth2UserService를 사용하여 OAuth2 사용자 정보를 처리합니다.
     *
     * @return ReactiveAuthenticationManager
     */
    @Bean
    fun oauth2AuthenticationManager(): ReactiveAuthenticationManager {
        val tokenResponseClient = WebClientReactiveAuthorizationCodeTokenResponseClient()
        return OAuth2LoginReactiveAuthenticationManager(tokenResponseClient, customOAuth2UserService)
    }

    /**
     * CORS 설정
     *
     * 프론트엔드에서 백엔드 API 호출을 허용합니다.
     *
     * @return CorsConfigurationSource
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf(
                "http://localhost:3000",  // 개발 환경 프론트엔드
                "http://localhost:5173"   // Vite 기본 포트
            )
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
