package me.onetwo.growsnap.domain.user.model

import java.time.LocalDateTime
import java.util.UUID

data class UserProfile(
    val id: Long? = null,
    val userId: UUID,
    val nickname: String,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
