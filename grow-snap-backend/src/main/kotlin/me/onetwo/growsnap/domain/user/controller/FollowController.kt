package me.onetwo.growsnap.domain.user.controller

import me.onetwo.growsnap.domain.user.dto.FollowCheckResponse
import me.onetwo.growsnap.domain.user.dto.FollowResponse
import me.onetwo.growsnap.domain.user.dto.FollowStatsResponse
import me.onetwo.growsnap.domain.user.service.FollowService
import me.onetwo.growsnap.infrastructure.security.jwt.JwtAuthenticationToken
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 팔로우 관리 Controller
 *
 * 팔로우, 언팔로우, 팔로우 관계 확인 등의 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/follows")
class FollowController(
    private val followService: FollowService
) {

    /**
     * 사용자 팔로우
     *
     * SecurityContext에서 인증된 사용자 정보를 가져와 팔로우 관계를 생성합니다.
     *
     * @param followingId 팔로우할 사용자 ID
     * @return 생성된 팔로우 관계
     */
    @PostMapping("/{followingId}")
    fun follow(
        @PathVariable followingId: UUID
    ): Mono<ResponseEntity<FollowResponse>> {
        return ReactiveSecurityContextHolder.getContext()
            .map { (it.authentication as JwtAuthenticationToken).getUserId() }
            .flatMap { userId ->
                Mono.fromCallable {
                    val follow = followService.follow(userId, followingId)
                    ResponseEntity.status(HttpStatus.CREATED).body(FollowResponse.from(follow))
                }
            }
    }

    /**
     * 사용자 언팔로우
     *
     * SecurityContext에서 인증된 사용자 정보를 가져와 팔로우 관계를 삭제합니다.
     *
     * @param followingId 언팔로우할 사용자 ID
     */
    @DeleteMapping("/{followingId}")
    fun unfollow(
        @PathVariable followingId: UUID
    ): Mono<ResponseEntity<Void>> {
        return ReactiveSecurityContextHolder.getContext()
            .map { (it.authentication as JwtAuthenticationToken).getUserId() }
            .flatMap { userId ->
                Mono.fromRunnable<Void> {
                    followService.unfollow(userId, followingId)
                }.then(Mono.just(ResponseEntity.noContent().build<Void>()))
            }
    }

    /**
     * 팔로우 관계 확인
     *
     * SecurityContext에서 인증된 사용자 정보를 가져와 팔로우 관계를 확인합니다.
     *
     * @param followingId 팔로우 대상 사용자 ID
     * @return 팔로우 여부
     */
    @GetMapping("/check/{followingId}")
    fun checkFollowing(
        @PathVariable followingId: UUID
    ): Mono<ResponseEntity<FollowCheckResponse>> {
        return ReactiveSecurityContextHolder.getContext()
            .map { (it.authentication as JwtAuthenticationToken).getUserId() }
            .flatMap { userId ->
                Mono.fromCallable {
                    val isFollowing = followService.isFollowing(userId, followingId)
                    ResponseEntity.ok(FollowCheckResponse(userId, followingId, isFollowing))
                }
            }
    }

    /**
     * 사용자의 팔로우 통계 조회
     *
     * @param targetUserId 조회할 사용자 ID
     * @return 팔로워/팔로잉 수
     */
    @GetMapping("/stats/{targetUserId}")
    fun getFollowStats(
        @PathVariable targetUserId: UUID
    ): Mono<ResponseEntity<FollowStatsResponse>> {
        return Mono.fromCallable {
            val followerCount = followService.getFollowerCount(targetUserId)
            val followingCount = followService.getFollowingCount(targetUserId)
            ResponseEntity.ok(FollowStatsResponse(targetUserId, followerCount, followingCount))
        }
    }

    /**
     * 내 팔로우 통계 조회
     *
     * SecurityContext에서 인증된 사용자 정보를 가져와 팔로우 통계를 조회합니다.
     *
     * @return 팔로워/팔로잉 수
     */
    @GetMapping("/stats/me")
    fun getMyFollowStats(): Mono<ResponseEntity<FollowStatsResponse>> {
        return ReactiveSecurityContextHolder.getContext()
            .map { (it.authentication as JwtAuthenticationToken).getUserId() }
            .flatMap { userId ->
                Mono.fromCallable {
                    val followerCount = followService.getFollowerCount(userId)
                    val followingCount = followService.getFollowingCount(userId)
                    ResponseEntity.ok(FollowStatsResponse(userId, followerCount, followingCount))
                }
            }
    }
}
