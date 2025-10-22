package me.onetwo.growsnap.domain.user.dto

import me.onetwo.growsnap.domain.user.model.OAuthProvider
import me.onetwo.growsnap.domain.user.model.User
import me.onetwo.growsnap.domain.user.model.UserRole
import java.time.LocalDateTime

/**
 * 사용자 응답 DTO
 */
data class UserResponse(
    val id: Long,
    val email: String,
    val provider: OAuthProvider,
    val role: UserRole,
    val isCreator: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                email = user.email,
                provider = user.provider,
                role = user.role,
                isCreator = user.isCreator,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
