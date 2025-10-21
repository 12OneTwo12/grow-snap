package me.onetwo.growsnap.domain.user.model

import java.time.LocalDateTime

data class UserProfile(
    val id: Long? = null,
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
