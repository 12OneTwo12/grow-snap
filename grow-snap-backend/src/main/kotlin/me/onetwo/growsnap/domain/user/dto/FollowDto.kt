package me.onetwo.growsnap.domain.user.dto

import me.onetwo.growsnap.domain.user.model.Follow
import java.time.LocalDateTime

/**
 * 팔로우 응답 DTO
 */
data class FollowResponse(
    val id: Long,
    val followerId: Long,
    val followingId: Long,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(follow: Follow): FollowResponse {
            return FollowResponse(
                id = follow.id!!,
                followerId = follow.followerId,
                followingId = follow.followingId,
                createdAt = follow.createdAt
            )
        }
    }
}

/**
 * 팔로우 관계 확인 응답 DTO
 */
data class FollowCheckResponse(
    val followerId: Long,
    val followingId: Long,
    val isFollowing: Boolean
)

/**
 * 팔로우 통계 응답 DTO
 */
data class FollowStatsResponse(
    val userId: Long,
    val followerCount: Int,
    val followingCount: Int
)
