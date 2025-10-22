package me.onetwo.growsnap.domain.user.model

import java.time.LocalDateTime
import java.util.UUID

data class Follow(
    val id: Long? = null,
    val followerId: UUID,
    val followingId: UUID,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val createdBy: UUID? = null,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val updatedBy: UUID? = null,
    val deletedAt: LocalDateTime? = null
)
