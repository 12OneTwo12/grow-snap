package me.onetwo.growsnap.domain.user.model

import java.time.LocalDateTime

data class Follow(
    val id: Long? = null,
    val followerId: Long,
    val followingId: Long,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
