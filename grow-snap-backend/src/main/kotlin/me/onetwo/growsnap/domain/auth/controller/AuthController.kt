package me.onetwo.growsnap.domain.auth.controller

import jakarta.validation.Valid
import me.onetwo.growsnap.domain.auth.dto.LogoutRequest
import me.onetwo.growsnap.domain.auth.dto.RefreshTokenRequest
import me.onetwo.growsnap.domain.auth.dto.RefreshTokenResponse
import me.onetwo.growsnap.domain.auth.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인증 컨트롤러
 *
 * OAuth2 로그인, 토큰 갱신, 로그아웃 등의 인증 관련 API를 제공합니다.
 *
 * @property authService 인증 서비스
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    /**
     * Access Token 갱신
     *
     * Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.
     *
     * @param request Refresh Token 요청
     * @return RefreshTokenResponse 새로운 Access Token
     */
    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<RefreshTokenResponse> {
        val response = authService.refreshAccessToken(request.refreshToken)
        return ResponseEntity.ok(response)
    }

    /**
     * 로그아웃
     *
     * Refresh Token을 무효화하여 로그아웃 처리합니다.
     *
     * @param request 로그아웃 요청
     * @return ResponseEntity<Void>
     */
    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody request: LogoutRequest
    ): ResponseEntity<Void> {
        authService.logout(request.refreshToken)
        return ResponseEntity.noContent().build()
    }
}
