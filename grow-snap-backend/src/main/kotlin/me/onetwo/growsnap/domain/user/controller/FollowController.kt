package me.onetwo.growsnap.domain.user.controller

import me.onetwo.growsnap.domain.user.dto.FollowCheckResponse
import me.onetwo.growsnap.domain.user.dto.FollowResponse
import me.onetwo.growsnap.domain.user.dto.FollowStatsResponse
import me.onetwo.growsnap.domain.user.service.FollowService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

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
     * @param userId 팔로우하는 사용자 ID (인증된 사용자)
     * @param followingId 팔로우할 사용자 ID
     * @return 생성된 팔로우 관계
     */
    @PostMapping("/{followingId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun follow(
        @RequestAttribute userId: Long,
        @PathVariable followingId: Long
    ): Mono<FollowResponse> {
        return Mono.fromCallable {
            val follow = followService.follow(userId, followingId)
            FollowResponse.from(follow)
        }
    }

    /**
     * 사용자 언팔로우
     *
     * @param userId 언팔로우하는 사용자 ID (인증된 사용자)
     * @param followingId 언팔로우할 사용자 ID
     */
    @DeleteMapping("/{followingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unfollow(
        @RequestAttribute userId: Long,
        @PathVariable followingId: Long
    ): Mono<Void> {
        return Mono.fromRunnable {
            followService.unfollow(userId, followingId)
        }
    }

    /**
     * 팔로우 관계 확인
     *
     * @param userId 팔로우하는 사용자 ID (인증된 사용자)
     * @param followingId 팔로우 대상 사용자 ID
     * @return 팔로우 여부
     */
    @GetMapping("/check/{followingId}")
    fun checkFollowing(
        @RequestAttribute userId: Long,
        @PathVariable followingId: Long
    ): Mono<FollowCheckResponse> {
        return Mono.fromCallable {
            val isFollowing = followService.isFollowing(userId, followingId)
            FollowCheckResponse(userId, followingId, isFollowing)
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
        @PathVariable targetUserId: Long
    ): Mono<FollowStatsResponse> {
        return Mono.fromCallable {
            val followerCount = followService.getFollowerCount(targetUserId)
            val followingCount = followService.getFollowingCount(targetUserId)
            FollowStatsResponse(targetUserId, followerCount, followingCount)
        }
    }

    /**
     * 내 팔로우 통계 조회
     *
     * @param userId 사용자 ID (인증된 사용자)
     * @return 팔로워/팔로잉 수
     */
    @GetMapping("/stats/me")
    fun getMyFollowStats(
        @RequestAttribute userId: Long
    ): Mono<FollowStatsResponse> {
        return Mono.fromCallable {
            val followerCount = followService.getFollowerCount(userId)
            val followingCount = followService.getFollowingCount(userId)
            FollowStatsResponse(userId, followerCount, followingCount)
        }
    }
}
