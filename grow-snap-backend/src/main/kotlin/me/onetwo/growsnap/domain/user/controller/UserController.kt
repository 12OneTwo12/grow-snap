package me.onetwo.growsnap.domain.user.controller

import me.onetwo.growsnap.domain.user.dto.UserResponse
import me.onetwo.growsnap.domain.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 사용자 관리 Controller
 *
 * 사용자 조회 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    /**
     * 내 정보 조회
     *
     * @param userId 사용자 ID (인증된 사용자)
     * @return 사용자 정보
     */
    @GetMapping("/me")
    fun getMe(
        @RequestAttribute userId: UUID
    ): Mono<ResponseEntity<UserResponse>> {
        return Mono.fromCallable {
            val user = userService.getUserById(userId)
            ResponseEntity.ok(UserResponse.from(user))
        }
    }

    /**
     * 사용자 ID로 조회
     *
     * @param targetUserId 조회할 사용자 ID
     * @return 사용자 정보
     */
    @GetMapping("/{targetUserId}")
    fun getUserById(
        @PathVariable targetUserId: UUID
    ): Mono<ResponseEntity<UserResponse>> {
        return Mono.fromCallable {
            val user = userService.getUserById(targetUserId)
            ResponseEntity.ok(UserResponse.from(user))
        }
    }
}
